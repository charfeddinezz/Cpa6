package com.example.util

import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/**
 * Embedded local HTTP-to-SOCKS5 bridge.
 * 
 * Chromium (and Android System WebView) does NOT natively support SOCKS5 username/password
 * authentication. When a user configures an authenticated SOCKS5 proxy (e.g. Asocks, BrightData,
 * Oxylabs, Smartproxy), Chromium fails to connect with ERR_PROXY_CONNECTION_FAILED.
 * 
 * This bridge runs locally on 127.0.0.1, receives standard HTTP / HTTPS CONNECT requests from
 * the WebView, performs the RFC 1928 / 1929 SOCKS5 authentication with the remote proxy,
 * and seamlessly relays the traffic with remote DNS resolution (zero DNS leak).
 */
object LocalSocks5HttpBridge {

    private const val TAG = "Socks5Bridge"

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var isRunning: Boolean = false

    @Volatile
    var localPort: Int = 0
        private set

    private var remoteHost: String = ""
    private var remotePort: Int = 0
    private var proxyUser: String = ""
    private var proxyPass: String = ""

    private val threadPool = Executors.newCachedThreadPool()

    @Synchronized
    fun start(
        host: String,
        port: Int,
        user: String = "",
        pass: String = ""
    ): Int {
        // If already running with the exact same configuration, return existing port
        if (isRunning && serverSocket != null && !serverSocket!!.isClosed &&
            remoteHost == host && remotePort == port && proxyUser == user && proxyPass == pass && localPort > 0
        ) {
            return localPort
        }

        stop()

        remoteHost = host
        remotePort = port
        proxyUser = user
        proxyPass = pass

        try {
            val server = ServerSocket(0, 50, java.net.InetAddress.getByName("127.0.0.1"))
            serverSocket = server
            localPort = server.localPort
            isRunning = true

            Log.i(TAG, "Started LocalSocks5HttpBridge on 127.0.0.1:$localPort -> SOCKS5 $remoteHost:$remotePort (Auth: ${proxyUser.isNotBlank()})")

            thread(name = "Socks5Bridge-Acceptor", isDaemon = true) {
                while (isRunning && !server.isClosed) {
                    try {
                        val clientSocket = server.accept()
                        threadPool.execute {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isRunning) break
                    }
                }
            }

            return localPort
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LocalSocks5HttpBridge: ${e.message}", e)
            return 0
        }
    }

