package com.example.service

import com.example.data.model.ExtractedInfo
import com.example.data.model.GeneratedIdentity
import org.json.JSONArray
import org.json.JSONObject

object AutomationScriptBuilder {

    fun buildAntiDetectionScript(
        proxyIp: String = "",
        webrtcMode: String = "spoof", // "spoof", "disabled", "real"
        timezone: String = "America/New_York",
        language: String = "en-US",
        latitude: Double = 40.7128,
        longitude: Double = -74.0060,
        userAgent: String = "",
        fingerprintSeed: Long = 0L // Expert: stable per-session seed; 0 = legacy random
    ): String {
        val cleanIp = proxyIp.trim().ifBlank { "104.28.19.42" }
        val cleanTz = timezone.trim().ifBlank { "America/New_York" }
        val cleanLang = language.trim().ifBlank { "en-US" }
        val cleanUa = userAgent.trim().replace("'", "\\'")

        val uaLower = userAgent.lowercase()
        val isAndroid = uaLower.contains("android")
        val isIphone = uaLower.contains("iphone") || uaLower.contains("ipad")
        val isMac = !isIphone && uaLower.contains("macintosh")
        val isLinux = !isAndroid && uaLower.contains("linux")

        val platformString = when {
            isAndroid -> "Linux armv81"
            isIphone -> "iPhone"
            isMac -> "MacIntel"
            isLinux -> "Linux x86_64"
            else -> "Win32"
        }

        val isMobile = isAndroid || isIphone
        val touchPoints = if (isMobile) 5 else 0
        val webglVendor = if (isIphone || isMac) "Apple Inc." else "Google Inc. (NVIDIA)"
        val webglRenderer = when {
            isAndroid -> "Adreno (TM) 740"
            isIphone -> "Apple GPU"
            isMac -> "Apple M2 Pro"
            else -> "ANGLE (NVIDIA, NVIDIA GeForce RTX 3060 Direct3D11 vs_5_0 ps_5_0, D3D11)"
        }
        val clientPlatform = when {
            isAndroid -> "Android"
            isIphone -> "iOS"
            isMac -> "macOS"
            isLinux -> "Linux"
            else -> "Windows"
        }

        // Seeded PRNG so the SAME session keeps the SAME fingerprint (mid-session drift = bot signal)
        val seedInit = if (fingerprintSeed == 0L) "Math.floor(Math.random()*1e9)" else "${fingerprintSeed}>>>0"

        return """
(function() {
  var __cpaSeed = $seedInit;
  function __cpaRnd() { __cpaSeed = ((__cpaSeed * 1664525 + 1013904223) >>> 0); return __cpaSeed / 4294967296; }
  function __cpaPick(arr) { return arr[Math.floor(__cpaRnd() * arr.length)]; }
  // 1. WebGL Vendor & Renderer spoofing (Synchronized with User-Agent & Platform)
  try {
    var getParameter = WebGLRenderingContext.prototype.getParameter;
    WebGLRenderingContext.prototype.getParameter = function(parameter) {
      if (parameter === 37445) return '$webglVendor';
      if (parameter === 37446) return '$webglRenderer';
      if (parameter === 7936) return 'WebKit';
      if (parameter === 7937) return 'WebKit WebGL';
      return getParameter.call(this, parameter);
    };
    if (window.WebGL2RenderingContext) {
      var getParameter2 = WebGL2RenderingContext.prototype.getParameter;
      WebGL2RenderingContext.prototype.getParameter = function(parameter) {
        if (parameter === 37445) return '$webglVendor';
        if (parameter === 37446) return '$webglRenderer';
        if (parameter === 7936) return 'WebKit';
        if (parameter === 7937) return 'WebKit WebGL';
        return getParameter2.call(this, parameter);
      };
    }
  } catch(e) {}

  // 2. Hardware, Navigator & Client Hints (Aligned with active User-Agent profile)
  try {
    Object.defineProperty(navigator, 'webdriver', { get: () => false, configurable: true });
    Object.defineProperty(navigator, 'platform', { get: () => '$platformString', configurable: true });
    Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 8, configurable: true });
    Object.defineProperty(navigator, 'deviceMemory', { get: () => 8, configurable: true });
    Object.defineProperty(navigator, 'maxTouchPoints', { get: () => $touchPoints, configurable: true });

    if ('$cleanUa'.length > 0) {
      try {
        Object.defineProperty(navigator, 'userAgent', { get: () => '$cleanUa', configurable: true });
      } catch(e) {}
    }

    if (!window.chrome) {
      window.chrome = {
        runtime: {
          PlatformOs: { ANDROID: 'android', CROS: 'cros', LINUX: 'linux', MAC: 'mac', OPENBSD: 'openbsd', WIN: 'win' },
          PlatformArch: { ARM: 'arm', ARM64: 'arm64', MIPS: 'mips', MIPS64: 'mips64', X86_32: 'x86-32', X86_64: 'x86-64' },
          PlatformNaclArch: { ARM: 'arm', MIPS: 'mips', MIPS64: 'mips64', X86_32: 'x86-32', X86_64: 'x86-64' }
        },
        loadTimes: function() {},
        csi: function() {},
        app: { isInstalled: false }
      };
    }

    // Spoof modern Chromium Client Hints (navigator.userAgentData)
    var uaData = {
      brands: [
        { brand: 'Chromium', version: '122' },
        { brand: 'Not(A:Brand', version: '24' },
        { brand: 'Google Chrome', version: '122' }
      ],
      mobile: $isMobile,
      platform: '$clientPlatform',
      getHighEntropyValues: function(hints) {
        return Promise.resolve({
          architecture: '${if (isMobile) "arm" else "x86"}',
          bitness: '64',
          brands: [
            { brand: 'Chromium', version: '122' },
            { brand: 'Not(A:Brand', version: '24' },
            { brand: 'Google Chrome', version: '122' }
          ],
          fullVersionList: [
            { brand: 'Chromium', version: '122.0.6261.94' },
            { brand: 'Not(A:Brand', version: '24.0.0.0' },
            { brand: 'Google Chrome', version: '122.0.6261.94' }
          ],
          mobile: $isMobile,
          model: '${if (isAndroid) "Pixel 8 Pro" else if (isIphone) "iPhone" else ""}',
          platform: '$clientPlatform',
          platformVersion: '15.0.0',
          uaFullVersion: '122.0.6261.94'
        });
      },
      toJSON: function() {
        return { brands: this.brands, mobile: this.mobile, platform: this.platform };
      }
    };
    try {
      Object.defineProperty(navigator, 'userAgentData', {
        get: function() { return uaData; },
        configurable: true
      });
    } catch(e) {}

    // Screen color depth
    try {
      Object.defineProperty(screen, 'colorDepth', { get: () => 24, configurable: true });
      Object.defineProperty(screen, 'pixelDepth', { get: () => 24, configurable: true });
    } catch(e) {}

    // Subtle Canvas Fingerprint Shield - Randomized noise injection
    try {
      function addNoiseToImageData(imageData) {
        if (!imageData || !imageData.data) return;
        var data = imageData.data;
        var len = data.length;
        if (len === 0) return;
        var stride = len > 40000 ? 16 : 4;
        for (var i = 0; i < len; i += stride) {
          if (data[i + 3] > 5) {
            var noise = (Math.random() < 0.5 ? 1 : -1) * (1 + Math.floor(Math.random() * 2));
            var channel = i % 3;
            var val = data[i + channel] + noise;
            data[i + channel] = val < 0 ? 0 : (val > 255 ? 255 : val);
          }
        }
      }

      if (window.CanvasRenderingContext2D) {
        var origGetImageData = CanvasRenderingContext2D.prototype.getImageData;
        var origPutImageData = CanvasRenderingContext2D.prototype.putImageData;

        CanvasRenderingContext2D.prototype.getImageData = function(sx, sy, sw, sh) {
          var imgData = origGetImageData.apply(this, arguments);
          addNoiseToImageData(imgData);
          return imgData;
        };
      }

      if (window.HTMLCanvasElement) {
        var origToDataURL = HTMLCanvasElement.prototype.toDataURL;
        var origToBlob = HTMLCanvasElement.prototype.toBlob;

        function perturbCanvasPixels(canvas) {
          try {
            if (!canvas || canvas.width === 0 || canvas.height === 0) return;
            var ctx = null;
            try { ctx = canvas.getContext('2d'); } catch(err) {}

            if (ctx && typeof origGetImageData === 'function' && typeof origPutImageData === 'function') {
              var sampleW = Math.min(canvas.width, 48);
              var sampleH = Math.min(canvas.height, 48);
              var slice = origGetImageData.call(ctx, 0, 0, sampleW, sampleH);
              var d = slice.data;
              var modified = false;
              for (var k = 0; k < d.length; k += 4) {
                if (d[k + 3] > 5) {
                  var delta = (Math.random() < 0.5 ? 1 : -1);
                  var c = k % 3;
                  d[k + c] = Math.min(255, Math.max(0, d[k + c] + delta));
                  modified = true;
                  if (Math.random() < 0.08) break;
                }
              }
              if (modified) {
                origPutImageData.call(ctx, slice, 0, 0);
              }
            }
          } catch(e) {}
        }

        HTMLCanvasElement.prototype.toDataURL = function(type, encoderOptions) {
          perturbCanvasPixels(this);
          return origToDataURL.apply(this, arguments);
        };

        HTMLCanvasElement.prototype.toBlob = function(callback, type, quality) {
          perturbCanvasPixels(this);
          return origToBlob.apply(this, arguments);
        };
      }

      if (window.OffscreenCanvas && window.OffscreenCanvasRenderingContext2D) {
        var origOffscreenGetImageData = OffscreenCanvasRenderingContext2D.prototype.getImageData;
        OffscreenCanvasRenderingContext2D.prototype.getImageData = function(sx, sy, sw, sh) {
          var imgData = origOffscreenGetImageData.apply(this, arguments);
          addNoiseToImageData(imgData);
          return imgData;
        };
      }

      if (window.WebGLRenderingContext) {
        var origReadPixels = WebGLRenderingContext.prototype.readPixels;
        WebGLRenderingContext.prototype.readPixels = function(x, y, width, height, format, type, pixels) {
          origReadPixels.apply(this, arguments);
          try {
            if (pixels && pixels.length > 0) {
              for (var p = 0; p < pixels.length; p += 16) {
                if (Math.random() < 0.15) {
                  pixels[p] = Math.min(255, Math.max(0, pixels[p] + (Math.random() < 0.5 ? 1 : -1)));
                }
              }
            }
          } catch(e) {}
        };
      }
    } catch(e) {}

    // Subtle AudioContext Fingerprint Shield
    if (window.AudioBuffer) {
      var origGetChannelData = AudioBuffer.prototype.getChannelData;
      AudioBuffer.prototype.getChannelData = function() {
        var data = origGetChannelData.apply(this, arguments);
        if (data && data.length > 0) {
          for (var i = 0; i < Math.min(data.length, 8); i++) {
            data[i] += (Math.random() - 0.5) * 0.0000001;
          }
        }
        return data;
      };
    }
  } catch(e) {}

  // 3. Timezone & Locale Matching (Synchronized with Proxy Country / City)
  try {
    var tz = '$cleanTz';
    var lang = '$cleanLang';

    var OrigDTF = Intl.DateTimeFormat;
    Intl.DateTimeFormat = function(locales, options) {
      options = options || {};
      if (!options.timeZone) options.timeZone = tz;
      return new OrigDTF(lang, options);
    };
    Intl.DateTimeFormat.prototype = OrigDTF.prototype;

    var origResolvedOptions = OrigDTF.prototype.resolvedOptions;
    OrigDTF.prototype.resolvedOptions = function() {
      var res = origResolvedOptions.call(this);
      res.timeZone = tz;
      return res;
    };

    Object.defineProperty(navigator, 'language', { get: () => lang, configurable: true });
    Object.defineProperty(navigator, 'languages', { get: () => [lang, lang.split('-')[0]], configurable: true });

    // Dynamic Timezone Offset matching the proxy IANA timezone
    function calculateTzOffset(ianaTz) {
      try {
        var date = new Date();
        var utcDate = new Date(date.toLocaleString('en-US', { timeZone: 'UTC' }));
        var tzDate = new Date(date.toLocaleString('en-US', { timeZone: ianaTz }));
        return Math.round((utcDate.getTime() - tzDate.getTime()) / 60000);
      } catch(err) {
        return 0;
      }
    }
    var targetOffset = calculateTzOffset(tz);
    Date.prototype.getTimezoneOffset = function() {
      return targetOffset;
    };
  } catch(e) {}

  // 4. HTML5 Geolocation Spoofing (Matches Proxy Latitude / Longitude exactly)
  try {
    if (navigator.geolocation) {
      var fakeGeoPos = {
        coords: {
          latitude: $latitude,
          longitude: $longitude,
          accuracy: 20.0,
          altitude: null,
          altitudeAccuracy: null,
          heading: null,
          speed: null
        },
        timestamp: Date.now()
      };
      navigator.geolocation.getCurrentPosition = function(success, error, options) {
        if (typeof success === 'function') {
          setTimeout(function() { success(fakeGeoPos); }, 20);
        }
      };
      navigator.geolocation.watchPosition = function(success, error, options) {
        if (typeof success === 'function') {
          setTimeout(function() { success(fakeGeoPos); }, 20);
        }
        return 1;
      };
      navigator.geolocation.clearWatch = function() {};
    }
  } catch(e) {}

  // 5. WebRTC Configuration & IP Spoofing (Mode: $webrtcMode, Target IP: $cleanIp)
  try {
    var mode = '$webrtcMode';
    var targetIp = '$cleanIp';
    var localIp = '192.168.1.105';

    function applyWebRtcToWindow(targetWin) {
      if (!targetWin) return;

      if (mode === 'disabled') {
        var webRtcProps = [
          'RTCPeerConnection',
          'webkitRTCPeerConnection',
          'mozRTCPeerConnection',
          'RTCDataChannel',
          'RTCIceCandidate',
          'RTCSessionDescription'
        ];
        webRtcProps.forEach(function(prop) {
          try { delete targetWin[prop]; } catch(e) {}
          try {
            Object.defineProperty(targetWin, prop, {
              get: function() { return undefined; },
              set: function() {},
              configurable: false,
              enumerable: false
            });
          } catch(ex) {
            try { targetWin[prop] = undefined; } catch(ex2) {}
          }
        });
        return;
      }

      if (mode !== 'spoof') return;

      // High-Fidelity WebRTC Spoofing Engine:
      function FakeRTCIceCandidate(init) {
        if (!init) init = {};
        this.candidate = init.candidate || ('candidate:2130706431 1 udp 2122260223 ' + targetIp + ' 54322 typ srflx raddr ' + localIp + ' rport 54321 generation 0 ufrag abcd network-id 1');
        this.sdpMid = init.sdpMid !== undefined ? init.sdpMid : '0';
        this.sdpMLineIndex = init.sdpMLineIndex !== undefined ? init.sdpMLineIndex : 0;
        this.usernameFragment = init.usernameFragment || 'abcd';
        this.foundation = init.foundation || '2130706431';
        this.component = init.component || 'rtp';
        this.priority = init.priority || 2122260223;
        this.address = init.address || targetIp;
        this.protocol = init.protocol || 'udp';
        this.port = init.port || 54322;
        this.type = init.type || 'srflx';
        this.tcpType = null;
        this.relatedAddress = init.relatedAddress || localIp;
        this.relatedPort = init.relatedPort || 54321;
      }
      FakeRTCIceCandidate.prototype.toJSON = function() {
        return {
          candidate: this.candidate,
          sdpMid: this.sdpMid,
          sdpMLineIndex: this.sdpMLineIndex,
          usernameFragment: this.usernameFragment
        };
      };

      function FakeRTCSessionDescription(init) {
        if (!init) init = {};
        this.type = init.type || 'offer';
        this.sdp = init.sdp || '';
      }
      FakeRTCSessionDescription.prototype.toJSON = function() {
        return { type: this.type, sdp: this.sdp };
      };

      function buildSpoofedSdp() {
        return [
          'v=0',
          'o=- 422319207012345678 2 IN IP4 127.0.0.1',
          's=-',
          't=0 0',
          'a=group:BUNDLE 0',
          'a=msid-semantic: WMS',
          'm=application 9 UDP/DTLS/SCTP webrtc-datachannel',
          'c=IN IP4 ' + targetIp,
          'a=candidate:842163049 1 udp 2122199295 ' + localIp + ' 54321 typ host generation 0 ufrag abcd network-id 1',
          'a=candidate:2130706431 1 udp 2122260223 ' + targetIp + ' 54322 typ srflx raddr ' + localIp + ' rport 54321 generation 0 ufrag abcd network-id 1',
          'a=ice-ufrag:abcd',
          'a=ice-pwd:abcdefghijklmnopqrstuvwxyz01',
          'a=ice-options:trickle',
          'a=fingerprint:sha-256 00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF',
          'a=setup:actpass',
          'a=mid:0',
          'a=sctp-port:5000',
          'a=max-message-size:262144',
          ''
        ].join('\r\n');
      }

      function FakeRTCPeerConnection(config) {
        var self = this;
        this.config = config || {};
        this.localDescription = null;
        this.remoteDescription = null;
        this.signalingState = 'stable';
        this.iceGatheringState = 'new';
        this.iceConnectionState = 'new';
        this.connectionState = 'new';
        this.canTrickleIceCandidates = true;

        this.onicecandidate = null;
        this.onicecandidateerror = null;
        this.onicegatheringstatechange = null;
        this.oniceconnectionstatechange = null;
        this.onsignalingstatechange = null;
        this.onconnectionstatechange = null;
        this.onnegotiationneeded = null;
        this.ondatachannel = null;
        this.ontrack = null;

        var listeners = {};
        this.addEventListener = function(type, listener) {
          if (typeof listener !== 'function') return;
          if (!listeners[type]) listeners[type] = [];
          listeners[type].push(listener);
        };
        this.removeEventListener = function(type, listener) {
          if (!listeners[type]) return;
          listeners[type] = listeners[type].filter(function(l) { return l !== listener; });
        };
        this.dispatchEvent = function(event) {
          var type = event.type;
          var handler = self['on' + type];
          if (typeof handler === 'function') {
            try { handler.call(self, event); } catch(e) {}
          }
          if (listeners[type]) {
            listeners[type].forEach(function(l) {
              try { l.call(self, event); } catch(e) {}
            });
          }
          return true;
        };

        this.createDataChannel = function(label, options) {
          var dc = {
            label: label || '',
            ordered: true,
            protocol: '',
            id: 0,
            readyState: 'open',
            bufferedAmount: 0,
            onopen: null,
            onclose: null,
            onerror: null,
            onmessage: null,
            send: function() {},
            close: function() { this.readyState = 'closed'; if (this.onclose) this.onclose(); },
            addEventListener: function() {},
            removeEventListener: function() {},
            dispatchEvent: function() { return true; }
          };
          setTimeout(function() { if (dc.onopen) dc.onopen({ type: 'open' }); }, 20);
          return dc;
        };

        this.createOffer = function(arg1) {
          var sdpStr = buildSpoofedSdp();
          var offer = new FakeRTCSessionDescription({ type: 'offer', sdp: sdpStr });
          if (typeof arg1 === 'function') {
            arg1(offer);
            return Promise.resolve(offer);
          }
          return Promise.resolve(offer);
        };

        this.createAnswer = function(arg1) {
          var sdpStr = buildSpoofedSdp();
          var answer = new FakeRTCSessionDescription({ type: 'answer', sdp: sdpStr });
          if (typeof arg1 === 'function') {
            arg1(answer);
            return Promise.resolve(answer);
          }
          return Promise.resolve(answer);
        };

        this.setLocalDescription = function(desc, successCb) {
          self.localDescription = desc || new FakeRTCSessionDescription({ type: 'offer', sdp: buildSpoofedSdp() });
          self.signalingState = 'have-local-offer';
          self.dispatchEvent(new Event('signalingstatechange'));

          self.iceGatheringState = 'gathering';
          self.dispatchEvent(new Event('icegatheringstatechange'));

          // 1. Dispatch Host Candidate
          setTimeout(function() {
            if (self.signalingState === 'closed') return;
            var hostCandidate = new FakeRTCIceCandidate({
              candidate: 'candidate:842163049 1 udp 2122199295 ' + localIp + ' 54321 typ host generation 0 ufrag abcd network-id 1',
              sdpMid: '0',
              sdpMLineIndex: 0,
              type: 'host',
              address: localIp,
              port: 54321,
              relatedAddress: null,
              relatedPort: null
            });
            self.dispatchEvent({ type: 'icecandidate', candidate: hostCandidate, target: self, srcElement: self });
          }, 35);

          // 2. Dispatch Server-Reflexive Candidate containing the SPOOFED PROXY IP
          setTimeout(function() {
            if (self.signalingState === 'closed') return;
            var srflxCandidate = new FakeRTCIceCandidate({
              candidate: 'candidate:2130706431 1 udp 2122260223 ' + targetIp + ' 54322 typ srflx raddr ' + localIp + ' rport 54321 generation 0 ufrag abcd network-id 1',
              sdpMid: '0',
              sdpMLineIndex: 0,
              type: 'srflx',
              address: targetIp,
              port: 54322,
              relatedAddress: localIp,
              relatedPort: 54321
            });
            self.dispatchEvent({ type: 'icecandidate', candidate: srflxCandidate, target: self, srcElement: self });
          }, 85);

          // 3. Dispatch end-of-candidates (null candidate) and mark complete
          setTimeout(function() {
            if (self.signalingState === 'closed') return;
            self.dispatchEvent({ type: 'icecandidate', candidate: null, target: self, srcElement: self });
            self.iceGatheringState = 'complete';
            self.dispatchEvent(new Event('icegatheringstatechange'));
            self.iceConnectionState = 'checking';
            self.dispatchEvent(new Event('iceconnectionstatechange'));
          }, 150);

          if (typeof successCb === 'function') successCb();
          return Promise.resolve();
        };

        this.setRemoteDescription = function(desc, successCb) {
          self.remoteDescription = desc;
          if (typeof successCb === 'function') successCb();
          return Promise.resolve();
        };

        this.addIceCandidate = function(cand, successCb) {
          if (typeof successCb === 'function') successCb();
          return Promise.resolve();
        };

        this.getConfiguration = function() {
          return self.config;
        };

        this.setConfiguration = function(config) {
          self.config = config || {};
        };

        this.getSenders = function() { return []; };
        this.getReceivers = function() { return []; };
        this.getTransceivers = function() { return []; };
        this.addTrack = function(track) { return { track: track }; };
        this.removeTrack = function() {};
        this.addTransceiver = function() { return {}; };

        this.getStats = function(selector, successCb) {
          var stats = new Map();
          stats.set('cand-pair', {
            type: 'candidate-pair',
            id: 'cand-pair',
            state: 'succeeded',
            localCandidateId: 'local-cand-srflx'
          });
          stats.set('local-cand-srflx', {
            type: 'local-candidate',
            id: 'local-cand-srflx',
            candidateType: 'srflx',
            ip: targetIp,
            address: targetIp,
            port: 54322,
            protocol: 'udp'
          });
          if (typeof selector === 'function') {
            selector(stats);
            return Promise.resolve(stats);
          }
          return Promise.resolve(stats);
        };

        this.close = function() {
          self.signalingState = 'closed';
          self.iceConnectionState = 'closed';
          self.iceGatheringState = 'complete';
          self.connectionState = 'closed';
        };
      }

      try {
        Object.defineProperty(FakeRTCPeerConnection.prototype, Symbol.toStringTag, { value: 'RTCPeerConnection' });
        Object.defineProperty(FakeRTCIceCandidate.prototype, Symbol.toStringTag, { value: 'RTCIceCandidate' });
        Object.defineProperty(FakeRTCSessionDescription.prototype, Symbol.toStringTag, { value: 'RTCSessionDescription' });
      } catch(e) {}

      targetWin.RTCPeerConnection = FakeRTCPeerConnection;
      targetWin.webkitRTCPeerConnection = FakeRTCPeerConnection;
      targetWin.mozRTCPeerConnection = FakeRTCPeerConnection;
      targetWin.msRTCPeerConnection = FakeRTCPeerConnection;
      targetWin.RTCIceCandidate = FakeRTCIceCandidate;
      targetWin.webkitRTCIceCandidate = FakeRTCIceCandidate;
      targetWin.RTCSessionDescription = FakeRTCSessionDescription;
      targetWin.webkitRTCSessionDescription = FakeRTCSessionDescription;

      try {
        Object.defineProperty(targetWin, 'RTCPeerConnection', { value: FakeRTCPeerConnection, writable: true, configurable: true });
        Object.defineProperty(targetWin, 'webkitRTCPeerConnection', { value: FakeRTCPeerConnection, writable: true, configurable: true });
        Object.defineProperty(targetWin, 'mozRTCPeerConnection', { value: FakeRTCPeerConnection, writable: true, configurable: true });
        Object.defineProperty(targetWin, 'msRTCPeerConnection', { value: FakeRTCPeerConnection, writable: true, configurable: true });
        Object.defineProperty(targetWin, 'RTCIceCandidate', { value: FakeRTCIceCandidate, writable: true, configurable: true });
        Object.defineProperty(targetWin, 'webkitRTCIceCandidate', { value: FakeRTCIceCandidate, writable: true, configurable: true });
        Object.defineProperty(targetWin, 'RTCSessionDescription', { value: FakeRTCSessionDescription, writable: true, configurable: true });
        Object.defineProperty(targetWin, 'webkitRTCSessionDescription', { value: FakeRTCSessionDescription, writable: true, configurable: true });
      } catch(e) {}

      // Codecs & Capabilities spoofing for BrowserLeaks
      var sampleAudioCodecs = [
        { mimeType: 'audio/opus', clockRate: 48000, channels: 2, sdpFmtpLine: 'minptime=10;useinbandfec=1' },
        { mimeType: 'audio/PCMU', clockRate: 8000, channels: 1 },
        { mimeType: 'audio/PCMA', clockRate: 8000, channels: 1 }
      ];
      var sampleVideoCodecs = [
        { mimeType: 'video/VP8', clockRate: 90000 },
        { mimeType: 'video/H264', clockRate: 90000, sdpFmtpLine: 'level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f' },
        { mimeType: 'video/VP9', clockRate: 90000, sdpFmtpLine: 'profile-id=0' }
      ];
      targetWin.RTCRtpSender = {
        getCapabilities: function(k) {
          return { codecs: k === 'audio' ? sampleAudioCodecs : sampleVideoCodecs, headerExtensions: [] };
        }
      };
      targetWin.RTCRtpReceiver = {
        getCapabilities: function(k) {
          return { codecs: k === 'audio' ? sampleAudioCodecs : sampleVideoCodecs, headerExtensions: [] };
        }
      };
    }

    // Apply WebRTC spoof to top window
    applyWebRtcToWindow(window);

    // Iframe WebRTC Leak Shield (Protects against tests creating hidden iframes to bypass top window)
    try {
      var origAppendChild = Element.prototype.appendChild;
      Element.prototype.appendChild = function(el) {
        if (el && el.tagName === 'IFRAME') {
          el.addEventListener('load', function() {
            try { if (el.contentWindow) applyWebRtcToWindow(el.contentWindow); } catch(e) {}
          });
        }
        return origAppendChild.apply(this, arguments);
      };

      var origInsertBefore = Element.prototype.insertBefore;
      Element.prototype.insertBefore = function(newNode, referenceNode) {
        if (newNode && newNode.tagName === 'IFRAME') {
          newNode.addEventListener('load', function() {
            try { if (newNode.contentWindow) applyWebRtcToWindow(newNode.contentWindow); } catch(e) {}
          });
        }
        return origInsertBefore.apply(this, arguments);
      };

      var origContentWindow = Object.getOwnPropertyDescriptor(HTMLIFrameElement.prototype, 'contentWindow');
      if (origContentWindow && origContentWindow.get) {
        Object.defineProperty(HTMLIFrameElement.prototype, 'contentWindow', {
          get: function() {
            var cw = origContentWindow.get.call(this);
            if (cw) {
              try { applyWebRtcToWindow(cw); } catch(e) {}
            }
            return cw;
          },
          configurable: true
        });
      }

      var origContentDoc = Object.getOwnPropertyDescriptor(HTMLIFrameElement.prototype, 'contentDocument');
      if (origContentDoc && origContentDoc.get) {
        Object.defineProperty(HTMLIFrameElement.prototype, 'contentDocument', {
          get: function() {
            var cd = origContentDoc.get.call(this);
            if (cd && cd.defaultView) {
              try { applyWebRtcToWindow(cd.defaultView); } catch(e) {}
            }
            return cd;
          },
          configurable: true
        });
      }

      // Continuous DOM scan for dynamically placed frames
      setInterval(function() {
        try {
          var iframes = document.getElementsByTagName('iframe');
          for (var i = 0; i < iframes.length; i++) {
            var ifr = iframes[i];
            if (ifr && ifr.contentWindow) {
              applyWebRtcToWindow(ifr.contentWindow);
            }
          }
        } catch(e) {}
      }, 300);
    } catch(e) {}

    // Media devices spoofing
    if (navigator.mediaDevices && navigator.mediaDevices.enumerateDevices) {
      navigator.mediaDevices.enumerateDevices = function() {
        return Promise.resolve([
          { deviceId: 'default', kind: 'audioinput', label: 'Default Microphone', groupId: 'audio-1', toJSON: function() { return this; } },
          { deviceId: 'default', kind: 'audiooutput', label: 'Default Speaker', groupId: 'audio-1', toJSON: function() { return this; } },
          { deviceId: 'cam1', kind: 'videoinput', label: 'HD Web Camera', groupId: 'video-1', toJSON: function() { return this; } }
        ]);
      };
    }
    if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
      navigator.mediaDevices.getUserMedia = function() {
        return Promise.reject(new DOMException('Permission denied', 'NotAllowedError'));
      };
    }
    if (navigator.mediaDevices && navigator.mediaDevices.getSupportedConstraints) {
      navigator.mediaDevices.getSupportedConstraints = function() {
        return {
          aspectRatio: true,
          autoGainControl: true,
          channelCount: true,
          deviceId: true,
          echoCancellation: true,
          facingMode: true,
          frameRate: true,
          groupId: true,
          height: true,
          noiseSuppression: true,
          sampleRate: true,
          sampleSize: true,
          width: true
        };
      };
    }
  } catch(e) {
    console.error('WebRTC configuration error:', e);
  }

  // 6. Anti-DNS-Leak Shield & Speculative Prefetch Blocker
  // Forces all DNS lookups to go through the remote proxy tunnel by shutting down client-side DNS pre-resolution
  try {
    var dnsMeta = document.createElement('meta');
    dnsMeta.httpEquiv = 'x-dns-prefetch-control';
    dnsMeta.content = 'off';
    if (document.head) {
      document.head.appendChild(dnsMeta);
    } else if (document.documentElement) {
      document.documentElement.appendChild(dnsMeta);
    }

    function neutralizeDnsLeakLinks(root) {
      if (!root || !root.querySelectorAll) return;
      var leakLinks = root.querySelectorAll('link[rel*="dns-prefetch"], link[rel*="preconnect"], link[rel*="prerender"]');
      for (var i = 0; i < leakLinks.length; i++) {
        try {
          leakLinks[i].removeAttribute('href');
          if (leakLinks[i].parentNode) leakLinks[i].parentNode.removeChild(leakLinks[i]);
        } catch(err) {}
      }
    }
    neutralizeDnsLeakLinks(document);

    if (window.MutationObserver) {
      var leakObserver = new MutationObserver(function(mutations) {
        for (var i = 0; i < mutations.length; i++) {
          var added = mutations[i].addedNodes;
          for (var j = 0; j < added.length; j++) {
            var node = added[j];
            if (node.nodeType === 1) {
              if (node.tagName === 'LINK') {
                var rel = (node.getAttribute('rel') || '').toLowerCase();
                if (rel.indexOf('dns-prefetch') !== -1 || rel.indexOf('preconnect') !== -1 || rel.indexOf('prerender') !== -1) {
                  node.removeAttribute('href');
                  if (node.parentNode) node.parentNode.removeChild(node);
                }
              } else {
                neutralizeDnsLeakLinks(node);
              }
            }
          }
        }
      });
      if (document.documentElement) {
        leakObserver.observe(document.documentElement, { childList: true, subtree: true });
      }
    }
  } catch(e) {}

  console.log('[CPA] Anti-detection, Anti-DNS-Leak & WebRTC active (mode: ' + '$webrtcMode' + ', ip: ' + '$cleanIp' + ', tz: ' + '$cleanTz' + ')');
})();
true;
        """.trimIndent()
    }

