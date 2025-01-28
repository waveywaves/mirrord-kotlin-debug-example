package com.guestbook

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 9090
    val server = GuestbookServer(port)
    server.start()
} 