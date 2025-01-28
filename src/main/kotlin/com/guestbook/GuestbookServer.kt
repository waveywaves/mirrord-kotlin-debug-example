package com.guestbook

import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpExchange
import java.net.InetSocketAddress
import redis.clients.jedis.Jedis
import com.google.gson.Gson
import java.net.URI

class GuestbookServer(private val port: Int) {
    private val gson = Gson()
    private var redis: Jedis? = null
    private lateinit var server: HttpServer
    
    private fun connectToRedis() {
        val redisHost = System.getenv("REDIS_HOST") ?: "localhost"
        val redisPort = System.getenv("REDIS_PORT")?.toInt() ?: 6379
        try {
            redis = Jedis(redisHost, redisPort)
            println("Connected to Redis at $redisHost:$redisPort")
        } catch (e: Exception) {
            println("Failed to connect to Redis: ${e.message}")
        }
    }

    fun start() {
        connectToRedis()
        setupServer()
        server.start()
        println("Server started on port $port")
    }

    private fun setupServer() {
        server = HttpServer.create(InetSocketAddress(port), 0)
        server.createContext("/") { exchange ->
            when (exchange.requestMethod) {
                "GET" -> handleGet(exchange)
                "POST" -> handlePost(exchange)
                else -> sendResponse(exchange, 405, "Method not allowed")
            }
        }
        server.executor = null
    }

    private fun handleGet(exchange: HttpExchange) {
        val entries = redis?.lrange("entries", 0, -1)?.map { gson.fromJson(it, GuestbookEntry::class.java) } ?: listOf()
        val entriesHtml = entries.joinToString("") { entry ->
            "<div class='entry'><strong>${entry.name}</strong>: ${entry.message}</div>"
        }
        
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>mirrord Guestbook</title>
                <style>
                    body { font-family: sans-serif; max-width: 600px; margin: 20px auto; padding: 0 20px; }
                    .entry { margin: 10px 0; padding: 10px; border: 1px solid #ddd; }
                    form { margin: 20px 0; }
                    input, textarea { display: block; margin: 10px 0; width: 100%; padding: 5px; }
                </style>
            </head>
            <body>
                <h1>mirrord Guestbook</h1>
                <form method="post">
                    <input type="text" name="name" placeholder="Your Name" required>
                    <textarea name="message" placeholder="Your Message" required></textarea>
                    <input type="submit" value="Submit">
                </form>
                <div id="entries">
                    $entriesHtml
                </div>
            </body>
            </html>
        """.trimIndent()
        
        sendResponse(exchange, 200, html, "text/html")
    }

    private fun handlePost(exchange: HttpExchange) {
        val formData = String(exchange.requestBody.readBytes())
        val params = formData.split("&").associate { 
            val (key, value) = it.split("=")
            java.net.URLDecoder.decode(key, "UTF-8") to java.net.URLDecoder.decode(value, "UTF-8")
        }
        
        val entry = GuestbookEntry(
            name = params["name"] ?: "",
            message = params["message"] ?: ""
        )
        
        redis?.lpush("entries", gson.toJson(entry))
        
        // Redirect back to the main page after submission
        exchange.responseHeaders.set("Location", "/")
        exchange.sendResponseHeaders(303, -1)
        exchange.responseBody.close()
    }

    private fun sendResponse(exchange: HttpExchange, code: Int, response: String, contentType: String = "text/plain") {
        exchange.responseHeaders.set("Content-Type", contentType)
        exchange.sendResponseHeaders(code, response.length.toLong())
        exchange.responseBody.use { os ->
            os.write(response.toByteArray())
        }
    }
} 