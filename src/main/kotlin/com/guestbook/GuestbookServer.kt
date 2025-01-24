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
    private var redis: Jedis? = null
    private val maxRetries = 5
    private val retryDelayMs = 5000L

    init {
        val redisAddress = System.getenv("REDIS_HOST") ?: "localhost"
        redisPort = System.getenv("REDIS_PORT")?.toInt() ?: 6379

        redisHost = if (redisAddress.startsWith("tcp://")) {
            URI(redisAddress.replace("tcp://", "http://")).host
        } else {
            redisAddress
        }

        connectToRedis()
    }

    private fun connectToRedis() {
        var retries = 0
        while (retries < maxRetries) {
            try {
                redis = Jedis(redisHost, redisPort)
                redis?.ping()  // Test the connection
                println("Successfully connected to Redis at $redisHost:$redisPort")
                return
            } catch (e: Exception) {
                retries++
                println("Failed to connect to Redis (attempt $retries/$maxRetries): ${e.message}")
                if (retries < maxRetries) {
                    println("Retrying in ${retryDelayMs/1000} seconds...")
                    Thread.sleep(retryDelayMs)
                }
            }
        }
        throw RuntimeException("Failed to connect to Redis after $maxRetries attempts")
    }

    private val gson = Gson()

    init {
        server.createContext("/") { exchange ->
            when (exchange.requestURI.path) {
                "/" -> handleRoot(exchange)
                "/entries" -> handleEntries(exchange)
                else -> {
                    exchange.sendResponseHeaders(404, 0)
                    exchange.responseBody.close()
                }
            }
        }
        server.executor = null
    }

    private fun handleRoot(exchange: HttpExchange) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>mirrord Guestbook</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        max-width: 800px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #f5f5f5;
                        color: #333;
                    }
                    .entry {
                        background: white;
                        border: 1px solid #e0e0e0;
                        margin: 15px 0;
                        padding: 15px;
                        border-radius: 8px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.05);
                        transition: transform 0.2s ease;
                    }
                    .entry:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 4px 8px rgba(0,0,0,0.1);
                    }
                    .entry-form {
                        background: white;
                        padding: 20px;
                        border-radius: 8px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.05);
                        margin-bottom: 30px;
                    }
                    input, textarea {
                        width: 100%;
                        margin-bottom: 15px;
                        padding: 12px;
                        border: 1px solid #ddd;
                        border-radius: 6px;
                        box-sizing: border-box;
                        font-family: inherit;
                    }
                    textarea {
                        min-height: 100px;
                        resize: vertical;
                    }
                    button {
                        background-color: #4361ee;
                        color: white;
                        padding: 12px 24px;
                        border: none;
                        border-radius: 6px;
                        cursor: pointer;
                        font-size: 16px;
                        transition: background-color 0.2s ease;
                    }
                    button:hover {
                        background-color: #3046c5;
                    }
                    .header {
                        display: flex;
                        align-items: center;
                        gap: 15px;
                        margin-bottom: 30px;
                        padding-bottom: 20px;
                        border-bottom: 2px solid #e0e0e0;
                    }
                    .logo {
                        height: 40px;
                        width: auto;
                    }
                    h1 {
                        color: #2d3748;
                        margin: 0;
                    }
                    h2 {
                        color: #2d3748;
                        margin-bottom: 15px;
                    }
                    .entry strong {
                        color: #4361ee;
                        font-size: 1.1em;
                        display: block;
                        margin-bottom: 8px;
                    }
                    .entry p {
                        margin: 0;
                        line-height: 1.5;
                    }
                    @media (max-width: 600px) {
                        body {
                            padding: 15px;
                        }
                        .entry-form {
                            padding: 15px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>🪞 mirrord Guestbook</h1>
                </div>
                
                <div class="entry-form">
                    <h2>Add New Entry</h2>
                    <input type="text" id="name" placeholder="Your Name" required>
                    <textarea id="message" placeholder="Your Message" required></textarea>
                    <button onclick="addEntry()">Submit</button>
                </div>

                <h2>Entries</h2>
                <div id="entries"></div>

                <script>
                    function loadEntries() {
                        fetch('/entries')
                            .then(response => response.json())
                            .then(entries => {
                                const entriesDiv = document.getElementById('entries');
                                entriesDiv.innerHTML = '';
                                entries.forEach(entry => {
                                    const date = new Date(entry.timestamp);
                                    const entryDiv = document.createElement('div');
                                    entryDiv.className = 'entry';
                                    entryDiv.innerHTML = `<strong>${'$'}{entry.name}</strong><p>${'$'}{entry.message}</p>`;
                                    entriesDiv.appendChild(entryDiv);
                                });
                            })
                            .catch(error => console.error('Error:', error));
                    }

                    function addEntry() {
                        const name = document.getElementById('name').value;
                        const message = document.getElementById('message').value;
                        
                        if (!name || !message) {
                            alert('Please fill in all fields');
                            return;
                        }

                        fetch('/entries', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json',
                            },
                            body: JSON.stringify({
                                name: name,
                                message: message
                            })
                        })
                        .then(response => {
                            if (response.status === 201) {
                                document.getElementById('name').value = '';
                                document.getElementById('message').value = '';
                                loadEntries();
                            }
                        })
                        .catch(error => console.error('Error:', error));
                    }

                    // Load entries when page loads
                    loadEntries();
                    // Refresh entries every 10 seconds
                    setInterval(loadEntries, 10000);
                </script>
            </body>
            </html>
        """.trimIndent()
        
        exchange.responseHeaders.set("Content-Type", "text/html")
        exchange.sendResponseHeaders(200, html.length.toLong())
        val os = exchange.responseBody
        os.write(html.toByteArray())
        os.close()
    }

    private fun handleEntries(exchange: HttpExchange) {
        when (exchange.requestMethod) {
            "GET" -> handleGetEntries(exchange)
            "POST" -> handlePostEntry(exchange)
            else -> {
                exchange.sendResponseHeaders(405, 0)
                exchange.responseBody.close()
            }
        }
    }

    private fun handleGetEntries(exchange: HttpExchange) {
        try {
            val entries = redis?.lrange("entries", 0, -1) ?: emptyList()
            val response = gson.toJson(entries.map { gson.fromJson(it, GuestbookEntry::class.java) })
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.length.toLong())
            val os = exchange.responseBody
            os.write(response.toByteArray())
            os.close()
        } catch (e: Exception) {
            val errorResponse = """{"error": "Database connection error"}"""
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(503, errorResponse.length.toLong())
            val os = exchange.responseBody
            os.write(errorResponse.toByteArray())
            os.close()
        }
    }

    private fun handlePostEntry(exchange: HttpExchange) {
        try {
            val requestBody = String(exchange.requestBody.readBytes())
            val guestbookEntry = gson.fromJson(requestBody, GuestbookEntry::class.java)
            redis?.lpush("entries", gson.toJson(guestbookEntry))
                ?: throw Exception("Redis connection not available")
            
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(201, -1)
        } catch (e: Exception) {
            val errorResponse = """{"error": "Failed to save entry"}"""
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(503, errorResponse.length.toLong())
            val os = exchange.responseBody
            os.write(errorResponse.toByteArray())
            os.close()
            return
        } finally {
            exchange.responseBody.close()
        }
    }

    fun start() {
        server.start()
        println("Server started on port $port")
    }
} 