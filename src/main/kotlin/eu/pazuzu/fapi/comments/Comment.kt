package eu.pazuzu.fapi.comments

import eu.pazuzu.fapi.FA
import eu.pazuzu.fapi.util.Node
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
        val time: String,
        val content: Element,
        val fa: FA) {

    override fun toString() =
            "(Comment $id by $user : ${content.text()} (at $date))"

    /**
     * The post time of the comment.
     */
    val date get() = FA.parseTime(time)

    /**
     * The text of the comment.
     */
    val contentText get() = content.text()
}

/**
 * Converts the iterable of comments to a tree structure.
 */
fun Iterable<Comment>.toTree(): List<Node<Comment>> {
    fun mapFor(comment: Comment): Node<Comment> =
            Node(comment, filter { it.replyToId == comment.id }.map { mapFor(it) })
    return filter { it.replyToId == null }.map { mapFor(it) }
}

/**
 * Converts the sequence of comments to a tree structure.
 */
fun Sequence<Comment>.toTree(): List<Node<Comment>> {
    fun mapFor(comment: Comment): Node<Comment> =
            Node(comment, filter { it.replyToId == comment.id }.map { mapFor(it) }.toList())
    return filter { it.replyToId == null }.map { mapFor(it) }.toList()
}