package eu.pazuzu.fapi.submissions

import eu.pazuzu.fapi.FA
import eu.pazuzu.fapi.comments.Comment
import eu.pazuzu.fapi.util.textAfter
import eu.pazuzu.fapi.util.width
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * A submission.
 * @property user The posting user.
 * @property page The page this user was on.
 * @property id The ID of the submission.
 * @property title The submission title.
 * @property thumbnail The thumbnail of the submission.
 * @property width The width of the submission.
 * @property height The height of the submission.
 * @property fa The configuration used.
 */
data class Submission(
        val user: String,
        val page: Int,
        val id: String,
        val title: String,
        val thumbnail: String,
        val width: Double,
        val height: Double,
        val fa: FA) {
    /**
     * The full document of the submission.
     */
    private val document: Document by lazy {
        val url = "${fa.url}/view/$id"
        Jsoup.connect(url).get()
    }

    val full by lazy {
        document.selectFirst("a:containsOwn(Download)").attr("href")
    }

    val content by lazy {
        document.selectFirst("img.avatar").parent().parent()
    }

    val contentText get() = content.text()

    /**
     * The comments on the journal.
     */
    val comments: List<Comment> by lazy {
        // Make result list
        val result = arrayListOf<Comment>()

        // Select tables with comment ids
        val nodes = document.select("table[id^=cid]")

        for ((i, c) in nodes.withIndex()) {
            // Get the ID of the comment
            val id = c.id().substringAfter("cid:")

            // Replies are indented, select first before that is of a higher level
            val replyToId = nodes.subList(0, i)
                    .asReversed()
                    .firstOrNull { it.width == fa.commentWidthDecrement + c.width }
                    ?.id()?.substringAfter("cid:")

            // Userpage is marked as a link to the user page
            val user = c.selectFirst("a[href^=/user/]")
                    .attr("href")
                    .substringAfter("/user/")
                    .substringBefore("/")
            // Content is present in a specific div
            val content = c.selectFirst("div.message-text")

            // Add result to list
            result += Comment(id, replyToId, user, content, fa)
        }

        // Make list of mutable list
        result.toList()
    }

    private val statsEntries by lazy {
        document.selectFirst("td.stats-container").select("b").subList(1, 9)
    }

    val postedTime get() = statsEntries[0].nextElementSibling().attr("title")

    val postedDate get() = FA.parseTime(postedTime)

    val category get() = statsEntries[1].textAfter()

    val theme get() = statsEntries[2].textAfter()

    val species get() = statsEntries[3].textAfter()

    val gender get() = statsEntries[4].textAfter()

    val numFavorites get() = statsEntries[5].textAfter()?.trim()?.toInt() ?: 0

    val numComments get() = statsEntries[6].textAfter()?.trim()?.toInt() ?: 0

    val numViews get() = statsEntries[7].textAfter()?.trim()?.toInt() ?: 0
}

data class SubmissionPage(
        val user: String,
        val page: Int,
        val submissions: List<Submission>,
        val more: Boolean,
        val fa: FA)


/**
 * Gets a specific submission page.
 * @param user The user to retrieve the page for.
 * @param page The page number to retrieve, starting at 1.
 * @param fa The basic configuration for URLs, methods and constants and units.
 */
fun submissionPage(user: String, page: Int, fa: FA): SubmissionPage {
    // Get the URLS
    val pageSuffix = if (page == 1) "" else "/$page"
    val url = "${fa.url}/gallery/$user$pageSuffix"

    // Get the document
    val doc = Jsoup.connect(url).get()

    // Find all tables that have submission IDs
    val submissions = doc
            .select("figure[id^=sid]")
            .map {
                val id = it.id()
                        .substringAfter("sid-")
                val entries = it.select("a").subList(0, 2)
                val thumbnailImage = entries[0].selectFirst("img")
                val thumbnail = thumbnailImage.attr("src")
                val width = thumbnailImage.attr("data-width").toDouble()
                val height = thumbnailImage.attr("data-height").toDouble()
                val title = entries[1].text()

                Submission(user, page, id, title, thumbnail, width, height, fa)
            }

    // There is a next page if there is a button pointing to a next page
    val more = doc.select("div.fancy-pagination > a.right").any()

    // Return results
    return SubmissionPage(user, page, submissions, more, fa)
}

/**
 * Gets all submission pages until there are no more left.
 * @param user The user to find the pages for.
 * @param fa The basic configuration for URLs, methods and constants and units.
 * @return Returns the page number associated to the submission page.
 */
fun submissionPages(user: String, fa: FA) =
        generateSequence(1 to submissionPage(user, 1, fa)) { (p, j) ->
            if (j.more)
                (p + 1) to submissionPage(user, p + 1, fa)
            else
                null
        }

/**
 * Gets all submissions for the user.
 * @param user The user to get the submissions for.
 * @param fa The basic configuration for URLs, methods and constants and units.
 * @return Returns all submissions.
 */
fun submissions(user: String, fa: FA) =
        submissionPages(user, fa).flatMap { (_, j) -> j.submissions.asSequence() }