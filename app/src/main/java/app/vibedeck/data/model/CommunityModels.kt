package app.vibedeck.data.model

data class RatingTotals(val sum: Long, val count: Long) {
    val average: Double get() = if (count == 0L) 0.0 else sum.toDouble() / count
    fun change(previous: Int?, next: Int?): RatingTotals {
        require(previous == null || previous in 1..5)
        require(next == null || next in 1..5)
        val updated = RatingTotals(sum - (previous ?: 0) + (next ?: 0), count + (if (next == null) 0 else 1) - (if (previous == null) 0 else 1))
        require(updated.count >= 0 && updated.sum >= updated.count && updated.sum <= updated.count * 5)
        return updated
    }
}
data class GameComment(val id: String, val uid: String, val authorName: String, val authorPhotoUrl: String, val text: String, val createdAtMillis: Long?)
data class CommentPage(val comments: List<GameComment>, val nextCursor: String?, val hasMore: Boolean)
data class GameDraft(val url: String, val title: String, val description: String, val imageUrl: String, val tags: List<String>, val orientation: String)
sealed interface SaveGameResult { data class Created(val id: String) : SaveGameResult; data class AlreadyExists(val id: String) : SaveGameResult }
data class CatalogStatus(val loading: Boolean = false, val error: String? = null, val warning: String? = null)
