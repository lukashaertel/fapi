package eu.pazuzu.fapi

import eu.pazuzu.fapi.comments.Comment
import eu.pazuzu.fapi.journals.Journal
import eu.pazuzu.fapi.submissions.Submission
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Quick equality of [T]s in the context of [R].
 */
typealias Eq<R, T> = R.(T, T) -> Boolean

/**
 * Equality applied to lists.
 * @receiver The context
 * @param l The left list
 * @param r The right list
 * @param eq The [Eq] to apply.
 */
inline fun <R, T> R.listEq(l: List<T>, r: List<T>, eq: Eq<R, T>) =
        l.size == r.size && (l zip r).all { (x, y) -> eq(this, x, y) }

/**
 * The configuration to all API calls.
 * @property url The base URL, without trailing slash.
 * @property delay The delay for daemons.
 * @property delayUnit The unit of [delay].
 * @property commentWidthDecrement The decrement of comment table width from comment to reply.
 * @property commentEq Quick equality on comments for delta operations.
 * @property journalEq Quick equality on journals for delta operations.
 */
data class FA(
        val url: String,
        val delay: Long,
        val delayUnit: TimeUnit,
        val commentWidthDecrement: Int,
        val commentEq: Eq<FA, Comment>,
        val journalEq: Eq<FA, Journal>,
        val submissionEq: Eq<FA, Submission>) {
    companion object {

        /**
         * The default configuration.
         */
        val default = FA(
                "https://www.furaffinity.net",
                10L,
                TimeUnit.MINUTES,
                3,
                { l, r ->
                    l.id == r.id
                            && l.user == r.user
                            && l.contentText == r.contentText
                },
                { l, r ->
                    l.id == r.id
                            && l.title == r.title
                            && l.postTime == r.postTime
                            && l.contentText == r.contentText
                            && l.comments.size == r.comments.size
                            && listEq(l.comments, r.comments, commentEq)
                },
                { l, r ->
                    l.id == r.id
                            && l.title == r.title
                            && l.postedTime == r.postedTime
                            && l.category == r.category
                            && l.theme == r.theme
                            && l.species == r.species
                            && l.gender == r.gender
                            && l.contentText == r.contentText
                            && listEq(l.comments, r.comments, commentEq)
                })

        /**
         * Date format used to parse, after preparing.
         */
        private val COMMENT_DATE_FORMAT = SimpleDateFormat("MMM dd, yyyy hh:mm aa")

        /**
         * Parses a date as given on the site.
         */
        fun parseTime(string: String): Date =
                string
                        .replace("st", "")
                        .replace("nd", "")
                        .replace("rd", "")
                        .replace("th", "")
                        .let { COMMENT_DATE_FORMAT.parse(it) }
    }
}