    fun buildTimezoneScript(timezone: String, language: String): String {
        return """
(function() {
  try {
    const tz = '$timezone';
    const lang = '$language';

    const OrigIntl = window.Intl;
    const OrigDTF = Intl.DateTimeFormat;
    Intl.DateTimeFormat = function(locales, options) {
      options = options || {};
      if (!options.timeZone) options.timeZone = tz;
      return new OrigDTF(lang, options);
    };
    Intl.DateTimeFormat.prototype = OrigDTF.prototype;

    Object.defineProperty(navigator, 'language', { get: () => lang, configurable: true });
    Object.defineProperty(navigator, 'languages', { get: () => [lang, lang.split('-')[0]], configurable: true });

    function calculateTzOffset(ianaTz) {
      try {
        var date = new Date();
        var utcDate = new Date(date.toLocaleString('en-US', { timeZone: 'UTC' }));
        var tzDate = new Date(date.toLocaleString('en-US', { timeZone: ianaTz }));
        return Math.round((utcDate.getTime() - tzDate.getTime()) / 60000);
      } catch(err) {
        return 0;
      }
    }
    var targetOffset = calculateTzOffset(tz);
    Date.prototype.getTimezoneOffset = function() {
      return targetOffset;
    };
  } catch(e) {}
})();
true;
        """.trimIndent()
    }

    /**
     * Builds a standalone JavaScript injection script loaded on page start that modifies the
     * Canvas API (CanvasRenderingContext2D.getImageData, HTMLCanvasElement.toDataURL,
     * HTMLCanvasElement.toBlob, OffscreenCanvas, and WebGL readPixels) to return randomized noise,
     * completely defeating Canvas and WebGL fingerprinting algorithms.
     */
    fun buildCanvasNoiseScript(): String {
        return """
(function() {
  if (window.__cpaCanvasNoiseInjected) return;
  window.__cpaCanvasNoiseInjected = true;

  // Helper: inject subtle, randomized noise into an ImageData pixel buffer
  function addNoiseToImageData(imageData) {
    try {
      if (!imageData || !imageData.data) return;
      var data = imageData.data;
      var len = data.length;
      if (len === 0) return;

      // Stride through pixels: perturb a random fraction of non-transparent pixels
      var stride = len > 40000 ? 16 : 4;
      for (var i = 0; i < len; i += stride) {
        // Only modify non-transparent pixels (Alpha > 5) to keep transparent areas clean
        if (data[i + 3] > 5) {
          var noise = (Math.random() < 0.5 ? 1 : -1) * (1 + Math.floor(Math.random() * 2));
          var channel = i % 3; // Alter R, G, or B
          var val = data[i + channel] + noise;
          data[i + channel] = val < 0 ? 0 : (val > 255 ? 255 : val);
        }
      }
    } catch(e) {}
  }

  // 1. Hook CanvasRenderingContext2D.prototype.getImageData & putImageData
  try {
    if (window.CanvasRenderingContext2D) {
      var origGetImageData = CanvasRenderingContext2D.prototype.getImageData;
      var origPutImageData = CanvasRenderingContext2D.prototype.putImageData;

      CanvasRenderingContext2D.prototype.getImageData = function(sx, sy, sw, sh) {
        var imgData = origGetImageData.apply(this, arguments);
        addNoiseToImageData(imgData);
        return imgData;
      };
    }
  } catch(e) {}

  // 2. Hook HTMLCanvasElement.prototype.toDataURL and toBlob
  try {
    if (window.HTMLCanvasElement) {
      var origToDataURL = HTMLCanvasElement.prototype.toDataURL;
      var origToBlob = HTMLCanvasElement.prototype.toBlob;

      function perturbCanvasPixels(canvas) {
        try {
          if (!canvas || canvas.width === 0 || canvas.height === 0) return;
          var ctx = null;
          try { ctx = canvas.getContext('2d'); } catch(err) {}

          if (ctx && typeof origGetImageData === 'function' && typeof origPutImageData === 'function') {
            // Apply randomized noise to a region of the canvas
            var sampleW = Math.min(canvas.width, 48);
            var sampleH = Math.min(canvas.height, 48);
            var slice = origGetImageData.call(ctx, 0, 0, sampleW, sampleH);
            var d = slice.data;
            var modified = false;
            for (var k = 0; k < d.length; k += 4) {
              if (d[k + 3] > 5) {
                var delta = (Math.random() < 0.5 ? 1 : -1);
                var c = k % 3;
                d[k + c] = Math.min(255, Math.max(0, d[k + c] + delta));
                modified = true;
                if (Math.random() < 0.08) break;
              }
            }
            if (modified) {
              origPutImageData.call(ctx, slice, 0, 0);
            }
          }
        } catch(e) {}
      }

      HTMLCanvasElement.prototype.toDataURL = function(type, encoderOptions) {
        perturbCanvasPixels(this);
        return origToDataURL.apply(this, arguments);
      };

      HTMLCanvasElement.prototype.toBlob = function(callback, type, quality) {
        perturbCanvasPixels(this);
        return origToBlob.apply(this, arguments);
      };
    }
  } catch(e) {}

  // 3. Hook OffscreenCanvas if available
  try {
    if (window.OffscreenCanvas) {
      if (window.OffscreenCanvasRenderingContext2D) {
        var origOffscreenGetImageData = OffscreenCanvasRenderingContext2D.prototype.getImageData;
        OffscreenCanvasRenderingContext2D.prototype.getImageData = function(sx, sy, sw, sh) {
          var imgData = origOffscreenGetImageData.apply(this, arguments);
          addNoiseToImageData(imgData);
          return imgData;
        };
      }
      var origConvertToBlob = OffscreenCanvas.prototype.convertToBlob;
      if (origConvertToBlob) {
        OffscreenCanvas.prototype.convertToBlob = function(options) {
          try {
            var ctx = this.getContext('2d');
            if (ctx && this.width > 0 && this.height > 0) {
              var w = Math.min(this.width, 32);
              var h = Math.min(this.height, 32);
              var s = ctx.getImageData(0, 0, w, h);
              addNoiseToImageData(s);
              ctx.putImageData(s, 0, 0);
            }
          } catch(e) {}
          return origConvertToBlob.apply(this, arguments);
        };
      }
    }
  } catch(e) {}

  // 4. Hook WebGL readPixels to prevent WebGL-based Canvas fingerprinting
  try {
    function hookWebGLContext(proto) {
      if (!proto || !proto.readPixels) return;
      var origReadPixels = proto.readPixels;
      proto.readPixels = function(x, y, width, height, format, type, pixels) {
        origReadPixels.apply(this, arguments);
        try {
          if (pixels && pixels.length > 0) {
            for (var p = 0; p < pixels.length; p += 16) {
              if (Math.random() < 0.15) {
                pixels[p] = Math.min(255, Math.max(0, pixels[p] + (Math.random() < 0.5 ? 1 : -1)));
              }
            }
          }
        } catch(e) {}
      };
    }
    if (window.WebGLRenderingContext) hookWebGLContext(WebGLRenderingContext.prototype);
    if (window.WebGL2RenderingContext) hookWebGLContext(WebGL2RenderingContext.prototype);
  } catch(e) {}

  console.log('[CPA Shield] Canvas API randomized noise protection active.');
})();
true;
        """.trimIndent()
    }

