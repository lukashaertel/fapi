package eu.pazuzu.fapi.users

import eu.pazuzu.fapi.FA
import eu.pazuzu.fapi.comments.Comment
import eu.pazuzu.fapi.comments.toTree
import eu.pazuzu.fapi.submissions.Submission
import eu.pazuzu.fapi.util.parent
import eu.pazuzu.fapi.util.textAfter
import eu.pazuzu.fapi.watchlists.Direction
import eu.pazuzu.fapi.watchlists.watchlists
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.*

/**
 * A userpage.
 * @property user The user handle.
 * @property avatar The avatar of the user.
 * @property fullName The full name of the user.
 * @property artistType The artist type.
 * @property registeredTime the registration time as given on site.
 * @property mood The current mood.
 * @property content The userpage content.
 * @property numPageVisits The number of page visits.
 * @property numSubmissions The number of submissions.
 * @property numCommentsReceived The number of comments received.
 * @property numCommentsGiven The number of comments given.
 * @property numJournals The number of journals.
 * @property numFavorites The number of favorites.
 * @property fa The configuration used.
 */
data class Userpage(
        val user: String,
        val avatar: String,
        val fullName: String,
        val artistType: String,
        val registeredTime: String,
        val mood: String,
        val content: Element,
        val shouts: List<Comment>,
        val numPageVisits: Int,
        val numSubmissions: Int,
        val numCommentsReceived: Int,
        val numCommentsGiven: Int,
        val numJournals: Int,
        val numFavorites: Int,
        val fa: FA) {

    /**
     * The actual date of the registration.
     */
    val registeredDate: Date get() = FA.parseTime(registeredTime)

    /**
     * The text of the userpage.
     */
    val contentText get() = content.text()

    /**
     * Gets the submissions posted by the user.
     */
    val submissions get() = eu.pazuzu.fapi.submissions.submissions(user, fa)

    /**
     * Gets the faves of the user.
     */
    val faves get() = eu.pazuzu.fapi.submissions.faves(user, fa)

    /**
     * Gets the journals posted by the user.
     */
    val journals get() = eu.pazuzu.fapi.journals.journals(user, fa)

    /**
     * Gets all users the user is watching.
     */
    val watches get() = watchlists(user, Direction.Out, fa)

    /**
     * Gets all users watching the user.
     */
    val watchedBy get() = watchlists(user, Direction.In, fa)

    val mutuals get() = watches.toSortedSet() intersect watchedBy.toSortedSet()
}

/**
 * Gets a userpage from the handle.
 * @param user The user handle
 * @param fa The configuration to use
 * @return Returns a userpage object.
 */
fun userpage(user: String, fa: FA): Userpage {
    val url = "${fa.url}/user/$user/"

    // Get the document
    val doc = Jsoup.connect(url).get()
    val page = doc.selectFirst("table[id=page-userpage]")
    val avatarImage = page.selectFirst("img.avatar")
    val avatar = avatarImage.attr("src")

    val main = avatarImage.parent(4)
    val mainEntries = main.select("b").subList(1, 6)
    val fullName = mainEntries[0].textAfter() ?: ""
    val artistType = mainEntries[1].textAfter() ?: ""
    val registeredTime = mainEntries[2].textAfter() ?: ""
    val mood = mainEntries[3].textAfter() ?: ""
    val content = mainEntries[4]
            .nextElementSibling()
            .nextElementSibling()

    val stats = main.selectFirst("b:containsOwn(Statistics)").parent(3)
    val statsEntries = stats.select("b").subList(1, 7)

    val pageVisits = statsEntries[0].textAfter()?.trim()?.toInt() ?: 0
    val submissions = statsEntries[1].textAfter()?.trim()?.toInt() ?: 0
    val commentsReceived = statsEntries[2].textAfter()?.trim()?.toInt() ?: 0
    val commentsGiven = statsEntries[3].textAfter()?.trim()?.toInt() ?: 0
    val journals = statsEntries[4].textAfter()?.trim()?.toInt() ?: 0
    val favorites = statsEntries[5].textAfter()?.trim()?.toInt() ?: 0

    val shouts = page.select("table[id^=shout-]")
            .map {
                val id = it.id().substringAfter("shout-")
                val shoutUser = it.selectFirst("a[href^=/user/]").attr("href")
                        .substringAfter("/user/")
                        .substringBefore("/")
                val time = it.selectFirst("span.popup_date").attr("title")
                val shoutContent = it.selectFirst("div")
                Comment(id, null, shoutUser, time, shoutContent, fa)
            }

    return Userpage(user, avatar, fullName, artistType, registeredTime, mood, content, shouts, pageVisits,
            submissions, commentsReceived, commentsGiven, journals, favorites, fa)
}