    @Synchronized
    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
        serverSocket = null
        localPort = 0
        Log.i(TAG, "Stopped LocalSocks5HttpBridge")
    }

    private fun handleClient(clientSocket: Socket) {
        var remoteSocket: Socket? = null
        try {
            clientSocket.soTimeout = 30000
            val clientIn = BufferedInputStream(clientSocket.getInputStream())
            val clientOut = BufferedOutputStream(clientSocket.getOutputStream())

            val requestLine = readLine(clientIn) ?: return
            val parts = requestLine.trim().split(" ")
            if (parts.size < 2) return

            val method = parts[0].uppercase()
            val target = parts[1]

            val targetHost: String
            val targetPort: Int

            if (method == "CONNECT") {
                // HTTPS Tunneling: CONNECT host:port HTTP/1.1
                val hostPort = target.split(":")
                targetHost = hostPort[0]
                targetPort = hostPort.getOrNull(1)?.toIntOrNull() ?: 443

                // Drain remaining HTTP headers from client
                while (true) {
                    val line = readLine(clientIn) ?: break
                    if (line.isEmpty()) break
                }

                // Connect to remote SOCKS5 proxy
                remoteSocket = connectSocks5(targetHost, targetPort)
                if (remoteSocket == null) {
                    clientOut.write("HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\n\r\n".toByteArray(Charsets.US_ASCII))
                    clientOut.flush()
                    return
                }

                // Confirm tunnel establishment to client
                clientOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray(Charsets.US_ASCII))
                clientOut.flush()

                // Remove socket timeout for bidirectional streaming
                clientSocket.soTimeout = 0

                // Bidirectional pipe
                pipeBiDirectional(clientSocket, remoteSocket, clientIn, clientOut)
            } else {
                // Plain HTTP request: GET http://host:port/path HTTP/1.1
                val urlString = if (target.startsWith("http://", ignoreCase = true)) {
                    target.substring(7)
                } else {
                    target
                }
                val hostPart = urlString.substringBefore("/")
                val targetPath = "/" + urlString.substringAfter("/", "")

                val hostPort = hostPart.split(":")
                targetHost = hostPort[0]
                targetPort = hostPort.getOrNull(1)?.toIntOrNull() ?: 80

                remoteSocket = connectSocks5(targetHost, targetPort)
                if (remoteSocket == null) {
                    clientOut.write("HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\n\r\n".toByteArray(Charsets.US_ASCII))
                    clientOut.flush()
                    return
                }

                val remoteOut = BufferedOutputStream(remoteSocket.getOutputStream())
                val httpVersion = parts.getOrNull(2) ?: "HTTP/1.1"
                remoteOut.write("$method $targetPath $httpVersion\r\n".toByteArray(Charsets.US_ASCII))

                // Forward remaining headers, sanitizing Proxy-Connection
                while (true) {
                    val line = readLine(clientIn) ?: break
                    if (line.isEmpty()) {
                        remoteOut.write("\r\n".toByteArray(Charsets.US_ASCII))
                        break
                    }
                    val sanitizedLine = if (line.startsWith("Proxy-Connection:", ignoreCase = true)) {
                        "Connection: " + line.substringAfter(":").trim()
                    } else {
                        line
                    }
                    remoteOut.write("$sanitizedLine\r\n".toByteArray(Charsets.US_ASCII))
                }
                remoteOut.flush()

                clientSocket.soTimeout = 0
                pipeBiDirectional(clientSocket, remoteSocket, clientIn, clientOut)
            }
        } catch (e: Exception) {
            // connection ended or failed
        } finally {
            try { clientSocket.close() } catch (e: Exception) {}
            try { remoteSocket?.close() } catch (e: Exception) {}
        }
    }

    private fun connectSocks5(targetHost: String, targetPort: Int): Socket? {
        val s = Socket()
        try {
            s.soTimeout = 20000
            s.connect(InetSocketAddress(remoteHost, remotePort), 15000)
            val sIn = s.getInputStream()
            val sOut = s.getOutputStream()

            // 1. SOCKS5 Greeting (RFC 1928)
            val hasAuth = proxyUser.isNotBlank() && proxyPass.isNotBlank()
            if (hasAuth) {
                // Support NO_AUTH (0x00) and USER_PASS (0x02)
                sOut.write(byteArrayOf(0x05, 0x02, 0x00, 0x02))
            } else {
                sOut.write(byteArrayOf(0x05, 0x01, 0x00))
            }
            sOut.flush()

            val greetingResp = ByteArray(2)
            readExact(sIn, greetingResp)
            if (greetingResp[0] != 0x05.toByte()) {
                s.close()
                return null
            }

            val chosenMethod = greetingResp[1].toInt() and 0xFF
            if (chosenMethod == 0xFF) {
                // No acceptable methods
                s.close()
                return null
            }

            // 2. Authentication Subnegotiation (RFC 1929)
            if (chosenMethod == 0x02) {
                val uBytes = proxyUser.toByteArray(Charsets.UTF_8)
                val pBytes = proxyPass.toByteArray(Charsets.UTF_8)
                val authReq = ByteArray(3 + uBytes.size + pBytes.size)
                authReq[0] = 0x01 // Auth version
                authReq[1] = uBytes.size.toByte()
                System.arraycopy(uBytes, 0, authReq, 2, uBytes.size)
                authReq[2 + uBytes.size] = pBytes.size.toByte()
                System.arraycopy(pBytes, 0, authReq, 3 + uBytes.size, pBytes.size)

                sOut.write(authReq)
                sOut.flush()

                val authResp = ByteArray(2)
                readExact(sIn, authResp)
                if (authResp[1] != 0x00.toByte()) {
                    Log.w(TAG, "SOCKS5 authentication failed for user: $proxyUser")
                    s.close()
                    return null
                }
            }

            // 3. SOCKS5 CONNECT request
            // If targetHost is an IPv4 address, use 0x01. If domain name, use 0x03 for remote DNS resolution.
            val ipParts = targetHost.split(".")
            val isIpv4 = ipParts.size == 4 && ipParts.all { part -> part.toIntOrNull() in 0..255 }

            val connReq = if (isIpv4) {
                val req = ByteArray(4 + 4 + 2)
                req[0] = 0x05 // Version
                req[1] = 0x01 // Command: CONNECT
                req[2] = 0x00 // Reserved
                req[3] = 0x01 // Address Type: IPv4
                for (i in 0..3) {
                    req[4 + i] = ipParts[i].toInt().toByte()
                }
                req[8] = (targetPort shr 8).toByte()
                req[9] = (targetPort and 0xFF).toByte()
                req
            } else {
                val hostBytes = targetHost.toByteArray(Charsets.US_ASCII)
                val req = ByteArray(4 + 1 + hostBytes.size + 2)
                req[0] = 0x05 // Version
                req[1] = 0x01 // Command: CONNECT
                req[2] = 0x00 // Reserved
                req[3] = 0x03 // Address Type: Domain Name (remote DNS resolution)
                req[4] = hostBytes.size.toByte()
                System.arraycopy(hostBytes, 0, req, 5, hostBytes.size)
                req[5 + hostBytes.size] = (targetPort shr 8).toByte()
                req[6 + hostBytes.size] = (targetPort and 0xFF).toByte()
                req
            }

            sOut.write(connReq)
            sOut.flush()

            // 4. SOCKS5 Connection Response
            val head = ByteArray(4)
            readExact(sIn, head)
            if (head[1] != 0x00.toByte()) {
                Log.w(TAG, "SOCKS5 connect to $targetHost:$targetPort rejected, rep=${head[1]}")
                s.close()
                return null
            }

            // Skip BND.ADDR and BND.PORT using exact byte buffer reads (NEVER rely on InputStream.skip on sockets)
            val atyp = head[3].toInt() and 0xFF
            when (atyp) {
                0x01 -> readExact(sIn, ByteArray(4 + 2)) // IPv4 + port
                0x03 -> {
                    val len = sIn.read()
                    if (len == -1) throw java.io.EOFException("Premature EOF reading BND.ADDR")
                    readExact(sIn, ByteArray(len + 2))
                }
                0x04 -> readExact(sIn, ByteArray(16 + 2)) // IPv6 + port
            }

            s.soTimeout = 0 // back to non-timeout for streaming
            return s
        } catch (e: Exception) {
            try { s.close() } catch (ex: Exception) {}
            return null
        }
    }

    private fun pipeBiDirectional(client: Socket, remote: Socket, clientIn: InputStream, clientOut: OutputStream) {
        val remoteIn = remote.getInputStream()
        val remoteOut = remote.getOutputStream()
        val closed = java.util.concurrent.atomic.AtomicBoolean(false)

        fun closeBoth() {
            if (closed.compareAndSet(false, true)) {
                try { client.close() } catch (e: Exception) {}
                try { remote.close() } catch (e: Exception) {}
            }
        }

        val t1 = thread(name = "Bridge-C2R", isDaemon = true) {
            try {
                val buf = ByteArray(16384)
                var read: Int
                while (clientIn.read(buf).also { read = it } != -1) {
                    remoteOut.write(buf, 0, read)
                    remoteOut.flush()
                }
            } catch (e: Exception) {
            } finally {
                closeBoth()
            }
        }

        val t2 = thread(name = "Bridge-R2C", isDaemon = true) {
            try {
                val buf = ByteArray(16384)
                var read: Int
                while (remoteIn.read(buf).also { read = it } != -1) {
                    clientOut.write(buf, 0, read)
                    clientOut.flush()
                }
            } catch (e: Exception) {
            } finally {
                closeBoth()
            }
        }

        try {
            t1.join()
        } catch (e: Exception) {}
        try {
            t2.join()
        } catch (e: Exception) {}
        closeBoth()
    }

    private fun readLine(input: InputStream): String? {
        val bos = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b == -1) {
                if (bos.size() == 0) return null
                break
            }
            if (b == '\n'.code) {
                break
            }
            if (b != '\r'.code) {
                bos.write(b)
            }
        }
        return bos.toString("US-ASCII")
    }

    private fun readExact(input: InputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read == -1) throw java.io.EOFException("Premature EOF reading SOCKS5 frame")
            offset += read
        }
    }
}