    fun buildSmartFormFillScript(
        identity: GeneratedIdentity,
        categories: String = "",
        clickTexts: List<String> = emptyList(),
        activeClickText: String? = null,
        extractedInfo: ExtractedInfo? = null
    ): String {
        val planJson = TaskCategoryPlanner.buildPlanJson(TaskCategoryPlanner.parseCategories(categories))
        val clickTextsArray = JSONArray().apply {
            clickTexts.filter { it.isNotBlank() }.forEach { put(it) }
        }.toString()
        val activeTargetJson = if (activeClickText.isNullOrBlank()) "null" else JSONObject.quote(activeClickText)
        val rawCategoriesJson = JSONObject.quote(categories)

        val identityJson = JSONObject().apply {
            put("firstName", identity.firstName)
            put("lastName", identity.lastName)
            put("fullName", identity.fullName)
            put("email", identity.email)
            put("phone", identity.phone)
            put("address", identity.address)
            put("city", identity.city.ifBlank { extractedInfo?.city ?: "" })
            put("state", identity.state.ifBlank { extractedInfo?.region ?: "" })
            put("postalCode", identity.postalCode.ifBlank { extractedInfo?.postalCode ?: "10001" })
            put("country", identity.country.ifBlank { extractedInfo?.country ?: "United States" })
            put("birthDate", identity.birthDate)
            put("gender", identity.gender)
            put("username", identity.username)
            put("password", identity.password)
            put("cardNumber", identity.cardNumber)
            put("cardExpiry", identity.cardExpiry)
            put("cardCvv", identity.cardCvv)
            put("cardHolder", identity.cardHolder)
            put("cardType", identity.cardType)
            put("bankName", identity.bankName)
            put("ip", extractedInfo?.ip ?: "")
            put("timezone", extractedInfo?.timezone ?: "")
            put("language", extractedInfo?.language ?: "")
            put("isp", extractedInfo?.isp ?: "")
        }.toString()

        return """
(function() {
  window._cpaIdentity = $identityJson;
  window._cpaCategoryPlan = $planJson;
  window._cpaClickTexts = $clickTextsArray;
  window._cpaActiveClickText = $activeTargetJson;
  window._cpaCategoriesRaw = $rawCategoriesJson;
  window._cpaAnsweredQuestions = window._cpaAnsweredQuestions || {};

  function logCpa(msg) {
    console.log('[CPA Auto-Pilot] ' + msg);
  }

  function showFloatingBadge(text) {
    // Pure browsing mode: No DOM pollution or injected elements on the webpage
  }

  function setNativeValue(element, value) {
    if (!element || value === undefined || value === null) return;
    try {
      var lastValue = element.value;
      var prototype = element.tagName === 'INPUT' ? window.HTMLInputElement.prototype :
                      element.tagName === 'SELECT' ? window.HTMLSelectElement.prototype :
                      window.HTMLTextAreaElement.prototype;
      var setter = Object.getOwnPropertyDescriptor(prototype, 'value')?.set;
      if (setter) {
        setter.call(element, value);
      } else {
        element.value = value;
      }
      // React 16+ input tracker update
      var tracker = element._valueTracker;
      if (tracker) {
        tracker.setValue(lastValue);
      }
      element.dispatchEvent(new Event('input', { bubbles: true }));
      element.dispatchEvent(new Event('change', { bubbles: true }));
      element.dispatchEvent(new Event('blur', { bubbles: true }));
    } catch(err) {
      element.value = value;
      element.dispatchEvent(new Event('input', { bubbles: true }));
      element.dispatchEvent(new Event('change', { bubbles: true }));
    }
  }

  function isElementInViewport(el) {
    if (!el) return true;
    try {
      var rect = el.getBoundingClientRect();
      return (
        rect.top >= 0 &&
        rect.left >= 0 &&
        rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
        rect.right <= (window.innerWidth || document.documentElement.clientWidth)
      );
    } catch(e) { return true; }
  }

  function triggerClick(el) {
    if (!el) return;
    try {
      // Avoid excessive page movement: only gently scroll if element is not in view
      if (!isElementInViewport(el)) {
        el.scrollIntoView({ behavior: 'instant', block: 'nearest' });
      }
    } catch(e) {}

    var opts = { bubbles: true, cancelable: true, view: window };
    el.dispatchEvent(new MouseEvent('mouseenter', opts));
    el.dispatchEvent(new MouseEvent('mouseover', opts));
    el.dispatchEvent(new MouseEvent('mousedown', opts));
    el.focus();
    el.dispatchEvent(new MouseEvent('mouseup', opts));
    el.dispatchEvent(new MouseEvent('click', opts));
    if (typeof el.click === 'function') {
      el.click();
    }
  }

  function matchField(el) {
    var raw = ((el.name || '') + ' ' + (el.id || '') + ' ' + (el.placeholder || '') + ' ' + (el.getAttribute('aria-label') || '') + ' ' + (el.className || '')).toLowerCase();
    var type = (el.type || '').toLowerCase();

    if (type === 'email' || raw.match(/email|e-mail|correo|courriel/)) return 'email';
    if (raw.match(/areacode|area.?code|phone.?1|tel.?1|ph.?1/)) return 'phoneArea';
    if (raw.match(/prefix|phone.?2|tel.?2|ph.?2/)) return 'phonePrefix';
    if (raw.match(/line.?num|phone.?3|tel.?3|ph.?3/)) return 'phoneLine';
    if (type === 'tel' || raw.match(/phone|tel|mobile|cel|movil|telephone|contact/)) return 'phone';
    if (raw.match(/dob.?month|birth.?month|month.?of.?birth/)) return 'dobMonth';
    if (raw.match(/dob.?day|birth.?day|day.?of.?birth/)) return 'dobDay';
    if (raw.match(/dob.?year|birth.?year|year.?of.?birth/)) return 'dobYear';
    if (raw.match(/first.?name|fname|given.?name|prenom|vorname/)) return 'firstName';
    if (raw.match(/last.?name|lname|surname|family.?name|nom|nachname/)) return 'lastName';
    if (raw.match(/full.?name|your.?name|nombre.?completo|nom.?complet/)) return 'fullName';
    if (raw.match(/zip|postal|postcode|plz|cap|pincode/)) return 'postalCode';
    if (raw.match(/address|addr|street|rue|strasse|calle|via|direccion/)) return 'address';
    if (raw.match(/city|ville|stadt|ciudad|citta|town/)) return 'city';
    if (raw.match(/state|province|region|estado|departamento/)) return 'state';
    if (raw.match(/country|pays|land|pais/)) return 'country';
    if (raw.match(/birth|dob|birthday|date.?of.?birth/)) return 'birthDate';
    if (raw.match(/gender|sex|sexe/)) return 'gender';
    if (raw.match(/card.?number|cardnum|cc.?num|numero.?carte/)) return 'cardNumber';
    if (raw.match(/expiry|expiration|exp.?date|mm.?yy|valid/)) return 'cardExpiry';
    if (raw.match(/cvv|cvc|cvn|security.?code/)) return 'cardCvv';
    if (type === 'password' || raw.match(/password|passcode|pwd|secret|passwort|mot.?de.?passe|contrasena/)) return 'password';
    if (raw.match(/username|user.?name|login|usuario|utilisateur|pseudo|benutzer/)) return 'username';
    if (raw.match(/card.?holder|cardholder|name.?on.?card|nom.?sur.?la.?carte|titular/)) return 'cardHolder';
    if (raw.match(/exp.?month|card.?month|month.?of.?expir/)) return 'cardExpMonth';
    if (raw.match(/exp.?year|card.?year|year.?of.?expir/)) return 'cardExpYear';
    if (raw.match(/bank.?name|bank|issuing.?bank/)) return 'bankName';
    return null;
  }

  // --- Intelligent Survey Understanding Engine ---

  function getQuestionContext(element) {
    if (!element) return '';
    var curr = element.parentElement;
    var depth = 0;
    while (curr && depth < 6) {
      // Look for question headers inside container
      var headers = curr.querySelectorAll('h1, h2, h3, h4, h5, legend, [class*="question"], [class*="prompt"], [class*="title"], [class*="survey-header"], [id*="question"]');
      for (var h = 0; h < headers.length; h++) {
        var hEl = headers[h];
        if (hEl !== element && !element.contains(hEl)) {
          var txt = (hEl.innerText || hEl.textContent || '').trim().toLowerCase();
          if (txt.length > 3) return txt;
        }
      }
      // Look for preceding sibling with text
      var prev = curr.previousElementSibling;
      if (prev) {
        var ptxt = (prev.innerText || prev.textContent || '').trim().toLowerCase();
        if (ptxt.includes('?') || ptxt.includes('how') || ptxt.includes('what') || ptxt.includes('are you') || ptxt.includes('do you') || ptxt.includes('select') || ptxt.includes('choose')) {
          return ptxt;
        }
      }
      curr = curr.parentElement;
      depth++;
    }
    return '';
  }

  function pickBestSurveyOption(questionText, optionsList, ident) {
    if (!optionsList || optionsList.length === 0) return null;
    var q = (questionText || '').toLowerCase();

    function matches(opt, regex) {
      var str = ((opt.text || '') + ' ' + (opt.value || '')).toLowerCase();
      return regex.test(str);
    }

    // 1. Age & Majority Verification (CRITICAL for qualification)
    if (q.match(/age|18|years.?old|how.?old|birth|dob/)) {
      var adultOpt = optionsList.find(function(o) { return matches(o, /\b(yes|18\+|18-24|25-34|35-44|over 18|older)\b/); });
      if (adultOpt) return adultOpt;
      var nonMinor = optionsList.find(function(o) { return !matches(o, /\b(no|under 18|<18|17|minor)\b/); });
      if (nonMinor) return nonMinor;
    }

    // 2. US / Residency / Location Qualification
    if (q.match(/us.?resident|united.?states|citizen|live in the us|country|residence/)) {
      var usOpt = optionsList.find(function(o) { return matches(o, /\b(yes|united states|usa|us)\b/); });
      if (usOpt) return usOpt;
      var nonNo = optionsList.find(function(o) { return !matches(o, /\bno\b/); });
      if (nonNo) return nonNo;
    }

    // 3. Gender / Sex Matching
    if (q.match(/gender|sex|are you male|man or woman/)) {
      var isFemale = ident && ((ident.gender && ident.gender.toLowerCase().includes('female')) || (ident.firstName && /^(mary|patricia|jennifer|linda|elizabeth|barbara|susan|jessica|sarah|karen|nancy|lisa|betty|margaret|sandra|ashley|kimberly|emily|donna|michelle|carol|amanda|melissa|deborah|stephanie|rebecca|sharon|laura|cynthia|kathleen|amy|shirley|angela|helen|anna|brenda|pamela|nicole|emma|samantha|katherine|christine|debra|rachel|catherine|carolyn|janet|ruth|maria|heather|diane|virginia|julie|joyce|victoria|olivia|kelly|christina|lauren|joan|evelyn|judith|megan|cheryl|andrea|hannah|martha|jacqueline|frances|gloria|ann|teresa|kathryn|sara|janice|jean|alice|madison|doris|abigail|julia|judy|grace|denise|amber|marilyn|beverly|danielle|theresa|sophia|marie|diana|brittany|natalie|isabella|charlotte|rose|kayla|alexis)/i.test(ident.firstName)));
      if (isFemale) {
        var femOpt = optionsList.find(function(o) { return matches(o, /\b(female|woman|f|femme)\b/); });
        if (femOpt) return femOpt;
      } else {
        var maleOpt = optionsList.find(function(o) { return matches(o, /\b(male|man|m|homme)\b/); });
        if (maleOpt) return maleOpt;
      }
      return optionsList[0];
    }

    // 4. Shopping, Online Habits, Smartphone, Device
    if (q.match(/shop|online|internet|smartphone|mobile|device|phone|buy|store|amazon|walmart/)) {
      var frequentOpt = optionsList.find(function(o) { return matches(o, /\b(yes|daily|weekly|often|frequently|regularly|always|iphone|android|yes, i do)\b/); });
      if (frequentOpt) return frequentOpt;
    }

    // 5. Employment & Occupation
    if (q.match(/employ|job|work|occupation|career/)) {
      var empOpt = optionsList.find(function(o) { return matches(o, /\b(employed|full.?time|yes|professional)\b/); });
      if (empOpt) return empOpt;
    }

    // 6. Household Income (pick middle/upper-middle tier)
    if (q.match(/income|earn|salary|household/)) {
      var midIncome = optionsList.find(function(o) { return matches(o, /(50|60|75|80|100)k?|\$50|\$75/); });
      if (midIncome) return midIncome;
      if (optionsList.length >= 3) return optionsList[Math.floor(optionsList.length / 2)];
    }

    // 7. Homeownership
    if (q.match(/own or rent|homeowner|housing/)) {
      var ownOpt = optionsList.find(function(o) { return matches(o, /\b(own|homeowner|house)\b/); });
      if (ownOpt) return ownOpt;
    }

    // 8. Co-Reg Sponsor Deals / Upsells / Paid Offers / Insurance / Credit Cards
    // In co-reg walls, to proceed without payment, click "No thanks", "Skip", "Not interested"
    if (q.match(/special offer|sponsor|deal|partner|free trial|sign up for|subscription|quote|insurance|solar|card offer/)) {
      var passOpt = optionsList.find(function(o) { return matches(o, /\b(no thanks|no, thanks|no|skip|pass|not interested|continue without|no thank you|not at this time)\b/); });
      if (passOpt) return passOpt;
    }

    // 9. Ratings / Scales (e.g. 1 to 5 or 1 to 10)
    var numOptions = optionsList.filter(function(o) { return /^\d+$/.test((o.text || '').trim()); });
    if (numOptions.length >= 4) {
      return numOptions[numOptions.length - 2] || numOptions[numOptions.length - 1];
    }

    // 10. Binary Yes / No (Qualification questions almost always require "Yes")
    var yesOpt = optionsList.find(function(o) { return matches(o, /^\s*(yes|oui|si|agree|correct|definitely|absolutely)\b/); });
    var noOpt = optionsList.find(function(o) { return matches(o, /^\s*(no|non|disagree)\b/); });
    if (yesOpt && noOpt) {
      return yesOpt;
    }

    // 11. Positive sentiment matching
    var positiveOpt = optionsList.find(function(o) { return matches(o, /\b(yes|interested|agree|claim|participate|confirm|enter|proceed)\b/); });
    if (positiveOpt) return positiveOpt;

    // 12. Skip placeholders like "Select...", "Choose one..."
    var nonPlaceholder = optionsList.find(function(o) { return !matches(o, /\b(select|choose|pick|--|none)\b/); });
    if (nonPlaceholder) return nonPlaceholder;

    return optionsList[0];
  }

  // --- Handlers for Different Survey Types ---

  function handleButtonSurveys(ident) {
    // Find all choice buttons / cards inside survey containers or option lists
    var choiceSelectors = [
      'button:not([type=submit]):not([id*="submit"]):not([class*="submit"]):not([class*="continue"])',
      '[role="button"]:not([class*="submit"]):not([class*="continue"])',
      '.survey-btn', '.quiz-option', '.answer', '.choice', '.option-card', '.btn-option',
      '[data-answer]', '[data-choice]', '[data-value]', '.poll-option', '.survey-tile',
      'a.btn:not([class*="submit"]):not([class*="continue"])'
    ];

    var allButtons = Array.from(document.querySelectorAll(choiceSelectors.join(',')));
    if (allButtons.length === 0) return false;

    // Group buttons by parent container (representing a single question)
    var parentMap = new Map();
    for (var i = 0; i < allButtons.length; i++) {
      var btn = allButtons[i];
      if (btn.disabled || btn.offsetParent === null) continue;

      var parent = btn.closest('.survey-step, .question-container, .question, .quiz-step, .step, fieldset, .answers, .options, form') || btn.parentElement;
      if (!parentMap.has(parent)) {
        parentMap.set(parent, []);
      }
      parentMap.get(parent).push(btn);
    }

    var now = Date.now();
    var acted = false;

    parentMap.forEach(function(buttons, parent) {
      if (acted || buttons.length < 2) return;

      // Check if any button in this group is already selected/active
      var isAnyActive = buttons.some(function(b) {
        return b.classList.contains('active') || b.classList.contains('selected') || b.getAttribute('aria-selected') === 'true';
      });
      if (isAnyActive) return;

      var qText = getQuestionContext(parent) || getQuestionContext(buttons[0]);
      var qKey = (qText || '') + '_' + buttons.length;

      // Anti-loop protection: don't click the exact same question group within 2.5 seconds
      if (window._cpaAnsweredQuestions[qKey] && (now - window._cpaAnsweredQuestions[qKey] < 2500)) {
        return;
      }

      var optionsList = buttons.map(function(b) {
        return {
          el: b,
          text: (b.innerText || b.textContent || b.getAttribute('aria-label') || '').trim(),
          value: b.getAttribute('data-value') || b.getAttribute('value') || ''
        };
      });

      var best = pickBestSurveyOption(qText, optionsList, ident);
      if (best && best.el) {
        window._cpaAnsweredQuestions[qKey] = now;
        var chosenText = best.text || best.value || 'Selected';
        logCpa('Intelligent Survey Button Clicked: "' + chosenText + '" for question: "' + qText.slice(0, 40) + '..."');
        showFloatingBadge('⚡ Survey: ' + chosenText.slice(0, 16));
        triggerClick(best.el);
        acted = true;
      }
    });

    return acted;
  }

  function handleRadioSurveys(ident) {
    var radios = Array.from(document.querySelectorAll('input[type=radio]'));
    var groups = {};
    for (var i = 0; i < radios.length; i++) {
      var r = radios[i];
      if (r.disabled || r.offsetParent === null) continue;
      var name = r.name || ('radio_group_' + i);
      if (!groups[name]) groups[name] = [];
      groups[name].push(r);
    }

    var answeredCount = 0;
    for (var g in groups) {
      var groupRadios = groups[g];
      var anyChecked = groupRadios.some(function(r) { return r.checked; });
      if (!anyChecked && groupRadios.length > 0) {
        var qText = getQuestionContext(groupRadios[0]);
        var optionsList = groupRadios.map(function(r) {
          var labelText = r.parentElement ? (r.parentElement.innerText || r.parentElement.textContent || '') : '';
          return {
            el: r,
            text: labelText.trim(),
            value: r.value || ''
          };
        });

        var best = pickBestSurveyOption(qText, optionsList, ident);
        if (best && best.el) {
          best.el.checked = true;
          best.el.dispatchEvent(new Event('change', { bubbles: true }));
          best.el.dispatchEvent(new Event('click', { bubbles: true }));
          logCpa('Survey Radio Selected: "' + (best.text || best.value) + '"');
          showFloatingBadge('⚡ Radio: ' + (best.text || best.value).slice(0, 16));
          answeredCount++;
        }
      }
    }
    return answeredCount;
  }

  function handleSelectSurveys(ident) {
    var selects = Array.from(document.querySelectorAll('select'));
    var selectAnswered = 0;

    for (var i = 0; i < selects.length; i++) {
      var sel = selects[i];
      if (sel.disabled || sel.offsetParent === null) continue;
      if (sel.selectedIndex > 0 && sel.value && sel.value.trim().length > 0) continue;

      var qText = getQuestionContext(sel) || sel.name || sel.id || '';
      var options = Array.from(sel.options);
      if (options.length <= 1) continue;

      var optionsList = options.map(function(opt, idx) {
        return {
          el: opt,
          index: idx,
          text: (opt.text || '').trim(),
          value: opt.value || ''
        };
      });

      var best = pickBestSurveyOption(qText, optionsList, ident);
      if (best && best.index !== undefined) {
        sel.selectedIndex = best.index;
        sel.dispatchEvent(new Event('change', { bubbles: true }));
        logCpa('Survey Dropdown Selected: "' + best.text + '"');
        showFloatingBadge('⚡ Select: ' + best.text.slice(0, 16));
        selectAnswered++;
      }
    }
    return selectAnswered;
  }

  function handleCheckboxes() {
    var checkboxes = document.querySelectorAll('input[type=checkbox]');
    var checkedCount = 0;
    for (var i = 0; i < checkboxes.length; i++) {
      var cb = checkboxes[i];
      if (cb.checked || cb.disabled || cb.offsetParent === null) continue;

      var info = ((cb.name || '') + ' ' + (cb.id || '') + ' ' + (cb.className || '') + ' ' + (cb.getAttribute('aria-label') || '')).toLowerCase();
      var parentText = (cb.parentElement ? cb.parentElement.innerText : '').toLowerCase();
      var isTermsOrRequired = cb.required || 
        info.match(/agree|terms|condition|privacy|policy|optin|subscribe|age|18|consent|accept|rule|confirm/) ||
        parentText.match(/agree|terms|condition|privacy|policy|18 years|opt-in|subscribe|rules|i accept|i agree/);

      if (isTermsOrRequired) {
        cb.checked = true;
        cb.dispatchEvent(new Event('change', { bubbles: true }));
        cb.dispatchEvent(new Event('click', { bubbles: true }));
        checkedCount++;
      }
    }
    return checkedCount;
  }

  function isHoneypot(el) {
    try {
      if (!el) return true;
      var t = (el.type || '').toLowerCase();
      if (t === 'hidden') return true;
      var style = window.getComputedStyle(el);
      if (style && (style.display === 'none' || style.visibility === 'hidden' || parseFloat(style.opacity) === 0)) return true;
      var r = el.getBoundingClientRect();
      if ((r.width === 0 && r.height === 0) || r.left < -2000 || r.top < -2000) return true;
      var nm = ((el.name || '') + ' ' + (el.id || '') + ' ' + (el.className || '')).toLowerCase();
      if (nm.match(/honeypot|honey|trap|bot-field|spam|do-not-fill|hidden-field/)) return true;
      if (el.hasAttribute && el.hasAttribute('tabindex') && el.getAttribute('tabindex') === '-1' && (el.value || '') === '') {
        if (nm.match(/website|url|company|fax|middle/)) return true;
      }
    } catch(e) {}
    return false;
  }

  function fillInputFields() {
    var ident = window._cpaIdentity;
    if (!ident) return 0;

    var inputs = Array.from(document.querySelectorAll('input:not([type=hidden]):not([type=submit]):not([type=button]):not([type=checkbox]):not([type=radio]), select, textarea'));
    // Expert: human tab-order — top-to-bottom, skip honeypots (bot traps)
    inputs = inputs.filter(function(el) { return !isHoneypot(el); });
    inputs.sort(function(a, b) {
      try {
        var ra = a.getBoundingClientRect(), rb = b.getBoundingClientRect();
        if (Math.abs(ra.top - rb.top) > 20) return ra.top - rb.top;
        return ra.left - rb.left;
      } catch(e) { return 0; }
    });
    // Persona facts memory: consistent age/gender across multi-page funnels
    window._cpaPersonaFacts = window._cpaPersonaFacts || {};
    if (ident.birthDate) {
      var __y = parseInt((ident.birthDate || '1995').substring(0, 4)) || 1995;
      window._cpaPersonaFacts.age = 2026 - __y;
      window._cpaPersonaFacts.gender = (ident.gender || '').toLowerCase();
    }
    var filled = 0;

    var phoneDigits = (ident.phone || '2125550199').replace(/\D/g, '');
    var dobParts = (ident.birthDate || '1995-06-15').split(/[-/]/);
    var dobYear = dobParts[0] && dobParts[0].length === 4 ? dobParts[0] : (dobParts[2] || '1995');
    var dobMonth = dobParts[0].length === 4 ? (dobParts[1] || '06') : (dobParts[0] || '06');
    var dobDay = dobParts[0].length === 4 ? (dobParts[2] || '15') : (dobParts[1] || '15');

    for (var i = 0; i < inputs.length; i++) {
      var el = inputs[i];
      if (el.value && el.value.trim().length > 0 && el.tagName !== 'SELECT') {
        continue;
      }

      var field = matchField(el);
      var value = null;

      if (field === 'firstName') value = ident.firstName;
      else if (field === 'lastName') value = ident.lastName;
      else if (field === 'fullName') value = ident.fullName;
      else if (field === 'email') value = ident.email;
      else if (field === 'phone') value = ident.phone;
      else if (field === 'phoneArea') value = phoneDigits.slice(0, 3);
      else if (field === 'phonePrefix') value = phoneDigits.slice(3, 6);
      else if (field === 'phoneLine') value = phoneDigits.slice(6, 10);
      else if (field === 'dobMonth') value = dobMonth;
      else if (field === 'dobDay') value = dobDay;
      else if (field === 'dobYear') value = dobYear;
      else if (field === 'address') value = ident.address;
      else if (field === 'city') value = ident.city;
      else if (field === 'state') value = ident.state;
      else if (field === 'postalCode') value = ident.postalCode;
      else if (field === 'country') value = ident.country;
      else if (field === 'birthDate') value = ident.birthDate;
      else if (field === 'gender') value = ident.gender;
      else if (field === 'username') value = ident.username;
      else if (field === 'password') value = ident.password;
      else if (field === 'cardNumber' && ident.cardNumber) value = ident.cardNumber.replace(/\s/g, '');
      else if (field === 'cardExpiry' && ident.cardExpiry) value = ident.cardExpiry;
      else if (field === 'cardCvv' && ident.cardCvv) value = ident.cardCvv;
      else if (field === 'cardHolder' && ident.cardHolder) value = ident.cardHolder;
      else if (field === 'cardExpMonth' && ident.cardExpiry) {
        var mMatch = ident.cardExpiry.match(/^(\d{1,2})/);
        value = mMatch ? mMatch[1].padStart(2, '0') : '12';
      }
      else if (field === 'cardExpYear' && ident.cardExpiry) {
        var yMatch = ident.cardExpiry.match(/\/(\d{2,4})$/);
        var yr = yMatch ? yMatch[1] : '28';
        value = yr.length === 2 ? '20' + yr : yr;
      }
      else if (field === 'bankName' && ident.bankName) value = ident.bankName;

      // Smart Fallback for single-field landing pages (e.g. "Enter your email to claim $100")
      if (!value && inputs.length === 1 && (el.type === 'text' || el.type === 'email' || !el.type)) {
        value = ident.email;
      }

      if (value && el.tagName === 'SELECT') {
        var opts = el.options;
        var valLower = value.toString().toLowerCase();
        for (var j = 0; j < opts.length; j++) {
          var optText = opts[j].text.toLowerCase();
          var optVal = (opts[j].value || '').toLowerCase();
          if (optText === valLower || optVal === valLower || optText.includes(valLower) || optVal.includes(valLower.slice(0, 3))) {
            el.selectedIndex = j;
            el.dispatchEvent(new Event('change', { bubbles: true }));
            filled++;
            break;
          }
        }
      } else if (value) {
        setNativeValue(el, value);
        filled++;
      }
    }
    return filled;
  }

  function findAndClickSubmit() {
    var buttons = Array.from(document.querySelectorAll('button, input[type=submit], input[type=button], a[role=button], [class*="btn"], [class*="submit"], [class*="cta"], [id*="submit"], [id*="continue"], [class*="continue"], [class*="next"]'));
    var keywords = ['continue', 'next', 'submit', 'claim', 'enter', 'get started', 'proceed', 'start', 'join', 'sign up', 'agree', 'yes', 'participate', 'finish', 'go', 'win', 'reward', 'next question', 'claim reward', 'confirm'];

    for (var k = 0; k < buttons.length; k++) {
      var btn = buttons[k];
      if (btn.disabled || btn.offsetParent === null) continue;

      var txt = ((btn.textContent || '') + ' ' + (btn.value || '') + ' ' + (btn.getAttribute('aria-label') || '') + ' ' + (btn.id || '') + ' ' + (btn.className || '')).toLowerCase().trim();
      if (keywords.some(function(kw) { return txt.includes(kw); })) {
        logCpa('Auto-clicking action button: "' + (btn.textContent || btn.value || '').trim() + '"');
        showFloatingBadge('⚡ Action: ' + (btn.textContent || btn.value || 'Continue').trim().slice(0, 16) + '...');
        triggerClick(btn);
        return true;
      }
    }

    // Fallback: Submit form if available
    var forms = document.querySelectorAll('form');
    if (forms.length > 0) {
      for (var f = 0; f < forms.length; f++) {
        var submitBtn = forms[f].querySelector('[type=submit], button');
        if (submitBtn) {
          triggerClick(submitBtn);
          return true;
        }
      }
    }
    return false;
  }

  function handleSkipUpsells() {
    // 1. Close intrusive modals or promotional popups if blocking
    try {
      var closeBtns = document.querySelectorAll('.modal-close, .popup-close, [aria-label="Close"], .close-button, .btn-close, .dialog-close');
      for (var c = 0; c < closeBtns.length; c++) {
        if (closeBtns[c].offsetParent !== null) {
          triggerClick(closeBtns[c]);
          return true;
        }
      }
    } catch(e) {}

    // 2. Click skip/decline upsell links
    var skipKeywords = ['no thanks', 'skip', 'not interested', 'no thank you', 'skip this offer', 'continue without offer', 'decline', 'pass', 'maybe later', 'no, thanks', 'no, thank you', 'skip offer', 'i do not want this', 'continue to final step', 'skip deal', 'no, keep current'];
    var clickableElements = Array.from(document.querySelectorAll('a, button, input[type=button], span[role=button], div[role=button]'));
    for (var i = 0; i < clickableElements.length; i++) {
      var el = clickableElements[i];
      if (el.offsetParent === null) continue;
      var text = (el.innerText || el.textContent || el.getAttribute('aria-label') || el.value || '').toLowerCase().trim();
      if (skipKeywords.some(function(kw) { return text === kw || text.indexOf(kw) !== -1; })) {
        logCpa('Smart Upsell Skipped: "' + text.slice(0, 30) + '"');
        showFloatingBadge('⏭️ Skipped Offer');
        triggerClick(el);
        return true;
      }
    }
    return false;
  }

  function handleSignUpFields(ident) {
    var pwInputs = Array.from(document.querySelectorAll('input[type=password]'));
    var filledPw = 0;
    if (pwInputs.length > 0) {
      var generatedPassword = 'CpaPass@' + (ident.firstName || 'User') + '2026!';
      pwInputs.forEach(function(pw) {
        if (!pw.value || pw.value.trim().length === 0) {
          setNativeValue(pw, generatedPassword);
          filledPw++;
        }
      });
      if (filledPw > 0) {
        logCpa('Sign Up: Generated and filled secure password for ' + filledPw + ' fields');
        showFloatingBadge('👤 Sign Up Password Filled');
      }
    }
    return filledPw;
  }

  function handleOfferClick() {
    if (window._cpaOfferClicked) return false;

    // Strict category guard: Click is ONLY activated when selected in categories!
    var plan = window._cpaCategoryPlan || [];
    var rawCats = (window._cpaCategoriesRaw || '').toLowerCase();
    var isOfferClickCategoryEnabled = plan.some(function(p) { return p.id === 'offer_click'; }) ||
      rawCats.indexOf('offer_click') !== -1 ||
      rawCats.indexOf('النقرة') !== -1 ||
      rawCats.indexOf('نقر') !== -1;

    if (!isOfferClickCategoryEnabled) {
      return false;
    }

    var targetList = [];
    if (window._cpaActiveClickText && typeof window._cpaActiveClickText === 'string' && window._cpaActiveClickText.trim().length > 0) {
      targetList.push(window._cpaActiveClickText.trim().toLowerCase());
    }
    if (window._cpaClickTexts && Array.isArray(window._cpaClickTexts)) {
      for (var t = 0; t < window._cpaClickTexts.length; t++) {
        var str = (window._cpaClickTexts[t] || '').trim().toLowerCase();
        if (str && targetList.indexOf(str) === -1) {
          targetList.push(str);
        }
      }
    }
    // If no explicit targets specified by user or task, do not click random elements!
    if (targetList.length === 0) {
      return false;
    }

    function checkMatch(elementText, target) {
      if (!elementText || !target) return false;
      if (elementText.indexOf(target) !== -1) return true;
      var cleanEl = elementText.replace(/[^a-z0-9]/g, ' ').replace(/\s+/g, ' ').trim();
      var cleanTarget = target.replace(/[^a-z0-9]/g, ' ').replace(/\s+/g, ' ').trim();
      if (cleanEl.indexOf(cleanTarget) !== -1) return true;
      var tokens = cleanTarget.split(' ').filter(function(w) { return w.length >= 3; });
      if (tokens.length >= 2) {
        var allIn = tokens.every(function(tk) { return cleanEl.indexOf(tk) !== -1; });
        if (allIn) return true;
      }
      return false;
    }

    function scanAndClickOffer() {
      // Scan interactive and semantic elements: links, buttons, headers, cards
      var candidates = Array.from(document.querySelectorAll('a, button, [role=button], h1, h2, h3, h4, h5, p, span, div, strong, b, [class*="offer"], [class*="cta"], [class*="btn"], [class*="item"], [class*="card"]'));
      for (var i = 0; i < candidates.length; i++) {
        var el = candidates[i];
        if (el.offsetParent === null && el.offsetWidth === 0 && el.offsetHeight === 0) continue; // Skip hidden elements

        var txt = ((el.innerText || el.textContent || '') + ' ' + (el.getAttribute('aria-label') || '') + ' ' + (el.title || '')).toLowerCase().trim();
        if (!txt || txt.length < 2) continue;

        for (var k = 0; k < targetList.length; k++) {
          var targetWord = targetList[k];
          if (checkMatch(txt, targetWord)) {
            window._cpaOfferClicked = true;
            logCpa('🎯 [النقرة على العرض]: تم العثور على العرض الهدف: "' + targetWord + '"');
            showFloatingBadge('🎯 Clicked: ' + targetWord.slice(0, 16) + '...');

            var clickable = el.closest('a, button, [role=button]') || el;
            var href = clickable.href || (clickable.getAttribute ? clickable.getAttribute('href') : null);

            // If link opens in new tab (target="_blank"), modify to load in place or notify
            if (clickable.tagName === 'A' && clickable.target === '_blank') {
              clickable.target = '_self';
            }

            if (window.AndroidBridge && window.AndroidBridge.onOfferClicked) {
              try {
                window.AndroidBridge.onOfferClicked(targetWord, href || window.location.href);
              } catch(e) {}
            }

            triggerClick(clickable);

            // Force redirect if href is an external offer link and didn't fire
            if (href && href.startsWith('http') && href !== window.location.href) {
              setTimeout(function() {
                window.location.href = href;
              }, 400);
            }
            return true;
          }
        }
      }
      return false;
    }

    // Attempt 1: Direct scan in current visible DOM
    if (scanAndClickOffer()) return true;

    // Attempt 2: Dynamic JavaScript Menu / Drawer Triggering
    // On many sites, the offer list is rendered or shown via a JavaScript script or dropdown/toggle.
    if (!window._cpaMenuRevealed) {
      window._cpaMenuRevealed = true;

      // Check for inline JS functions that reveal menus/offers
      var jsFunctions = ['showOffers', 'toggleMenu', 'openMenu', 'openOffers', 'loadOffers', 'showList', 'displayOffers', 'renderOffers', 'unlockOffers'];
      for (var j = 0; j < jsFunctions.length; j++) {
        var fn = jsFunctions[j];
        if (typeof window[fn] === 'function') {
          try {
            logCpa('⚡ تشغيل سكريبت القائمة بالجافاسكريبت: ' + fn + '()');
            window[fn]();
            setTimeout(scanAndClickOffer, 350);
            return true;
          } catch(e) {}
        }
      }

      // Check for menu/dropdown buttons and toggles
      var menuToggles = document.querySelectorAll(
        'button.navbar-toggler, button.hamburger, .menu-toggle, .nav-toggle, [data-toggle="collapse"], [data-toggle="dropdown"], [aria-label*="menu" i], .dropdown-toggle, #menuBtn, #openMenu, button[id*="menu" i], button[class*="menu" i], [class*="offer-btn"], [class*="unlock-btn"], [id*="offer-btn"]'
      );
      for (var m = 0; m < menuToggles.length; m++) {
        var toggleBtn = menuToggles[m];
        if (toggleBtn.offsetParent !== null) {
          logCpa('⚡ فتح القائمة / النافذة لإظهار العروض المخفية');
          triggerClick(toggleBtn);
          setTimeout(scanAndClickOffer, 350);
          return true;
        }
      }
    }

    return false;
  }

  // --- Dynamic Cookie / GDPR Modal Bypass ---
  function dismissCookieBanners() {
    try {
      var cookieKeywords = ['accept all', 'allow all', 'agree', 'accept cookies', 'i accept', 'i agree', 'got it', 'understand', 'allow cookies'];
      var btns = Array.from(document.querySelectorAll('button, a[role=button], .cookie-btn, #cookie-btn, .consent-btn'));
      for (var b = 0; b < btns.length; b++) {
        var el = btns[b];
        if (el.offsetParent === null) continue;
        var txt = (el.innerText || el.textContent || '').toLowerCase().trim();
        if (cookieKeywords.some(function(k) { return txt === k || txt.startsWith(k); })) {
          logCpa('Bypassed cookie banner: ' + txt);
          triggerClick(el);
          return true;
        }
      }
    } catch(e) {}
    return false;
  }

  // --- Dynamic Page Action Map Generator (خريطة عمل الصفحة التلقائية) ---
  function generateAndReportActionMap() {
    try {
      var actionMap = [];
      var order = 1;

      // 1. Initial human scroll & view
      actionMap.push({
        order: order++,
        title: 'محاكاة استعراض الصفحة والتمرير الطبيعي',
        type: 'scroll',
        target: 'body',
        status: 'done',
        details: 'تمرير متدرج للتأكد من ظهور كافة العناصر'
      });

      // 2. Cookie banner check
      var hasCookieBanner = document.querySelector('[class*="cookie"], [id*="cookie"], [class*="consent"], [id*="consent"]');
      if (hasCookieBanner) {
        actionMap.push({
          order: order++,
          title: 'تخطي نافذة ملفات تعريف الارتباط (Cookie / GDPR)',
          type: 'click',
          target: 'cookie_banner',
          status: 'executing',
          details: 'إغلاق الإشعار للسماح بالوصول للأزرار والحقول'
        });
      }

      // 3. Offer Click check
      var rawCats = (window._cpaCategoriesRaw || '').toLowerCase();
      var plan = window._cpaCategoryPlan || [];
      var hasOfferClick = plan.some(function(p) { return p.id === 'offer_click'; }) ||
        rawCats.indexOf('offer_click') !== -1 || rawCats.indexOf('النقرة') !== -1 || rawCats.indexOf('نقر') !== -1;
      if (hasOfferClick && !window._cpaOfferClicked) {
        actionMap.push({
          order: order++,
          title: 'النقر على العرض الهدف (#1 في الأولوية)',
          type: 'click',
          target: window._cpaActiveClickText || 'Offer Link',
          status: 'executing',
          details: 'البحث عن نص العرض المحدد والنقر التلقائي للتحويل لموقع العرض'
        });
      }

      // 4. Questions / Survey / Quiz check
      var radios = Array.from(document.querySelectorAll('input[type=radio]'));
      var surveyBtns = Array.from(document.querySelectorAll('.survey-btn, .quiz-option, [data-choice], button:not([type=submit]):not([class*="submit"])'));
      if (radios.length >= 2 || surveyBtns.length >= 2) {
        var sampleQ = getQuestionContext(radios[0] || surveyBtns[0]) || 'سؤال استبيان تأهيلي';
        actionMap.push({
          order: order++,
          title: 'فهم وتكيّف سؤال الاستبيان: ' + (sampleQ.length > 25 ? sampleQ.slice(0, 25) + '...' : sampleQ),
          type: 'extract',
          target: 'survey_options',
          status: 'executing',
          details: 'استخراج منطوق السؤال واختيار الإجابة الأكثر ملاءمة للشخصية'
        });
      }

      // 5. Input fields check
      var inputs = Array.from(document.querySelectorAll('input:not([type=hidden]):not([type=submit]):not([type=button]):not([type=checkbox]):not([type=radio]), select, textarea'));
      if (inputs.length > 0) {
        actionMap.push({
          order: order++,
          title: 'ملء حقول البيانات (' + inputs.length + ' حقول مكتشفة)',
          type: 'fill',
          target: 'form_inputs',
          status: 'executing',
          details: 'إدراج البريد، الاسم، العنوان، الهاتف والرمز البريدي من شاشة المعلومات'
        });
      }

      // 6. Checkboxes (terms & majority age)
      var checkboxes = Array.from(document.querySelectorAll('input[type=checkbox]'));
      if (checkboxes.length > 0) {
        actionMap.push({
          order: order++,
          title: 'الموافقة على الشروط وتأكيد السن القانوني (18+)',
          type: 'check',
          target: 'checkboxes',
          status: 'executing',
          details: 'تحديد مربعات الشروط الإلزامية للمتابعة'
        });
      }

      // 7. Skip upsells check
      var hasSkip = Array.from(document.querySelectorAll('button, a, [role=button]')).some(function(b) {
        var t = (b.innerText || b.textContent || b.value || '').toLowerCase();
        return t.match(/no thanks|skip|not interested/);
      });
      if (hasSkip) {
        actionMap.push({
          order: order++,
          title: 'تخطي العروض الدعائية والرعاة (Skip / No Thanks)',
          type: 'click',
          target: 'skip_button',
          status: 'executing',
          details: 'النقر على زر التخطي لتفادي طلبات الدفع أو العروض الإضافية'
        });
      }

      // 8. Action button / Submit
      actionMap.push({
        order: order++,
        title: 'نقر زر المتابعة / إرسال التحويل (CTA Submit)',
        type: 'click',
        target: 'submit_button',
        status: 'pending',
        details: 'نقر زر الإرسال أو التالي للانتقال للمرحلة القادمة'
      });

      if (window.AndroidBridge && typeof window.AndroidBridge.onPageActionMapGenerated === 'function') {
        window.AndroidBridge.onPageActionMapGenerated(JSON.stringify(actionMap));
      }
    } catch(e) {}
  }

  // --- Main Execution Cycle ---

  window._cpaExecuteCycle = function() {
    var ident = window._cpaIdentity;
    if (!ident) return;

    var plan = window._cpaCategoryPlan || [];
    var rawCats = (window._cpaCategoriesRaw || '').toLowerCase();

    // 0. LOCKER & GATEKEEPER PROTECTION GUARD:
    // If a CPA Content Locker is present or expected, STRICTLY SUPPRESS all background page form-filling,
    // input typing, survey answering, and submit clicking!
    // The automator must NEVER touch or interact with the publisher's landing page!
    var hasLockerScript = !!document.querySelector(
      'script[src*="script_include"], script[src*="alignmentfiles"], script[src*="cpagrip"], script[src*="filetrkr"], script[src*="ogads"], script[src*="cpabuild"]'
    );
    var hasLockerElement = !!document.querySelector(
      '#InlineBoxMainOuterLayer, #InlineBoxDiv, #the_box, iframe[src*="alignmentfiles"], iframe[src*="load_box"], iframe[src*="cpagrip"], iframe[src*="ogads"], iframe[src*="cpabuild"], iframe[src*="locker"], div[class*="cpa_locker"], div[id*="cpa_locker"], div[class*="locker-offers"]'
    );
    var isLockerPlan = plan.some(function(p) { return p.id === 'content_locker' || p.id === 'wait_locker' || p.id === 'click_locker_offer'; }) ||
      rawCats.indexOf('locker') !== -1 || rawCats.indexOf('لوكر') !== -1 || rawCats.indexOf('قفل') !== -1;

    if ((hasLockerScript || hasLockerElement || isLockerPlan) && !window.__cpa_locker_offer_clicked) {
      logCpa('Content Locker is active / expected. Background page form interactions are strictly suspended until offer click.');
      return;
    }

    // Dismiss cookie banners only on the target offer page
    dismissCookieBanners();

    // Generate dynamic page action map for user inspection on offer page
    generateAndReportActionMap();

    var hasOfferClickPlan = plan.some(function(p) { return p.id === 'offer_click'; }) ||
      rawCats.indexOf('offer_click') !== -1 ||
      rawCats.indexOf('النقرة') !== -1 ||
      rawCats.indexOf('نقر') !== -1;
    var hasSkipPlan = plan.some(function(p) { return p.id === 'skip_upsells'; });
    var hasSignUpPlan = plan.some(function(p) { return p.id === 'sign_up'; });
    var hasSurveyPlan = plan.some(function(p) { return p.id === 'survey_quiz'; });

    // 0. PRIORITY #1 - OFFER CLICK only runs when selected in categories!
    if (hasOfferClickPlan && !window._cpaOfferClicked) {
      if (handleOfferClick()) {
        logCpa('Offer Click executed. Holding cycle for destination offer site to load.');
        return;
      }
    }

    // 0.5. Handle Skip Upsells if present on page
    if (hasSkipPlan || plan.length === 0) {
      if (handleSkipUpsells()) return;
    }

    // 1. Fill Text / Contact Inputs
    var filled = fillInputFields();

    // 2. Handle Password / Sign Up credentials if required
    if (hasSignUpPlan || document.querySelector('input[type=password]')) {
      filled += handleSignUpFields(ident);
    }

    // 3. Check Terms / Consent Checkboxes
    var checked = handleCheckboxes();

    // 4. Handle Radio Button Surveys
    var radios = handleRadioSurveys(ident);

    // 5. Handle Dropdown Surveys
    var selects = handleSelectSurveys(ident);

    // 6. Handle Button / Card / Tile Surveys
    var buttonSurveyHandled = handleButtonSurveys(ident);

    if (filled > 0 || checked > 0 || radios > 0 || selects > 0) {
      logCpa('Cycle update: filled ' + filled + ', checked ' + checked + ', radios ' + radios + ', selects ' + selects);
      // Brief human delay before clicking Next/Submit
      setTimeout(function() {
        findAndClickSubmit();
      }, 700);
    } else if (!buttonSurveyHandled) {
      // Check if all visible fields are filled and a submit button is ready
      var visibleInputs = Array.from(document.querySelectorAll('input:not([type=hidden]):not([type=submit]):not([type=button]):not([type=checkbox]):not([type=radio])'));
      var allFilled = visibleInputs.length > 0 && visibleInputs.every(function(i) { return i.value && i.value.trim().length > 0; });
      if (allFilled) {
        findAndClickSubmit();
      }
    }
  };

  // Run cycle immediately
  window._cpaExecuteCycle();

  // Install continuous observer and interval if not already running
  if (!window._cpaObserverInstalled) {
    window._cpaObserverInstalled = true;
    showFloatingBadge('⚡ Smart Auto-Pilot: Active');

    setInterval(function() {
      if (typeof window._cpaExecuteCycle === 'function') {
        window._cpaExecuteCycle();
      }
    }, 1100);

    var obs = new MutationObserver(function() {
      if (typeof window._cpaExecuteCycle === 'function') {
        window._cpaExecuteCycle();
      }
    });

    if (document.body) {
      obs.observe(document.body, { childList: true, subtree: true });
    }
  }
})();
true;
        """.trimIndent()
    }

