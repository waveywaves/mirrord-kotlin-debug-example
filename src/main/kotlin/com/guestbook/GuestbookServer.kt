package com.guestbook

import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpExchange
import java.net.InetSocketAddress
import java.io.*
import redis.clients.jedis.Jedis
import com.google.gson.Gson
import java.net.URI

class GuestbookServer(private val port: Int) {
    private val server: HttpServer = HttpServer.create(InetSocketAddress(port), 0)
    private val redisHost: String
    private val redisPort: Int
    private val redis: Jedis

    init {
        val redisAddress = System.getenv("REDIS_HOST") ?: "localhost"
        redisPort = System.getenv("REDIS_PORT")?.toInt() ?: 6379

        // Handle TCP URL format from Kubernetes
        redisHost = if (redisAddress.startsWith("tcp://")) {
            URI(redisAddress.replace("tcp://", "http://")).host
        } else {
            redisAddress
        }

        redis = Jedis(redisHost, redisPort)
    }

    private val gson = Gson()

    init {
        server.createContext("/entries") { exchange ->
            when (exchange.requestMethod) {
                "GET" -> handleGetEntries(exchange)
                "POST" -> handlePostEntry(exchange)
                else -> {
                    exchange.sendResponseHeaders(405, 0)
                    exchange.responseBody.close()
                }
            }
        }
        server.executor = null
    }

    private fun handleGetEntries(exchange: HttpExchange) {
        val entries = redis.lrange("entries", 0, -1)
        val response = gson.toJson(entries.map { gson.fromJson(it, GuestbookEntry::class.java) })
        exchange.responseHeaders.set("Content-Type", "application/json")
        exchange.sendResponseHeaders(200, response.length.toLong())
        val os = exchange.responseBody
        os.write(response.toByteArray())
        os.close()
    }

    private fun handlePostEntry(exchange: HttpExchange) {
        val input = String(exchange.requestBody.readBytes())
        val entry = gson.fromJson(input, GuestbookEntry::class.java)
        redis.lpush("entries", gson.toJson(entry))
        
        exchange.sendResponseHeaders(201, -1)
        exchange.responseBody.close()
    }

    fun start() {
        server.start()
        println("Server started on port $port")
        println("Connected to Redis at $redisHost:$redisPort")
    }
} 