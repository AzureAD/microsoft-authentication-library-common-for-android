// Copyright (c) Microsoft Corporation. All rights reserved.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package com.microsoft.identity.learning

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

internal class CopilotProtocolTest {
    @Test(timeout = 90_000) fun installedCliReturnsJsonWithStdinContextAndZeroToolsWithoutUpstreamAccess() {
        val executable = System.getProperty("reviewLearning.copilotExecutable").orEmpty()
        assumeTrue("Set -PcopilotExecutable to run the real-CLI offline contract", executable.isNotBlank())
        val model = System.getProperty("reviewLearning.copilotModel", "gpt-6.1-sol")
        val requests = AtomicInteger()
        val upstream = AtomicInteger()
        val toolCount = AtomicInteger(-1)
        val toolNames = AtomicReference<List<String>>(emptyList())
        val markerCount = AtomicInteger()
        val provider = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val proxy = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val marker = "BENIGN_LOCAL_PROTOCOL_FIXTURE"
        val answer = """{"version":1,"rules":[]}"""
        provider.createContext("/") { exchange ->
            try {
                if (exchange.requestMethod != "POST" || !exchange.requestURI.path.endsWith("/chat/completions") ||
                    requests.incrementAndGet() > 1) {
                    exchange.sendResponseHeaders(400, -1)
                    return@createContext
                }
                val text = exchange.requestBody.bufferedReader(Charsets.UTF_8).use { reader -> reader.readText() }
                val request = Json.parse(text).obj()
                toolCount.set(request.get("tools")?.takeUnless { tools -> tools.isJsonNull }?.asJsonArray?.size() ?: 0)
                toolNames.set(request.get("tools")?.takeUnless { tools -> tools.isJsonNull }?.asJsonArray?.map { tool ->
                    val value = tool.obj()
                    (value.getAsJsonObject("function") ?: value.getAsJsonObject("custom") ?: value)
                        .get("name")?.asString ?: "unnamed-tool"
                } ?: emptyList())
                if (marker in text) markerCount.incrementAndGet()
                val chunks = listOf(
                    mapOf("role" to "assistant", "content" to answer) to null,
                    emptyMap<String, String>() to "stop"
                ).joinToString("") { (delta, finish) ->
                    "data: " + Json.encode(mapOf("id" to "chatcmpl-fixture", "object" to "chat.completion.chunk",
                        "created" to Instant.now().epochSecond, "model" to "local-fixture",
                        "choices" to listOf(mapOf("index" to 0, "delta" to delta, "finish_reason" to finish)))) + "\n\n"
                } + "data: [DONE]\n\n"
                exchange.responseHeaders.set("Content-Type", "text/event-stream")
                exchange.sendResponseHeaders(200, 0)
                exchange.responseBody.write(chunks.toByteArray(Charsets.UTF_8))
            } finally {
                exchange.close()
            }
        }
        proxy.createContext("/") { exchange ->
            try {
                upstream.incrementAndGet()
                exchange.sendResponseHeaders(502, -1)
            } finally {
                exchange.close()
            }
        }
        provider.start()
        proxy.start()
        try {
            val engine = CopilotEngine(executable, model, "30", "unused-local-fixture") { builder ->
                builder.environment().apply {
                    remove("GITHUB_TOKEN")
                    putAll(mapOf(
                        "COPILOT_OFFLINE" to "true",
                        "COPILOT_PROVIDER_TYPE" to "openai",
                        "COPILOT_PROVIDER_BASE_URL" to "http://127.0.0.1:${provider.address.port}/v1",
                        "COPILOT_PROVIDER_API_KEY" to "unused-local-fixture",
                        "COPILOT_PROVIDER_MODEL_ID" to model,
                        "COPILOT_PROVIDER_WIRE_MODEL" to "local-fixture",
                        "HTTP_PROXY" to "http://127.0.0.1:${proxy.address.port}",
                        "HTTPS_PROXY" to "http://127.0.0.1:${proxy.address.port}",
                        "NO_PROXY" to "127.0.0.1,localhost",
                        "COPILOT_SYSTEM_PROXY" to "0",
                        "COPILOT_OTEL_ENABLED" to "false"
                    ))
                }
                builder.start()
            }
            val result = runBlocking { engine.generate("$marker: Return only $answer") }
            assertEquals(Proposal(1, emptyList()), Codec.proposal(result))
            assertEquals(1, requests.get())
            assertEquals(1, markerCount.get())
            assertEquals("Pinned CLI exposed model tools: ${toolNames.get()}", 0, toolCount.get())
            assertEquals("Unexpected upstream connection attempt", 0, upstream.get())
        } finally {
            provider.stop(0)
            proxy.stop(0)
        }
    }
}