    fun buildHumanBehaviorScript(): String {
        return """
(function() {
  // Gentle human presence without disruptive page movement
  try {
    var x = Math.floor(Math.random() * 200 + 100);
    var y = Math.floor(Math.random() * 200 + 100);
    document.dispatchEvent(new MouseEvent('mousemove', { clientX: x, clientY: y, bubbles: true }));
  } catch(e) {}
})();
true;
        """.trimIndent()
    }

    /**
     * Smart human simulation: natural reading scroll + mouse wander + hover awareness.
     * Does NOT click — pure awareness/exploration so bot-detectors see human rhythm.
     */
    fun buildAwareHumanSimulationScript(): String {
        return """
(function() {
  try {
    if (window.__cpa_aware_sim_running) return 'already';
    window.__cpa_aware_sim_running = true;
    var steps = 2 + Math.floor(Math.random() * 3);
    var i = 0;
    // 1. Mouse wander: 3-5 natural moves
    var mouseMoves = 0;
    var mouseTimer = setInterval(function() {
      try {
        var x = Math.floor(Math.random() * (window.innerWidth - 100)) + 50;
        var y = Math.floor(Math.random() * (window.innerHeight - 100)) + 50;
        ['mousemove','mouseover'].forEach(function(t) {
          document.dispatchEvent(new MouseEvent(t, { clientX: x, clientY: y, bubbles: true, view: window }));
        });
      } catch(e) {}
      if (++mouseMoves >= 4) clearInterval(mouseTimer);
    }, 700 + Math.floor(Math.random() * 600));
    // 2. Reading scroll: gradual down with pauses, occasional look-back
    function doScroll() {
      if (i >= steps) { window.__cpa_aware_sim_running = false; return; }
      var amt = 120 + Math.floor(Math.random() * 220);
      try { window.scrollBy({ top: amt, behavior: 'smooth' }); } catch(e) { window.scrollBy(0, amt); }
      i++;
      var pause = 900 + Math.floor(Math.random() * 1600);
      setTimeout(function() {
        if (Math.random() < 0.25) { try { window.scrollBy({ top: -60, behavior: 'smooth' }); } catch(e){} }
        setTimeout(doScroll, 500);
      }, pause);
    }
    setTimeout(doScroll, 600);
    // 3. Hover primary CTA without clicking (awareness signal)
    setTimeout(function() {
      try {
        var cta = document.querySelector('button:not([disabled]), a.btn, [class*="cta"]');
        if (cta && cta.offsetParent !== null) {
          var r = cta.getBoundingClientRect();
          cta.dispatchEvent(new MouseEvent('mouseover', { clientX: r.left + r.width/2, clientY: r.top + 10, bubbles: true }));
        }
      } catch(e) {}
    }, 1500);
    return 'started';
  } catch(e) { return 'err:' + e.message; }
})();
true;
        """.trimIndent()
    }

