data class GuestbookEntry(
    val name: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) 