package eu.pazuzu.fapi.comments

import eu.pazuzu.fapi.FA
import org.jsoup.nodes.Element

/**
 * Comment on a journal or submission.
 * @property id The ID of the comment.
 * @property user The user replying to a journal.
 * @property content The content of the comment.
 * @property fa The basic configuration for URLs, methods and constants and units.
 */
data class Comment(
        val id: String,
        val replyToId: String?,
        val user: String,
        val content: Element,
        val fa: FA) {

    override fun toString() =
            "(Comment ${id}: by $user)"

    /**
     * The text of the comment.
     */
    val contentText get() = content.text()
}