    /**
     * Human typing: types value char-by-char with random 30-90ms delays,
     * firing proper input events so React/Angular trackers accept it.
     * @param selector CSS selector of the field, @param value text to type
     */
    fun buildHumanTypingScript(selector: String, value: String): String {
        val safeSel = selector.replace("'", "\\'").replace("\n", "")
        val safeVal = org.json.JSONObject.quote(value)
        return """
(function() {
  try {
    var el = document.querySelector('$safeSel');
    if (!el) return 'not_found';
    var text = $safeVal;
    el.focus();
    el.value = '';
    el.dispatchEvent(new Event('focus', { bubbles: true }));
    var idx = 0;
    // Occasional typo + correction for ultra-human signal (5% chance)
    var makeTypo = Math.random() < 0.05 && text.length > 6;
    var typoAt = makeTypo ? (2 + Math.floor(Math.random() * (text.length - 4))) : -1;
    function typeNext() {
      if (idx >= text.length) {
        el.dispatchEvent(new Event('input', { bubbles: true }));
        el.dispatchEvent(new Event('change', { bubbles: true }));
        el.dispatchEvent(new Event('blur', { bubbles: true }));
        return 'done';
      }
      var ch = text[idx];
      if (idx === typoAt) {
        // type wrong char then backspace
        var wrong = (ch === 'a') ? 's' : 'a';
        el.value += wrong;
        el.dispatchEvent(new Event('input', { bubbles: true }));
        setTimeout(function() {
          el.value = el.value.slice(0, -1);
          el.dispatchEvent(new Event('input', { bubbles: true }));
          el.value += ch;
          el.dispatchEvent(new Event('input', { bubbles: true }));
          idx++;
          setTimeout(typeNext, 60 + Math.floor(Math.random() * 70));
        }, 120 + Math.floor(Math.random() * 150));
        return;
      }
      // Native setter for framework compat
      try {
        var proto = el.tagName === 'SELECT' ? window.HTMLSelectElement.prototype : window.HTMLInputElement.prototype;
        var setter = Object.getOwnPropertyDescriptor(proto, 'value')?.set;
        var cur = el.value + ch;
        if (setter) setter.call(el, cur); else el.value = cur;
      } catch(e) { el.value += ch; }
      el.dispatchEvent(new Event('input', { bubbles: true }));
      idx++;
      setTimeout(typeNext, 30 + Math.floor(Math.random() * 60));
    }
    typeNext();
    return 'typing';
  } catch(e) { return 'err:' + e.message; }
})();
true;
        """.trimIndent()
    }

    fun buildCompletionDetectorScript(keywords: List<String>): String {
        val kwArray = keywords.joinToString(",") { "'${it.trim().lowercase().replace("'", "\\'")}'" }
        return """
(function() {
  var keywords = [$kwArray];
  var completionReported = false;

  function checkCompletion() {
    if (completionReported) return;
    var bodyText = (document.body ? document.body.innerText : '').toLowerCase();
    var title = document.title.toLowerCase();

    for (var i = 0; i < keywords.length; i++) {
      var kw = keywords[i];
      if (kw && (bodyText.includes(kw) || title.includes(kw))) {
        completionReported = true;
        console.log('[CPA] Completion keyword detected: ' + kw);
        if (window.AndroidBridge && window.AndroidBridge.onTaskCompleted) {
          window.AndroidBridge.onTaskCompleted(kw, window.location.href);
        }
        break;
      }
    }
  }

  setTimeout(checkCompletion, 1500);
  window.addEventListener('load', function() { setTimeout(checkCompletion, 1000); });
  var obs = new MutationObserver(function() { setTimeout(checkCompletion, 500); });
  if (document.body) {
    obs.observe(document.body, { childList: true, subtree: true, characterData: true });
  }
})();
true;
        """.trimIndent()
    }

