package com.guestbook

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    val server = GuestbookServer(port)
    server.start()
} 