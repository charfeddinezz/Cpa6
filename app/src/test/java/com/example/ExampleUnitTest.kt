package com.example

import com.example.service.IdentityService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testAsocksFetch() = runBlocking {
    val newUrl = "https://asocks-list.org/nNXGLqOxzG66RnkaXmZCZ7VoXTuuWUw8.txt?limit=10&type=res&template_id=2&country=US"
    println("Testing new Asocks URL: $newUrl")
    val res = IdentityService.fetchProxiesFromUrl(newUrl)
    println("Fetch result: isSuccess=${res.isSuccess}")
    if (res.isFailure) {
      println("Live external network unavailable or expired: ${res.exceptionOrNull()?.message}")
      return@runBlocking
    }
    val proxies = res.getOrNull() ?: return@runBlocking
    println("Parsed proxies count: ${proxies.size}")
    for (p in proxies) {
      println("Proxy parsed: ${p.host}:${p.port} [${p.type}] user='${p.username}' pass='${p.password}'")
    }

    var workingCount = 0
    var failedCount = 0
    for ((index, p) in proxies.withIndex()) {
      val diag = IdentityService.testAndDetectProxy(p.host, p.port, p.type, p.username, p.password, 12000)
      println("Proxy #$index [${p.host}:${p.port}]: working=${diag.isWorking}, exitIp=${diag.exitIp}, ping=${diag.pingMs}ms, error=${diag.errorMessage}")
      if (diag.isWorking) workingCount++ else failedCount++
    }
    println("Summary: working=$workingCount, failed=$failedCount out of ${proxies.size}")
  }
}