    fun buildOfferClickScript(clickTexts: List<String>, activeTargetText: String? = null): String {
        val clickTextsArray = JSONArray().apply {
            clickTexts.filter { it.isNotBlank() }.forEach { put(it) }
        }.toString()
        val activeTargetJson = if (activeTargetText.isNullOrBlank()) "null" else JSONObject.quote(activeTargetText)

        return """
(function() {
  var targetList = [];
  var active = $activeTargetJson;
  var allTexts = $clickTextsArray;
  if (active && active.trim().length > 0) targetList.push(active.trim().toLowerCase());
  for (var i = 0; i < allTexts.length; i++) {
    var s = (allTexts[i] || '').trim().toLowerCase();
    if (s && targetList.indexOf(s) === -1) targetList.push(s);
  }
  if (targetList.length === 0) targetList = ['walmart', 'gift card', '$1000', 'claim reward'];

  function clickTarget() {
    var candidates = Array.from(document.querySelectorAll('a, button, [role=button], h1, h2, h3, h4, h5, p, span, div, strong, b'));
    for (var i = 0; i < candidates.length; i++) {
      var el = candidates[i];
      if (el.offsetParent === null) continue;
      var txt = ((el.innerText || el.textContent || '') + ' ' + (el.getAttribute('aria-label') || '')).toLowerCase().trim();
      if (!txt || txt.length < 2) continue;

      for (var k = 0; k < targetList.length; k++) {
        var kw = targetList[k];
        if (txt.includes(kw)) {
          console.log('[CPA] Offer link found and clicked: ' + kw);
          var clickable = el.closest('a, button, [role=button]') || el;
          var href = clickable.href || (clickable.getAttribute ? clickable.getAttribute('href') : null);
          if (clickable.tagName === 'A' && clickable.target === '_blank') clickable.target = '_self';
          if (window.AndroidBridge && window.AndroidBridge.onOfferClicked) {
            window.AndroidBridge.onOfferClicked(kw, href || window.location.href);
          }
          clickable.click();
          if (href && href.startsWith('http') && href !== window.location.href) {
            setTimeout(function() { window.location.href = href; }, 600);
          }
          return true;
        }
      }
    }
    return false;
  }

  setTimeout(clickTarget, 800);
  window.addEventListener('load', function() { setTimeout(clickTarget, 600); });
})();
true;
        """.trimIndent()
    }

    /**
     * Autonomous Deep DOM Analyzer Script.
     * Evaluates the active page structure, identifies the funnel stage / category,
     * and sends a comprehensive analysis report back to AndroidBridge.onPageAnalyzed().
     */
    fun buildPageAnalyzerScript(clickTexts: List<String> = emptyList(), activeClickText: String? = null): String {
        val clickTextsArray = JSONArray().apply {
            clickTexts.filter { it.isNotBlank() }.forEach { put(it) }
        }.toString()
        val activeTargetJson = if (activeClickText.isNullOrBlank()) "null" else JSONObject.quote(activeClickText)

        return """
(function() {
  try {
    var url = window.location.href;
    var title = document.title || '';
    var bodyText = (document.body ? document.body.innerText : '').toLowerCase();

    // 1. Scan Inputs & Forms
    var allInputs = Array.from(document.querySelectorAll('input:not([type=hidden])'));
    var emailFields = 0;
    var textFields = 0;
    var passwordFields = 0;
    var checkboxes = Array.from(document.querySelectorAll('input[type=checkbox]'));
    var radios = Array.from(document.querySelectorAll('input[type=radio]'));
    var selects = Array.from(document.querySelectorAll('select'));
    var buttons = Array.from(document.querySelectorAll('button, [role=button], input[type=submit], input[type=button]'));

    allInputs.forEach(function(i) {
      var t = (i.type || 'text').toLowerCase();
      var meta = ((i.name || '') + ' ' + (i.id || '') + ' ' + (i.placeholder || '') + ' ' + (i.getAttribute('aria-label') || '')).toLowerCase();
      if (t === 'email' || meta.match(/email|e-mail/)) {
        emailFields++;
      } else if (t === 'password') {
        passwordFields++;
      } else if (t === 'text' || t === 'tel' || t === 'number') {
        textFields++;
      }
    });

    // 2. Scan for Skip / Upsell Buttons ("No thanks", "Skip", "Not interested")
    var hasSkipButtons = false;
    buttons.forEach(function(b) {
      var txt = ((b.innerText || b.textContent || '') + ' ' + (b.value || '')).toLowerCase().trim();
      if (txt.match(/no thanks|no, thanks|skip|not interested|pass|continue without/)) {
        hasSkipButtons = true;
      }
    });

    // 3. Scan for Priority #1 Offer Click Candidates
    var hasOfferCandidate = false;
    var targets = [];
    var activeTarget = $activeTargetJson;
    var configuredTexts = $clickTextsArray;
    if (activeTarget && activeTarget.trim().length > 0) targets.push(activeTarget.toLowerCase().trim());
    for (var k = 0; k < configuredTexts.length; k++) {
      if (configuredTexts[k] && configuredTexts[k].trim().length > 0) {
        targets.push(configuredTexts[k].toLowerCase().trim());
      }
    }

    if (targets.length > 0) {
      var clickableElements = Array.from(document.querySelectorAll('a, button, [role=button], h1, h2, h3, h4, [class*="offer"], [class*="cta"], [class*="btn"]'));
      for (var c = 0; c < clickableElements.length; c++) {
        var elTxt = ((clickableElements[c].innerText || clickableElements[c].textContent || '')).toLowerCase().trim();
        if (elTxt.length > 2) {
          for (var tg = 0; tg < targets.length; tg++) {
            if (elTxt.indexOf(targets[tg]) !== -1) {
              hasOfferCandidate = true;
              break;
            }
          }
        }
        if (hasOfferCandidate) break;
      }
    }

    // 4. Scan for Confirmation / Thank-You Indicators (expanded smart list)
    var isConfirmation = false;
    var matchedConfirm = '';
    var confirmKeywords = ['thank you', 'thanks', 'congratulations', 'congrats', 'order received', 'claim confirmed', 'reward credited', 'entry received', 'entry confirmed', 'survey completed', 'successfully registered', 'success', 'verified', 'welcome', 'account created', 'ticket number', 'responses recorded', 'claim reward', 'reward claimed'];
    for (var kw = 0; kw < confirmKeywords.length; kw++) {
      if (bodyText.indexOf(confirmKeywords[kw]) !== -1 || title.toLowerCase().indexOf(confirmKeywords[kw]) !== -1) {
        isConfirmation = true;
        matchedConfirm = confirmKeywords[kw];
        break;
      }
    }
    // URL-based confirmation signals (thank-you / success / complete pages)
    if (!isConfirmation) {
      var urlLow = url.toLowerCase();
      if (urlLow.match(/(thank|success|complete|confirm|congrat|welcome|done|finish)/)) { isConfirmation = true; matchedConfirm = 'url-signal'; }
    }

    // 5. Scan for CPA Content Lockers
    var hasLocker = false;
    var lockerScripts = document.querySelectorAll('script[src*="cpa"], script[src*="cpagrip"], script[src*="alignmentfiles"], script[src*="locker"], script[src*="ogads"]');
    var lockerIframes = document.querySelectorAll('iframe[src*="cpa"], iframe[src*="cpagrip"], iframe[src*="alignmentfiles"], iframe[src*="locker"], iframe[class*="locker"], iframe[id*="locker"]');
    var lockerContainers = document.querySelectorAll('#cpa_locker, .cpa_locker, [id*="locker"], [class*="locker-offers"], [class*="offer-list"], [class*="cpa-offers"]');
    if (lockerScripts.length > 0 || lockerIframes.length > 0 || lockerContainers.length > 0 || bodyText.includes('complete an offer below') || bodyText.includes('locked content') || bodyText.includes('complete a survey below') || url.includes('alignmentfiles') || url.includes('cpagrip')) {
      hasLocker = true;
    }

    // 5b. SMART: Captcha / Block / Loading awareness (incl. Shadow DOM + iframes)
    var hasCaptcha = false; var captchaKeyword = '';
    var isBlocked = false; var blockKeyword = '';
    var isLoadingPage = false; var loadingKeyword = '';
    try {
      var captchaEls = document.querySelectorAll('iframe[src*="recaptcha"], iframe[src*="hcaptcha"], iframe[src*="captcha"], iframe[src*="turnstile"], div[class*="g-recaptcha"], div[class*="h-captcha"], div[id*="captcha"], [data-sitekey]');
      if (captchaEls.length > 0) { hasCaptcha = true; captchaKeyword = 'captcha-element'; }
      var captchaPats = ['captcha', 'recaptcha', 'hcaptcha', 'turnstile', "i'm not a robot", 'select all images', 'verify you are human'];
      for (var ci = 0; ci < captchaPats.length; ci++) { if (bodyText.indexOf(captchaPats[ci]) !== -1) { hasCaptcha = true; captchaKeyword = captchaPats[ci]; break; } }
      var blockPats = ['access denied', 'forbidden', 'your ip has been blocked', 'suspicious activity', 'unusual traffic', 'attention required', 'error 1020', 'ip banned', 'blocked'];
      for (var bi = 0; bi < blockPats.length; bi++) { if (bodyText.indexOf(blockPats[bi]) !== -1 || title.toLowerCase().indexOf(blockPats[bi]) !== -1) { isBlocked = true; blockKeyword = blockPats[bi]; break; } }
      var loadPats = ['just a moment', 'verifying your browser', 'checking your browser', 'please wait', 'redirecting', 'loading'];
      for (var li = 0; li < loadPats.length; li++) { if (bodyText.indexOf(loadPats[li]) !== -1 && bodyText.length < 800) { isLoadingPage = true; loadingKeyword = loadPats[li]; break; } }
      // Shadow DOM host count — pages hiding forms inside shadow roots
      var shadowHosts = 0;
      try { var allEls = document.querySelectorAll('*'); for (var si = 0; si < Math.min(allEls.length, 400); si++) { if (allEls[si].shadowRoot) shadowHosts++; } } catch(se) {}
      var iframeCount = document.querySelectorAll('iframe').length;
      var visibleBtnCount = buttons.filter(function(b){ try { return b.offsetParent !== null; } catch(e){ return true; } }).length;
    } catch(smartErr) { var shadowHosts = 0; var iframeCount = 0; var visibleBtnCount = buttons.length; }

    // 6. Intelligent Category & Stage Classification
    var category = 'general';
    var confidence = 70;
    var summary = 'Standard Web Page';
    var nextAction = 'Observe & wait';

    if (isConfirmation) {
      category = 'completion_confirm';
      confidence = 98;
      summary = 'Confirmation / Thank You Stage Detected! Reward claimed.';
      nextAction = 'Record lead conversion and proceed to next offer';
    } else if (hasLocker) {
      category = 'content_locker';
      confidence = 97;
      summary = 'Content / Link Locker Detected (Waiting for offers to auto-click)';
      nextAction = 'Wait for locker offers, auto-click priority matching offer, and switch to new tab';
    } else if (hasOfferCandidate && (url.includes('blogspot') || bodyText.includes('offer') || buttons.length <= 4)) {
      category = 'offer_click';
      confidence = 95;
      summary = 'Landing Bridge with Priority #1 Offer Target Detected!';
      nextAction = 'Execute Offer Click to bridge into destination offer funnel';
    } else if (hasSkipButtons) {
      category = 'skip_upsells';
      confidence = 92;
      summary = 'Co-Reg / Sponsor Upsell Wall Detected';
      nextAction = 'Auto-click No Thanks / Skip to bypass payment requirement';
    } else if (radios.length >= 2 || selects.length >= 1 || document.querySelectorAll('.survey-btn, .quiz-option, [data-choice]').length >= 2) {
      category = 'survey_quiz';
      confidence = 90;
      summary = 'Interactive Survey / Quiz Questionnaire Detected (' + radios.length + ' radios, ' + selects.length + ' dropdowns)';
      nextAction = 'Answer questions consistently matching persona demographics';
    } else if (emailFields >= 1 && textFields <= 2) {
      category = 'email_submit';
      confidence = 94;
      summary = 'High-Value Email Opt-In / SOI Lead Form Detected';
      nextAction = 'Fill verified persona email and submit';
    } else if (textFields >= 3) {
      category = 'lead_gen';
      confidence = 88;
      summary = 'Full Contact Info / Shipping Form Detected (' + textFields + ' fields)';
      nextAction = 'Fill complete contact persona details (Name, Address, Phone, Zip)';
    } else if (checkboxes.length >= 1 && checkboxes.some(function(cb) { return !cb.checked; })) {
      category = 'terms_agreement';
      confidence = 85;
      summary = 'Consent & 18+ Age Checkboxes Pending Agreement';
      nextAction = 'Check all mandatory terms and consent checkboxes';
    } else {
      category = 'sweepstakes';
      confidence = 75;
      summary = 'Offer Funnel Interactive Page';
      nextAction = 'Auto-detect active controls and advance';
    }

    // 6b. SMART confidence adjustment: penalize weak/ambiguous signals, boost strong ones
    try {
      if (typeof shadowHosts !== 'undefined' && shadowHosts > 0 && confidence > 55) confidence -= 10;
      if (typeof iframeCount !== 'undefined' && iframeCount >= 4 && category !== 'content_locker') confidence -= 5;
      if (allInputs.length === 0 && buttons.length === 0) confidence = Math.min(confidence, 45);
      if (hasCaptcha || isBlocked) confidence = Math.max(confidence, 88);
      if (isConfirmation && matchedConfirm) confidence = Math.min(99, confidence + 3);
    } catch(adjErr) {}

    var report = {
      url: url,
      title: title,
      detectedCategory: category,
      confidence: confidence,
      summary: summary,
      fieldsCount: allInputs.length,
      emailFields: emailFields,
      textFields: textFields,
      passwordFields: passwordFields,
      checkboxesCount: checkboxes.length,
      radioGroupsCount: Math.ceil(radios.length / 2),
      selectCount: selects.length,
      buttonsCount: buttons.length,
      hasSkipButtons: hasSkipButtons,
      hasLocker: hasLocker,
      hasOfferClickCandidate: hasOfferCandidate,
      isConfirmationPage: isConfirmation,
      recommendedNextAction: nextAction,
      hasCaptcha: (typeof hasCaptcha !== 'undefined') ? hasCaptcha : false,
      captchaKeyword: (typeof captchaKeyword !== 'undefined') ? captchaKeyword : '',
      isBlocked: (typeof isBlocked !== 'undefined') ? isBlocked : false,
      blockKeyword: (typeof blockKeyword !== 'undefined') ? blockKeyword : '',
      isLoading: (typeof isLoadingPage !== 'undefined') ? isLoadingPage : false,
      loadingKeyword: (typeof loadingKeyword !== 'undefined') ? loadingKeyword : '',
      confirmKeyword: (typeof matchedConfirm !== 'undefined') ? matchedConfirm : '',
      shadowHosts: (typeof shadowHosts !== 'undefined') ? shadowHosts : 0,
      iframeCount: (typeof iframeCount !== 'undefined') ? iframeCount : 0,
      visibleButtons: (typeof visibleBtnCount !== 'undefined') ? visibleBtnCount : buttons.length
    };

    var reportStr = JSON.stringify(report);
    if (window.AndroidBridge && typeof window.AndroidBridge.onPageAnalyzed === 'function') {
      window.AndroidBridge.onPageAnalyzed(reportStr);
    }
    return reportStr;
  } catch(err) {
    return JSON.stringify({ error: err.message });
  }
})();
        """.trimIndent()
    }

