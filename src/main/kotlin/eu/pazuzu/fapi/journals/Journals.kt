package eu.pazuzu.fapi.journals


import eu.pazuzu.fapi.FA
import eu.pazuzu.fapi.comments.Comment
import eu.pazuzu.fapi.util.width
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.*

/**
 * A journal.
 * @property user The user who posted that journal.
 * @property page The page number this journal is on.
 * @property id The ID of the journal.
 * @property title The title of the journal.
 * @property postTime The time of the journal, as given on the site.
 * @property numComments The number of comments on the journal.
 * @property fa The basic configuration for URLs, methods and constants and units.
 */
data class Journal(
        val user: String,
        val page: Int,
        val id: String,
        val title: String,
        val postTime: String,
        val numComments: Int,
        val fa: FA) {
    override fun toString() =
            "(Journal $id: $title on $postTime ($numComments comments))"


    /**
     * The full document of the journal.
     */
    private val document: Document by lazy {
        val url = "${fa.url}/journal/$id"
        Jsoup.connect(url).get()
    }

    /**
     * The actual date of the journal.
     */
    val postDate: Date get() = FA.parseTime(postTime)


    /**
     * The content of the journal.
     */
    val content: Element by lazy {
        document.selectFirst("div.journal-body")
    }

    /**
     * The text of the journal.
     */
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

            val time = c.selectFirst("span.popup_date").attr("title")

            // Content is present in a specific div
            val content = c.selectFirst("div.message-text")

            // Add result to list
            result += Comment(id, replyToId, user, time, content, fa)
        }

        // Make list of mutable list
        result.toList()
    }
}

/**
 * A page of journals.
 * @property journals The journals on this page.
 * @property more True if there are more pages after this one.
 * @property fa The basic configuration for URLs, methods and constants and units.
 */
data class JournalPage(
        val user: String,
        val page: Int,
        val journals: List<Journal>,
        val more: Boolean,
        val fa: FA)

/**
 * Gets a specific journal page.
 * @param user The user to retrieve the page for.
 * @param page The page number to retrieve, starting at 1.
 * @param fa The basic configuration for URLs, methods and constants and units.
 */
fun journalPage(user: String, page: Int, fa: FA): JournalPage {
    // Get the URLS
    val pageSuffix = if (page == 1) "" else "/$page"
    val url = "${fa.url}/journals/$user$pageSuffix"

    // Get the document
    val doc = Jsoup.connect(url).get()

    // Find all tables that have Journal IDs
    val journals = doc
            .select("table[id^=jid]")
            .map {
                val id = it.id()
                        .substringAfter("jid:")
                val title = it.selectFirst("td.cat").text()
                val time = it.selectFirst("span.popup_date")?.attr("title") ?: ""
                val numComments = it.selectFirst("a:containsOwn(Comments \\()")
                        ?.text()
                        ?.substringAfter('(')
                        ?.substringBefore(')')
                        ?.toInt() ?: 0

                Journal(user, page, id, title, time, numComments, fa)
            }

    // There is a next page if there is a button pointing to a next page
    val more = doc.select("a.older").any()

    // Return results
    return JournalPage(user, page, journals, more, fa)
}

/**
 * Gets all journal pages until there are no more left.
 * @param user The user to find the pages for.
 * @param fa The basic configuration for URLs, methods and constants and units.
 * @return Returns the page number associated to the journal page.
 */
fun journalPages(user: String, fa: FA) =
        generateSequence(1 to journalPage(user, 1, fa)) { (p, j) ->
            if (j.more)
                (p + 1) to journalPage(user, p + 1, fa)
            else
                null
        }

/**
 * Gets all journals for the user.
 * @param user The user to get the journals for.
 * @param fa The basic configuration for URLs, methods and constants and units.
 * @return Returns all journals.
 */
fun journals(user: String, fa: FA) =
        journalPages(user, fa).flatMap { (_, j) -> j.journals.asSequence() }

fun main(args: Array<String>) {
    val x = journals("silvixen", FA.default)
    for (y in x)
        println(y.numComments)
}