package app.vibedeck.data.model

data class Game(
    val id: String,
    val title: String,
    val url: String,
    val description: String = "เกมจากชุมชน VibeDeck ลองเล่นแล้วค้นพบไอเดียใหม่ไปด้วยกัน",
    val thumbnailUrl: String = "",
    val screenshots: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val aiTools: List<String> = emptyList(),
    val orientation: String = "auto",
    val usesAudio: Boolean = false,
    val developerName: String = "ผู้สร้างในชุมชน",
    val isVerified: Boolean = false,
    val rating: Double? = null,
    val ratingCount: Long = 0,
    val commentCount: Long = 0,
    val submittedBy: String = "",
    val status: String = "approved",
    val createdAtMillis: Long = 0,
)