    /**
     * Autonomous Template Generator: Scans active page DOM and constructs a complete,
     * reusable WorkTemplateEntity JSON model matching the exact buttons, forms, and questions on the page.
     */
    fun buildAutoTemplateGeneratorScript(): String {
        return """
(function() {
  try {
    var title = document.title || 'Landing Page';
    var url = window.location.href;
    var steps = [];
    var stepIndex = 1;

    // 1. Initial human scroll
    steps.push({
      id: 'step_' + (stepIndex++),
      type: 'SCROLL',
      targetType: 'auto',
      scrollDirection: 'down',
      scrollAmount: 320,
      delayMs: 800,
      description: 'تمرير طبيعي للمتصفح لتحميل واستكشاف عناصر الصفحة'
    });

    // 2. Scan for Priority Offer Click or Banner buttons
    var offerCandidates = Array.from(document.querySelectorAll('a[class*="offer"], a[class*="btn"], button[class*="cta"], [role=button]'));
    if (offerCandidates.length > 0 && offerCandidates.length <= 4) {
      var firstOffer = offerCandidates[0];
      var txt = (firstOffer.innerText || firstOffer.textContent || 'Click Offer').trim();
      steps.push({
        id: 'step_' + (stepIndex++),
        type: 'CLICK_BUTTON',
        targetType: 'text',
        targetValue: txt.slice(0, 30),
        delayMs: 1200,
        description: 'نقر رابط العرض الرئيسي: ' + txt.slice(0, 25)
      });
    }

    // 3. Scan inputs for Form Fields
    var inputs = Array.from(document.querySelectorAll('input:not([type=hidden]):not([type=submit]):not([type=button]):not([type=checkbox]):not([type=radio]), select, textarea'));
    inputs.forEach(function(inp) {
      var name = ((inp.name || '') + ' ' + (inp.id || '') + ' ' + (inp.placeholder || '')).toLowerCase();
      var type = (inp.type || 'text').toLowerCase();
      var fieldKey = 'custom';
      var fieldNameAr = 'حقل نصي';

      if (type === 'email' || name.match(/email|e-mail/)) {
        fieldKey = 'email';
        fieldNameAr = 'البريد الإلكتروني من شاشة المعلومات';
      } else if (name.match(/first.?name|fname/)) {
        fieldKey = 'firstName';
        fieldNameAr = 'الاسم الأول';
      } else if (name.match(/last.?name|lname/)) {
        fieldKey = 'lastName';
        fieldNameAr = 'اسم العائلة';
      } else if (name.match(/full.?name|name/)) {
        fieldKey = 'fullName';
        fieldNameAr = 'الاسم الكامل';
      } else if (type === 'tel' || name.match(/phone|tel|mobile/)) {
        fieldKey = 'phone';
        fieldNameAr = 'رقم الهاتف المولد';
      } else if (name.match(/zip|postal|postcode/)) {
        fieldKey = 'postalCode';
        fieldNameAr = 'الرمز البريدي';
      } else if (name.match(/address|street/)) {
        fieldKey = 'address';
        fieldNameAr = 'العنوان والشارع';
      } else if (name.match(/city/)) {
        fieldKey = 'city';
        fieldNameAr = 'المدينة';
      } else if (name.match(/state/)) {
        fieldKey = 'state';
        fieldNameAr = 'الولاية / المقاطعة';
      }

      steps.push({
        id: 'step_' + (stepIndex++),
        type: 'FILL_FIELD',
        targetType: 'auto',
        fieldSource: 'identity',
        fieldKey: fieldKey,
        targetValue: inp.id || inp.name || '',
        delayMs: 800,
        description: 'ملء ' + fieldNameAr + ' في النموذج'
      });
    });

    // 4. Scan for Questions / Radios
    var radios = document.querySelectorAll('input[type=radio]');
    if (radios.length >= 2) {
      steps.push({
        id: 'step_' + (stepIndex++),
        type: 'EXTRACT_ADAPT',
        targetType: 'auto',
        delayMs: 1000,
        description: 'استخراج ذكي لأسئلة الاستبيان واختيار الإجابات المؤهلة للتحويل'
      });
    }

    // 5. Scan Checkboxes (Terms, 18+ age)
    var checkboxes = document.querySelectorAll('input[type=checkbox]');
    if (checkboxes.length > 0) {
      steps.push({
        id: 'step_' + (stepIndex++),
        type: 'CHECK_BOX',
        targetType: 'auto',
        targetValue: 'terms',
        delayMs: 600,
        description: 'تحديد مربعات الموافقة على الشروط وتأكيد السن'
      });
    }

    // 6. Scan for Skip / Upsell bypass buttons
    var allButtons = Array.from(document.querySelectorAll('button, a, [role=button], input[type=submit]'));
    var hasSkip = allButtons.some(function(b) {
      var t = (b.innerText || b.textContent || b.value || '').toLowerCase();
      return t.match(/no thanks|skip|not interested/);
    });
    if (hasSkip) {
      steps.push({
        id: 'step_' + (stepIndex++),
        type: 'CLICK_BUTTON',
        targetType: 'text',
        targetValue: 'No Thanks, Skip',
        delayMs: 900,
        description: 'تخطي العروض الدعائية والرعاة تلقائياً'
      });
    }

    // 7. Action / Submit button
    steps.push({
      id: 'step_' + (stepIndex++),
      type: 'CLICK_BUTTON',
      targetType: 'auto',
      targetValue: 'Continue, Submit, Next, Claim',
      delayMs: 1200,
      description: 'نقر زر المتابعة أو إرسال الاستمارة'
    });

    var template = {
      name: 'قالب تلقائي: ' + (title.length > 25 ? title.slice(0, 25) + '...' : title),
      description: 'تم إنشاؤه تلقائياً بالذكاء الاصطناعي من تحليل صفحة: ' + url.slice(0, 40),
      targetCategory: inputs.length > 2 ? 'Lead Gen' : (inputs.length === 1 ? 'Email Submit' : 'Survey / Quiz'),
      isAutoGenerated: true,
      steps: steps
    };

    var templateJson = JSON.stringify(template);
    if (window.AndroidBridge && typeof window.AndroidBridge.onTemplateGenerated === 'function') {
      window.AndroidBridge.onTemplateGenerated(templateJson);
    }
    return templateJson;
  } catch(e) {
    return JSON.stringify({ error: e.message });
  }
})();
        """.trimIndent()
    }

    /**
     * Executes a user-defined or auto-generated Work Template on the active page DOM.
     */
    fun buildWorkTemplateExecutionScript(stepsJson: String, identity: GeneratedIdentity): String {
        val safeSteps = stepsJson.ifBlank { "[]" }
        val identityJson = JSONObject().apply {
            put("firstName", identity.firstName)
            put("lastName", identity.lastName)
            put("fullName", identity.fullName)
            put("email", identity.email)
            put("phone", identity.phone)
            put("address", identity.address)
            put("city", identity.city)
            put("state", identity.state)
            put("postalCode", identity.postalCode)
            put("country", identity.country)
            put("birthDate", identity.birthDate)
            put("gender", identity.gender)
            put("cardNumber", identity.cardNumber)
            put("cardExpiry", identity.cardExpiry)
            put("cardCvv", identity.cardCvv)
        }.toString()

        return """
(function() {
  try {
    var steps = $safeSteps;
    var ident = $identityJson;
    if (!Array.isArray(steps) || steps.length === 0) return 'No steps';

    function setVal(el, val) {
      if (!el || val === undefined || val === null) return;
      try {
        var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value')?.set;
        if (setter) setter.call(el, val); else el.value = val;
        el.dispatchEvent(new Event('input', { bubbles: true }));
        el.dispatchEvent(new Event('change', { bubbles: true }));
        el.dispatchEvent(new Event('blur', { bubbles: true }));
      } catch(e) {
        el.value = val;
      }
    }

    function triggerElClick(el) {
      if (!el) return;
      el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      var opts = { bubbles: true, cancelable: true, view: window };
      el.dispatchEvent(new MouseEvent('mouseover', opts));
      el.dispatchEvent(new MouseEvent('mousedown', opts));
      el.focus();
      el.dispatchEvent(new MouseEvent('mouseup', opts));
      el.dispatchEvent(new MouseEvent('click', opts));
      if (typeof el.click === 'function') el.click();
    }

    var currentStep = 0;
    function executeNextStep() {
      if (currentStep >= steps.length) {
        console.log('[Template Engine] All template steps executed successfully.');
        return;
      }
      var s = steps[currentStep];
      var idx = currentStep;
      currentStep++;

      try {
        if (s.type === 'SCROLL') {
          var amt = s.scrollAmount || 300;
          if (s.scrollDirection === 'up') amt = -amt;
          window.scrollBy({ top: amt, behavior: 'smooth' });
        } else if (s.type === 'FILL_FIELD') {
          var val = ident[s.fieldKey] || s.customValue || ident.email;
          var inps = Array.from(document.querySelectorAll('input:not([type=hidden]):not([type=submit]):not([type=button]):not([type=checkbox]):not([type=radio]), select, textarea'));
          var targetInp = inps.find(function(i) {
            var n = ((i.name || '') + ' ' + (i.id || '') + ' ' + (i.placeholder || '')).toLowerCase();
            return n.includes(s.fieldKey.toLowerCase());
          }) || inps[0];
          if (targetInp) setVal(targetInp, val);
        } else if (s.type === 'CHECK_BOX') {
          var cbs = Array.from(document.querySelectorAll('input[type=checkbox]'));
          cbs.forEach(function(cb) {
            if (!cb.checked) {
              cb.checked = true;
              cb.dispatchEvent(new Event('change', { bubbles: true }));
              cb.dispatchEvent(new Event('click', { bubbles: true }));
            }
          });
        } else if (s.type === 'CLICK_BUTTON') {
          var btns = Array.from(document.querySelectorAll('button, a, [role=button], input[type=submit]'));
          var matchKw = (s.targetValue || 'continue, submit, next, claim').toLowerCase().split(',').map(function(k) { return k.trim(); });
          var btnToClick = btns.find(function(b) {
            var txt = ((b.innerText || b.textContent || b.value || '')).toLowerCase();
            return matchKw.some(function(kw) { return txt.includes(kw); });
          }) || btns[0];
          if (btnToClick) triggerElClick(btnToClick);
        } else if (s.type === 'EXTRACT_ADAPT') {
          var radios = Array.from(document.querySelectorAll('input[type=radio]'));
          if (radios.length > 0 && !radios.some(function(r) { return r.checked; })) {
            radios[0].checked = true;
            radios[0].dispatchEvent(new Event('change', { bubbles: true }));
          }
        }

        if (window.AndroidBridge && typeof window.AndroidBridge.onPageActionStepExecuted === 'function') {
          window.AndroidBridge.onPageActionStepExecuted(idx, s.description || s.type, 'done');
        }
      } catch(err) {
        console.error('[Template Engine] Step error: ' + err.message);
      }

      setTimeout(executeNextStep, s.delayMs || 1000);
    }

    executeNextStep();
    return 'Started';
  } catch(e) {
    return 'Error: ' + e.message;
  }
})();
        """.trimIndent()
    }

