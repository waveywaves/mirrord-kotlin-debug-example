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

        redisHost = if (redisAddress.startsWith("tcp://")) {
            URI(redisAddress.replace("tcp://", "http://")).host
        } else {
            redisAddress
        }

        redis = Jedis(redisHost, redisPort)
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
                <title>Guestbook</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        max-width: 800px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .entry {
                        border: 1px solid #ddd;
                        margin: 10px 0;
                        padding: 10px;
                        border-radius: 4px;
                    }
                    .entry-form {
                        margin-bottom: 20px;
                    }
                    input, textarea {
                        width: 100%;
                        margin-bottom: 10px;
                        padding: 8px;
                    }
                    button {
                        background-color: #4CAF50;
                        color: white;
                        padding: 10px 20px;
                        border: none;
                        border-radius: 4px;
                        cursor: pointer;
                    }
                    button:hover {
                        background-color: #45a049;
                    }
                </style>
            </head>
            <body>
                <h1>Guestbook</h1>
                
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
                                    entryDiv.innerHTML = "<strong>${'$'}{entry.name}</strong><p>${'$'}{entry.message}</p><small>${'$'}{date.toLocaleString()}</small>"
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
        val entries = redis.lrange("entries", 0, -1)
        val response = gson.toJson(entries.map { gson.fromJson(it, GuestbookEntry::class.java) })
        exchange.responseHeaders.set("Content-Type", "application/json")
        exchange.sendResponseHeaders(200, response.length.toLong())
        val os = exchange.responseBody
        os.write(response.toByteArray())
        os.close()
    }

    private fun handlePostEntry(exchange: HttpExchange) {
        try {
            val requestBody = String(exchange.requestBody.readBytes())
            val guestbookEntry = gson.fromJson(requestBody, GuestbookEntry::class.java)
            redis.lpush("entries", gson.toJson(guestbookEntry))
            
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(201, -1)
        } finally {
            exchange.responseBody.close()
        }
    }

    fun start() {
        server.start()
        println("Server started on port $port")
        println("Connected to Redis at $redisHost:$redisPort")
    }
} 