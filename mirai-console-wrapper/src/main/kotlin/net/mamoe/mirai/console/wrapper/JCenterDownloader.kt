/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */
@file:Suppress("EXPERIMENTAL_API_USAGE")

package net.mamoe.mirai.console.wrapper

import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.ProxyConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.http
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.*
import java.io.File
import kotlin.system.exitProcess

internal val proxyAvailabilityJob: Deferred<ProxyConfig?> = GlobalScope.async {
    kotlin.run {
        val url = WrapperCli.proxy
        if (url != "DEFAULT") {
            ProxyBuilder.http(url).also {
                println("Testing proxy $it...")
                require(it.testProxy()) {
                    "Proxy $url is invalid."
                }
                println("Proxy is valid.")
            }
        } else {
            null
            /*
            lateinit var threads: List<Thread>
            withTimeoutOrNull(5_000) {
                suspendCancellableCoroutine<ProxyConfig?> { cont ->
                    threads = listOf("http://127.0.0.1:1080", "http://127.0.0.1:1088").map {
                        thread(isDaemon = true) {
                            runBlocking {
                                val proxy = ProxyBuilder.http(it)
                                if (proxy.testProxy())
                                    cont.resume(proxy)
                            }
                        }
                    }
                    threads = threads + listOf("127.0.0.1" to 1080, "127.0.0.1" to 1088).map {
                        thread(isDaemon = true) {
                            runBlocking {
                                val proxy = ProxyBuilder.socks(it.first, it.second)
                                if (proxy.testProxy())
                                    cont.resume(proxy)
                            }
                        }
                    }
                }
            }.also { threads.forEach(Thread::interrupt) }
            */
        }
    }.also { println("Using proxy: ${it?.address()}") }
}

suspend fun ProxyConfig.testProxy(): Boolean = kotlin.runCatching {
    HttpClient(CIO) {
        engine {
            proxy = this@testProxy
        }
    }.use {
        it.get<HttpStatusCode>("https://www.baidu.com").isSuccess()
    }
}.getOrElse { false }

/**
 * 自动检测 127.0.0.1:1080 和 127.0.0.1:1088 (SS 默认)
 */
internal suspend fun availableProxy(): ProxyConfig? = proxyAvailabilityJob.await()

internal val Http: HttpClient = HttpClient(CIO) {
    engine {
        proxy = runBlocking { availableProxy() }
    }
}

internal inline fun <R> tryNTimesOrQuit(repeat: Int, errorHint: String, block: (Int) -> R) {
    var lastException: Throwable? = null
    repeat(repeat) {
        try {
            block(it)
            return
        } catch (e: Throwable) {
            if (lastException == null) {
                lastException = e
            } else lastException!!.addSuppressed(e)
        }
    }
    println(errorHint)
    lastException!!.printStackTrace()
    println(errorHint)
    exitProcess(1)
}

internal suspend inline fun HttpClient.downloadRequest(url: String): ByteReadChannel {
    return with(this.get<HttpResponse>(url)) {
        if (this.status.value == 404 || this.status.value == 403) {
            error("File not found")
        }
        if (this.headers["status"] != null && this.headers["status"] == "404") {
            error("File not found")
        }
        this.content
    }
}

private val jcenterPath = "https://jcenter.bintray.com/{group}/{project}/{version}/{project}-{version}.{extension}"
private val aliyunPath =
        "https://maven.aliyun.com/nexus/content/repositories/jcenter/{group}/{project}/{version}/{project}-{version}.{extension}"

private fun String.buildPath(
        groupName: String,
        projectName: String,
        version: String,
        extension: String
): String {
    return this
            .replace(
                    "{group}", groupName
            )
            .replace(
                    "{project}", projectName
            )
            .replace(
                    "{extension}", extension
            )
            .replace(
                    "{version}", version
            )
}

internal suspend fun HttpClient.downloadMaven(
        groupName: String,
        projectName: String,
        version: String,
        extension: String
): ByteReadChannel {
    return kotlin.runCatching {
        downloadRequest(
                aliyunPath.buildPath(groupName, projectName, version, extension)
        )
    }.getOrElse {
        downloadRequest(
                jcenterPath.buildPath(groupName, projectName, version, extension)
        )
    }
}

internal suspend inline fun HttpClient.downloadMavenArchive(
        groupName: String,
        projectName: String,
        version: String
): ByteReadChannel {
    return downloadMaven(groupName, projectName, version, "jar")
}

internal suspend inline fun HttpClient.downloadMavenPom(
        groupName: String,
        projectName: String,
        version: String
): ByteReadChannel {
    return downloadMaven(groupName, projectName, version, "pom")
}

internal suspend fun HttpClient.downloadMavenPomAsString(
        groupName: String,
        projectName: String,
        version: String
): String {
    return kotlin.runCatching {
        this.get<String>(
                aliyunPath.buildPath(groupName, projectName, version, "pom")
        )
    }.getOrElse {
        this.get(
                aliyunPath.buildPath(groupName, projectName, version, "pom")
        )
    }
}


/**
 * 只要填 content path 后面的就可以
 */
internal suspend fun ByteReadChannel.saveToContent(filepath: String) {
    val fileStream = File(contentPath.absolutePath + "/" + filepath).also {
        withContext(Dispatchers.IO) {
            it.createNewFile()
        }
    }.outputStream()

    withContext(Dispatchers.IO) {
        this@saveToContent.copyTo(fileStream)
        fileStream.flush()
    }
}


internal fun getContent(filepath: String): File {
    return File(contentPath, filepath)
}