    /**
     * Extracts Locker URL and ID from arbitrary user input (full script tag, direct URL, or numeric ID).
     */
    fun extractLockerUrlAndId(input: String): Pair<String, String> {
        val clean = input.trim()
        val tagRegex = Regex("""src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val tagMatch = tagRegex.find(clean)
        val extractedUrl = tagMatch?.groupValues?.get(1) ?: if (clean.startsWith("http://") || clean.startsWith("https://")) clean else ""

        val idRegex = Regex("""id=([0-9a-zA-Z_-]+)""", RegexOption.IGNORE_CASE)
        val idFromUrl = if (extractedUrl.isNotBlank()) idRegex.find(extractedUrl)?.groupValues?.get(1) else null
        val finalId = idFromUrl ?: if (clean.isNotBlank() && clean.all { it.isDigit() || it == '-' || it == '_' }) clean else "1741238"

        val finalUrl = if (extractedUrl.isNotBlank()) extractedUrl else "https://alignmentfiles.com/script_include.php?id=$finalId"
        return Pair(finalUrl, finalId)
    }

    /**
     * Advanced CPA Content Locker Detector, Polyfiller & Force-Trigger Engine.
     * Works with CPAGrip, AlignmentFiles, FileTrkr, and custom offer lock scripts.
     *
     * Features:
     * - Polyfills document.write to unblock dynamically written scripts in modern WebView
     * - Injects jQuery safely if missing from the host website
     * - Detects existing <script> tags or embedded script_include.php references
     * - Dispatches call_locker(), DisplayInlineBox(), load_slidepage()
     * - Fixes iframe & modal container visibility, zIndex and dimensions
     * - Injects custom script URL or ID if not present on the webpage
     */
    fun buildCpaLockerDetectorAndActivatorScript(
        customUrl: String? = null,
        customId: String? = null,
        forceTrigger: Boolean = true,
        forceInjectIfMissing: Boolean = false
    ): String {
        val safeCustomUrl = (customUrl ?: "").trim().replace("'", "\\'")
        val safeCustomId = (customId ?: "1741238").trim().replace("'", "\\'")

        return """
(function() {
  try {
    // 1. Safe detection and activation without hijacking document.write or overriding jQuery
    var detectedUrl = '';
    var detectedId = '';
    var scriptTags = Array.from(document.querySelectorAll('script[src]'));
    for (var i = 0; i < scriptTags.length; i++) {
      var src = scriptTags[i].src || '';
      if (src.indexOf('script_include.php') !== -1 || src.indexOf('alignmentfiles') !== -1 || src.indexOf('cpagrip') !== -1 || src.indexOf('filetrkr') !== -1 || src.indexOf('ogads') !== -1 || src.indexOf('cpabuild') !== -1) {
        detectedUrl = src;
        var m = src.match(/id=([0-9a-zA-Z_-]+)/i);
        if (m && m[1]) detectedId = m[1];
        break;
      }
    }

    // Also scan innerHTML in case script tag is in raw markup or template
    if (!detectedUrl && document.documentElement) {
      var html = document.documentElement.innerHTML;
      var rawMatch = html.match(/(https?:\/\/[^\s"'<>]*(?:alignmentfiles\.com|cpagrip\.com|filetrkr\.com|ogads\.com)[^\s"'<>]*script_include\.php\?id=([0-9a-zA-Z_-]+))/i);
      if (rawMatch) {
        detectedUrl = rawMatch[1];
        detectedId = rawMatch[2];
      }
    }

    // Report detection to Android
    if (detectedUrl && window.AndroidBridge && typeof window.AndroidBridge.onLockerDetected === 'function') {
      window.AndroidBridge.onLockerDetected(detectedUrl, detectedId || '1741238', false);
    }

    var targetUrl = detectedUrl;
    var targetId = detectedId;

    if (!targetUrl && $forceInjectIfMissing) {
      targetId = '$safeCustomId'.trim() || '1741238';
      targetUrl = '$safeCustomUrl'.trim() || ('https://alignmentfiles.com/script_include.php?id=' + targetId);
    }

    if (targetUrl) {
      var existingTag = document.querySelector('script[src*="' + (targetId || 'script_include') + '"]');
      if (!existingTag) {
        var tag = document.createElement('script');
        tag.type = 'text/javascript';
        tag.src = targetUrl;
        tag.async = false;
        (document.head || document.documentElement || document.body).appendChild(tag);
        if (window.AndroidBridge && typeof window.AndroidBridge.onLockerStatus === 'function') {
          window.AndroidBridge.onLockerStatus('تم حقن سكريبت اللوكر: ' + targetId);
        }
      }
    }

    if ($forceTrigger) {
      var triggerAttempts = 0;
      var triggerTimer = setInterval(function() {
        triggerAttempts++;
        var triggered = false;

        // Call genuine locker trigger routines ONLY if registered by the locker script
        if (typeof window.call_locker === 'function') {
          try { window.call_locker(); triggered = true; } catch(e) {}
        } else if (typeof window.DisplayInlineBox === 'function') {
          try { window.DisplayInlineBox(); triggered = true; } catch(e) {}
        } else if (typeof window.call1 === 'function') {
          try { window.call1(); triggered = true; } catch(e) {}
        } else if (typeof window.call2 === 'function') {
          try { window.call2(); triggered = true; } catch(e) {}
        } else if (typeof window.load_slidepage === 'function') {
          try { window.load_slidepage(); triggered = true; } catch(e) {}
        } else if (typeof window.og_load === 'function') {
          try { window.og_load(); triggered = true; } catch(e) {}
        } else if (typeof window.CPABuildLock === 'function') {
          try { window.CPABuildLock(); triggered = true; } catch(e) {}
        } else if (typeof window.show_locker === 'function') {
          try { window.show_locker(); triggered = true; } catch(e) {}
        } else if (typeof window.open_locker === 'function') {
          try { window.open_locker(); triggered = true; } catch(e) {}
        } else if (typeof window.load_locker === 'function') {
          try { window.load_locker(); triggered = true; } catch(e) {}
        } else if (typeof window.start_locker === 'function') {
          try { window.start_locker(); triggered = true; } catch(e) {}
        } else if (typeof window.cpa_complete === 'function') {
          try { window.cpa_complete(); triggered = true; } catch(e) {}
        }

        // Check if locker elements are present
        var lockerEl = document.querySelector('#InlineBoxMainOuterLayer, #InlineBoxDiv, #the_box, div[id*="InlineBox"], iframe[src*="alignmentfiles"], iframe[src*="load_box"], iframe[src*="cpagrip"], iframe[src*="ogads"], iframe[src*="cpabuild"], iframe[src*="locker"], div[class*="cpa_locker"], div[id*="cpa_locker"], div[class*="locker-offers"], div[id*="locker-offers"]');
        if (lockerEl) {
          triggered = true;
          window.__cpa_locker_detected = true;
        }

        if (!triggered && triggerAttempts < 20) return;
        clearInterval(triggerTimer);

        // Unhide & bring to front any genuine locker overlay / iframe elements
        var lockerSelectors = [
          '#InlineBoxMainOuterLayer',
          '#InlineBoxDiv',
          '#the_box',
          'div[id*="InlineBox"]',
          'iframe[src*="alignmentfiles"]',
          'iframe[src*="load_box"]',
          'iframe[src*="cpagrip"]',
          'iframe[src*="ogads"]',
          'iframe[src*="cpabuild"]',
          'iframe[src*="locker"]',
          'div[class*="cpa_locker"]',
          'div[id*="cpa_locker"]',
          'div[class*="locker-offers"]',
          'div[id*="locker-offers"]'
        ];
        lockerSelectors.forEach(function(sel) {
          document.querySelectorAll(sel).forEach(function(el) {
            el.style.display = 'block';
            el.style.visibility = 'visible';
            el.style.opacity = '1';
            el.style.zIndex = '2147483647';
          });
        });

        if (window.AndroidBridge && (detectedUrl || targetUrl || lockerEl)) {
          if (typeof window.AndroidBridge.onLockerDetected === 'function') {
            window.AndroidBridge.onLockerDetected(detectedUrl || targetUrl || 'detected_locker', detectedId || targetId || '1741238', triggered);
          }
          if (typeof window.AndroidBridge.onLockerStatus === 'function') {
            window.AndroidBridge.onLockerStatus(triggered ? 'تم تشغيل وتفعيل اللوكر بنجاح!' : 'تم فك حظر سكريبت اللوكر وتجهيزه');
          }
        }
      }, 400);
    }

    return 'Locker Engine Ready';
  } catch(e) {
    return 'Locker Engine Error: ' + e.message;
  }
})();
true;
        """.trimIndent()
    }

    /**
     * Autonomous Content Locker Offer Scanner, Poller and Auto-Clicker.
     * Handles CPA Grip, AlignmentFiles, OGAds, AdWorkMedia, and custom locker overlays / iframes.
     * When categories 'offer click' and 'locker' are combined, this script:
     * 1. Continuously waits and watches for the Locker to appear (DOM + iframes + MutationObserver).
     * 2. Matches available offers against keywords from the Offer Click screen.
     * 3. Programmatically clicks the matching offer.
     * 4. Dispatches the destination URL to AndroidBridge to seamlessly open and transition work to a new tab.
     */
    fun buildLockerOfferAutoClickScript(
        clickTexts: List<String> = emptyList(),
        activeTargetText: String? = null,
        selectionStrategy: String = "priority", // "priority", "first", "random"
        openInNewTab: Boolean = true
    ): String {
        val clickTextsArray = org.json.JSONArray().apply {
            clickTexts.filter { it.isNotBlank() }.forEach { put(it) }
        }.toString()
        val activeTargetJson = if (activeTargetText.isNullOrBlank()) "null" else org.json.JSONObject.quote(activeTargetText)
        val strategyJson = org.json.JSONObject.quote(selectionStrategy)

        return """
(function() {
  try {
    if (window.__cpa_locker_offer_clicked) return 'already_clicked';

    var targetList = [];
    var active = $activeTargetJson;
    var allTexts = $clickTextsArray;
    var strategy = $strategyJson;
    var openNewTab = $openInNewTab;

    if (active && active.trim().length > 0) targetList.push(active.trim().toLowerCase());
    for (var i = 0; i < allTexts.length; i++) {
      var s = (allTexts[i] || '').trim().toLowerCase();
      if (s && targetList.indexOf(s) === -1) targetList.push(s);
    }
    if (targetList.length === 0) targetList = ['get a $100 nike gift card', 'walmart', 'cash app', 'gift card', '$1000', '$750', '$500', 'claim', 'survey', 'reward', 'free', 'win', 'card', 'nike'];

    function scanDocForOffers(doc, isIframe, list) {
      if (!doc) return;
      var selectors = [
        '#InlineBoxDiv a',
        '#the_box a',
        '#main_div a',
        '.offer_link',
        'a[href*="tracking"]',
        'a[href*="load_box"]',
        'a[href*="show_file"]',
        'a[href*="click.php"]',
        'a[href*="alignmentfiles"]',
        'a[href*="cpagrip"]',
        'a[href*="ogads"]',
        'a[href*="filetrkr"]',
        '[class*="offer-item"] a',
        '[class*="offer_item"] a',
        'div[class*="offer"] a',
        'li[id*="offer"] a',
        'tr[class*="offer"] a',
        'table[class*="offer"] a',
        '[data-offer-id] a',
        '[data-offer-id]',
        'a[class*="offer"]',
        'a[id*="offer"]',
        'button[class*="offer"]',
        'button[onclick*="offer"]'
      ];

      try {
        var found = doc.querySelectorAll(selectors.join(','));
        for (var k = 0; k < found.length; k++) {
          var el = found[k];
          var txt = ((el.innerText || el.textContent || '') + ' ' + (el.getAttribute('title') || '') + ' ' + (el.getAttribute('aria-label') || '')).trim();
          var href = el.href || el.getAttribute('href') || el.getAttribute('data-url') || '';
          if (!href && el.getAttribute('onclick')) {
            var m = el.getAttribute('onclick').match(/https?:\/\/[^\s'"]+/);
            if (m) href = m[0];
          }
          if (txt.length >= 2 || href.length >= 4) {
            list.push({ element: el, text: txt, href: href, isIframe: isIframe });
          }
        }
      } catch(e) {}
    }

    function collectAllCandidates() {
      var candidates = [];
      scanDocForOffers(document, false, candidates);

      var iframes = document.querySelectorAll('iframe[src*="load_box"], iframe[src*="alignmentfiles"], iframe[src*="cpagrip"], iframe[src*="locker"], iframe#the_box, iframe#InlineBoxFrame, iframe[src*="track"]');
      for (var f = 0; f < iframes.length; f++) {
        try {
          var iframeDoc = iframes[f].contentDocument || (iframes[f].contentWindow ? iframes[f].contentWindow.document : null);
          if (iframeDoc) {
            scanDocForOffers(iframeDoc, true, candidates);
          }
        } catch(frameErr) {
          var frameSrc = iframes[f].src || '';
          if (frameSrc && (frameSrc.includes('load_box') || frameSrc.includes('alignmentfiles') || frameSrc.includes('cpagrip')) && !candidates.length) {
            candidates.push({ element: iframes[f], text: 'Locker Frame Offer', href: frameSrc, isIframe: true });
          }
        }
      }

      // If no explicit locker links yet, scan anchor elements that contain keywords from the targetList
      // ONLY on non-locker pages! On a locker page, we must wait for the real locker overlay/iframe.
      var isLockerExpected = !!document.querySelector(
        'script[src*="script_include"], script[src*="alignmentfiles"], script[src*="cpagrip"], script[src*="filetrkr"], script[src*="ogads"], script[src*="cpabuild"], iframe[src*="load_box"], #InlineBoxMainOuterLayer, #the_box, div[class*="cpa_locker"], div[id*="cpa_locker"]'
      );

      if (candidates.length === 0 && !isLockerExpected) {
        var generalLinks = document.querySelectorAll('a, button, [role=button], h1, h2, h3, h4, p, span, div[class*="btn"], div[class*="cta"]');
        for (var g = 0; g < generalLinks.length; g++) {
          var gel = generalLinks[g];
          if (gel.offsetParent === null && gel.offsetWidth === 0 && gel.offsetHeight === 0) continue;
          var gtxt = (gel.innerText || gel.textContent || '').trim().toLowerCase();
          if (gtxt.length < 3) continue;
          for (var t = 0; t < targetList.length; t++) {
            if (gtxt.includes(targetList[t])) {
              var clickable = gel.closest('a, button, [role=button]') || gel;
              var gh = clickable.href || (clickable.getAttribute ? clickable.getAttribute('href') : null) || '';
              candidates.push({ element: clickable, text: gtxt, href: gh, isIframe: false });
              break;
            }
          }
        }
      }
      return candidates;
    }

    function normalizeOfferText(s) {
      if (!s) return '';
      return ('' + s).toLowerCase().replace(/[^a-z0-9$]+/g, ' ').replace(/\s+/g, ' ').trim();
    }

    function fuzzyOfferMatch(candidateRaw, targetRaw) {
      if (!candidateRaw || !targetRaw) return false;
      var cand = ('' + candidateRaw).toLowerCase();
      var targ = ('' + targetRaw).toLowerCase();
      if (cand.indexOf(targ) !== -1) return true;
      var cleanCand = normalizeOfferText(cand);
      var cleanTarg = normalizeOfferText(targ);
      if (cleanCand.indexOf(cleanTarg) !== -1) return true;
      // Token match: all meaningful tokens (>=3 chars or $/digits) must appear.
      // Handles "Get a $100 Nike Gift Card!" vs "Get $100 Nike Gift Card" vs line-breaks.
      var tokens = cleanTarg.split(' ').filter(function(w) { return w.length >= 3 || /\d/.test(w); });
      // Drop generic filler tokens so "get/a" don't dilute Nike matching
      var strong = tokens.filter(function(w) { return ['get','and','the','for','you','your','with','card','gift','nike','100','reward','claim','free','win'].indexOf(w) !== -1 ? true : w.length >= 4; });
      if (strong.length >= 2) {
        var hits = 0;
        for (var i = 0; i < strong.length; i++) { if (cleanCand.indexOf(strong[i]) !== -1) hits++; }
        // Require at least 2 hits AND brand token (nike/walmart/amazon/cash/target/apple) when present in target
        var brandTokens = ['nike','walmart','amazon','cash','target','apple','app','gift'];
        var brandInTarget = null;
        for (var b = 0; b < brandTokens.length; b++) { if (cleanTarg.indexOf(brandTokens[b]) !== -1) { brandInTarget = brandTokens[b]; break; } }
        if (brandInTarget && cleanCand.indexOf(brandInTarget) === -1) return false;
        if (hits >= Math.min(strong.length, 3)) return true;
        if (hits >= 2 && cleanCand.indexOf('gift') !== -1) return true;
      } else if (tokens.length >= 1) {
        var allIn = true;
        for (var j = 0; j < tokens.length; j++) { if (cleanCand.indexOf(tokens[j]) === -1) { allIn = false; break; } }
        if (allIn) return true;
      }
      return false;
    }

    function resolveOfferUrl(rawHref, el) {
      try {
        var h = (rawHref || '').trim();
        if (!h && el) {
          var anchor = (el.closest) ? el.closest('a') : null;
          if (anchor) h = anchor.href || anchor.getAttribute('href') || anchor.getAttribute('data-url') || '';
          if (!h && el.getAttribute) h = el.getAttribute('data-url') || el.getAttribute('data-href') || '';
          if (!h && el.getAttribute && el.getAttribute('onclick')) {
            var m = el.getAttribute('onclick').match(/https?:\/\/[^\s'"]+/);
            if (m) h = m[0];
          }
        }
        if (!h) return '';
        h = h.trim();
        if (h.indexOf('javascript:') === 0 || h === '#' || h === '') return '';
        // Resolve relative URLs against page base
        if (h.indexOf('http') !== 0) {
          try { h = new URL(h, document.baseURI || window.location.href).toString(); } catch(e) { return ''; }
        }
        if (h.indexOf('http') !== 0) return '';
        // Never treat the locker loader itself as the offer destination
        if (h.indexOf('script_include.php') !== -1 || h.indexOf('load_box.php') !== -1) return '';
        if (h === window.location.href) return '';
        return h;
      } catch(e) { return ''; }
    }

    function dismissLandingOverlays() {
      try {
        // Blogger / GDPR / cookie notices that cover the locker trigger area
        var sels = [
          '#cookieChoiceDismiss', '.cookie-choices-button', '[aria-label*="cookie" i]',
          '.cookie-notice button', '#cookie-notice button', '.consent button',
          'button[id*="cookie" i]', 'button[class*="cookie" i]', 'a[class*="cookie" i]'
        ];
        var texts = ['got it', 'ok', 'accept', 'agree', 'allow', 'understand', 'dismiss', 'close'];
        for (var s = 0; s < sels.length; s++) {
          var els = document.querySelectorAll(sels[s]);
          for (var k = 0; k < els.length; k++) {
            try {
              var t = ((els[k].innerText || els[k].textContent || '') + '').toLowerCase().trim();
              if (t.length < 40 && (t === '' || texts.some(function(w){ return t.indexOf(w) !== -1; }))) {
                if (els[k].offsetParent !== null) els[k].click();
              }
            } catch(e) {}
          }
        }
      } catch(e) {}
    }

    function executeClick(chosen) {
      if (!chosen || window.__cpa_locker_offer_clicked) return false;
      dismissLandingOverlays();

      var offerTitle = (chosen.text || 'Locker Offer').trim();
      var offerUrl = resolveOfferUrl(chosen.href, chosen.element);

      // Mark clicked only after we resolved a usable destination OR we will rely on popup relay.
      // This prevents double-tab storms while still guaranteeing a new-tab transition.
      window.__cpa_locker_offer_clicked = true;

      if (window.__cpa_locker_poll_interval) {
        clearInterval(window.__cpa_locker_poll_interval);
        window.__cpa_locker_poll_interval = null;
      }
      if (window.__cpa_locker_mutation_obs) {
        try { window.__cpa_locker_mutation_obs.disconnect(); } catch(e) {}
        window.__cpa_locker_mutation_obs = null;
      }

      console.log('[LockerAutoClicker] Successfully selected offer: ' + offerTitle + ' -> ' + offerUrl);

      var shortTitle = offerTitle.length > 30 ? offerTitle.substring(0, 30) : offerTitle;
      if (window.AndroidBridge && typeof window.AndroidBridge.onLockerStatus === 'function') {
        try { window.AndroidBridge.onLockerStatus('تم النقر على عرض: ' + shortTitle + ' ➔ جاري الانتقال لتبويب جديد...'); } catch(e) {}
      }

      // 1. Real user-like click first: lets window.open popups flow through onCreateWindow -> isolated tab.
      try {
        if (chosen.element.scrollIntoView) {
          chosen.element.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
      } catch(e) {}
      try {
        var mEvt = new MouseEvent('click', { bubbles: true, cancelable: true, view: window });
        chosen.element.dispatchEvent(mEvt);
        if (chosen.element.click) chosen.element.click();
      } catch(clickErr) {
        try { if (chosen.element.click) chosen.element.click(); } catch(e) {}
      }

      // 2. Guaranteed new-tab transition via native bridge (uses Info-screen identity context downstream).
      // Only with a valid http destination; otherwise the popup relay already opened the tab.
      if (offerUrl && offerUrl.indexOf('http') === 0) {
        if (openNewTab && window.AndroidBridge && typeof window.AndroidBridge.onOfferClickedInNewTab === 'function') {
          try { window.AndroidBridge.onOfferClickedInNewTab(offerTitle, offerUrl); } catch(e) {}
        } else if (window.AndroidBridge && typeof window.AndroidBridge.onOfferClicked === 'function') {
          try { window.AndroidBridge.onOfferClicked(offerTitle, offerUrl); } catch(e) {}
        }
      } else {
        // No direct URL (cross-origin iframe offer): notify status so native side keeps the popup tab
        // and starts human simulation + Info-identity form fill there.
        if (window.AndroidBridge && typeof window.AndroidBridge.onLockerStatus === 'function') {
          try { window.AndroidBridge.onLockerStatus('تم النقر داخل إطار اللوكر — بانتظار تبويب العرض المنبثق لبدء التعامل بهوية شاشة المعلومات...'); } catch(e) {}
        }
        if (window.AndroidBridge && typeof window.AndroidBridge.onMidPageInfoExtracted === 'function') {
          try { window.AndroidBridge.onMidPageInfoExtracted('locker_offer_clicked_no_direct_url', offerTitle.substring(0, 60)); } catch(e) {}
        }
      }

      if (!openNewTab && offerUrl && offerUrl.indexOf('http') === 0 && offerUrl !== window.location.href) {
        setTimeout(function() {
          window.location.href = offerUrl;
        }, 500);
      }
      return true;
    }

    function reportOffersForLearning(cands, matched) {
      try {
        var names = cands.slice(0, 8).map(function(o) { return (o.text || '(no-text)').slice(0, 40); }).join(' | ');
        if (window.AndroidBridge && typeof window.AndroidBridge.onMidPageInfoExtracted === 'function') {
          window.AndroidBridge.onMidPageInfoExtracted(matched ? 'locker_offers_matched' : 'locker_offers_available', names);
        }
        if (window.AndroidBridge && typeof window.AndroidBridge.onLockerStatus === 'function' && !matched) {
          window.AndroidBridge.onLockerStatus('لا تطابق حرفي — المتاح: ' + names.slice(0, 90) + ' — سيتم أول عرض كبديل ذكي');
        }
      } catch(e) {}
    }

    function scanAndExecuteOfferSelection() {
      if (window.__cpa_locker_offer_clicked) return true;
      dismissLandingOverlays();
      var candidates = collectAllCandidates();
      if (candidates.length === 0) return false;

      // Select offer based on strategy and user text targets (fuzzy Nike-safe matching)
      var chosen = null;
      if (strategy === 'priority') {
        for (var c = 0; c < candidates.length; c++) {
          var candText = candidates[c].text || '';
          for (var p = 0; p < targetList.length; p++) {
            if (fuzzyOfferMatch(candText, targetList[p])) {
              chosen = candidates[c];
              break;
            }
          }
          if (chosen) break;
        }
        if (!chosen) { chosen = candidates[0]; reportOffersForLearning(candidates, false); }
        else { reportOffersForLearning(candidates, true); }
      } else if (strategy === 'random') {
        var randIdx = Math.floor(Math.random() * candidates.length);
        chosen = candidates[randIdx];
      } else {
        chosen = candidates[0];
      }

      if (!chosen) chosen = candidates[0];
      return executeClick(chosen);
    }

    // 1. Initial immediate scan
    var immediateSuccess = scanAndExecuteOfferSelection();
    if (immediateSuccess) {
      return 'clicked_immediately';
    }

    // 2. Asynchronous Waiting & Continuous Scanner (Locker might appear after delay)
    if (!window.__cpa_locker_scanner_active) {
      window.__cpa_locker_scanner_active = true;
      if (window.AndroidBridge && typeof window.AndroidBridge.onLockerStatus === 'function') {
        window.AndroidBridge.onLockerStatus('انتظار ظهور لوكر العروض وتنشيطه...');
      }

      var attempts = 0;
      var maxAttempts = 75; // ~30 seconds of persistent watching
      window.__cpa_locker_poll_interval = setInterval(function() {
        attempts++;
        if (window.__cpa_locker_offer_clicked || attempts > maxAttempts) {
          clearInterval(window.__cpa_locker_poll_interval);
          window.__cpa_locker_poll_interval = null;
          window.__cpa_locker_scanner_active = false;
          return;
        }

        // Also check if locker call is pending and trigger if needed
        if (typeof window.call_locker === 'function' && attempts % 3 === 0) {
          try { window.call_locker(); } catch(e) {}
        }

        scanAndExecuteOfferSelection();
      }, 400);

      try {
        var obs = new MutationObserver(function() {
          if (window.__cpa_locker_offer_clicked) {
            obs.disconnect();
            return;
          }
          scanAndExecuteOfferSelection();
        });
        obs.observe(document.documentElement || document.body, { childList: true, subtree: true, attributes: true });
        window.__cpa_locker_mutation_obs = obs;
      } catch(obsErr) {}
    }

    return 'waiting_for_locker_offers';
  } catch(e) {
    return 'error:' + e.message;
  }
})();
true;
        """.trimIndent()
    }

    /**
     * Simulates natural human behavior on a newly opened offer tab:
     * - Progressive smooth scrolling
     * - Reading pauses
     * - Inspection of lead gen elements (email submit, survey question, postal code)
     * - Notifies Android when interaction session finishes
     */
    fun buildHumanInteractionOnOfferPageScript(
        tabId: String,
        stayDurationSec: Int = 15
    ): String {
        val safeTabId = tabId.replace("'", "\\'")
        val duration = stayDurationSec.coerceAtLeast(5)

        return """
(function() {
  try {
    if (window.__cpa_new_tab_interaction_started) return 'already_started';
    window.__cpa_new_tab_interaction_started = true;

    var tabId = '$safeTabId';
    var maxDurationMs = $duration * 1000;
    var startTime = Date.now();
    console.log('[NewTabHandler] Started natural human browsing on offer page: ' + window.location.href);

    var scrollTimer = setInterval(function() {
      var elapsed = Date.now() - startTime;
      if (elapsed >= maxDurationMs) {
        clearInterval(scrollTimer);
        finishInteraction();
        return;
      }

      var scrollAmount = Math.floor(Math.random() * 180) + 70;
      window.scrollBy({ top: scrollAmount, behavior: 'smooth' });

      if (Math.random() < 0.22) {
        setTimeout(function() {
          window.scrollBy({ top: -Math.floor(Math.random() * 90), behavior: 'smooth' });
        }, 400);
      }
    }, 1100);

    function finishInteraction() {
      var inputs = Array.from(document.querySelectorAll('input:not([type=hidden])'));
      var emailField = inputs.find(function(i) {
        var t = (i.type || '').toLowerCase();
        var m = (i.name || '') + ' ' + (i.id || '') + ' ' + (i.placeholder || '');
        return t === 'email' || m.toLowerCase().includes('email');
      });
      var zipField = inputs.find(function(i) {
        var m = (i.name || '') + ' ' + (i.id || '') + ' ' + (i.placeholder || '');
        return m.toLowerCase().includes('zip') || m.toLowerCase().includes('postal');
      });
      var radios = document.querySelectorAll('input[type=radio], input[type=checkbox]');

      var details = 'تصفح بشري مكتمل (${duration}s)';
      if (emailField) details += ' • رصد حقل إيميل';
      if (zipField) details += ' • رصد حقل الرمز البريدي';
      if (radios.length > 0) details += ' • خيارات استبيان (' + radios.length + ')';

      if (window.AndroidBridge && typeof window.AndroidBridge.onNewTabInteractionCompleted === 'function') {
        window.AndroidBridge.onNewTabInteractionCompleted(tabId, window.location.href, details);
      }
    }
  } catch(e) {
    console.warn('[NewTabHandler] Error: ' + e.message);
  }
})();
true;
        """.trimIndent()
    }
}