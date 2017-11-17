package eu.pazuzu.fapi.watchlists

import eu.pazuzu.fapi.FA
import eu.pazuzu.fapi.submissions.favesPage
import eu.pazuzu.fapi.submissions.favesPages
import eu.pazuzu.fapi.util.semanticEq
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

data class WatchlistPage(
        val user: String,
        val page: Int,
        val users: List<String>,
        val more: Boolean,
        val fa: FA)

enum class Direction(val urlLabel: String) {
    Out("by"),
    In("to")
}

/**
 * Gets a specific watchlist page.
 * @param user The user to retrieve the page for.
 * @param page The page number to retrieve, starting at 1.
 * @param fa The basic configuration for URLs, methods and constants and units.
 */
fun watchlistPage(user: String, direction: Direction, page: Int, fa: FA): WatchlistPage {
    // Get the URLS
    val pageSuffix = if (page == 1) "" else "/$page"
    val pageAction = "/watchlist/${direction.urlLabel}/$user$pageSuffix"
    val url = "${fa.url}$pageAction"

    // Get the document
    val doc = Jsoup.connect(url).get()

    // Find all tables that have submission IDs
    val users = doc
            .select("span.artist_name")
            .map(Element::text)

    // There is a next page if the form action for the button is not the same page
    val moreButton = doc.selectFirst("button:containsOwn(Next 200)")
    val more = !(moreButton.parent().attr("action") semanticEq pageAction)

    // Return results
    return WatchlistPage(user, page, users, more, fa)
}

/**
 * Gets all watchlist pages until there are no more left.
 * @param user The user to find the pages for.
 * @param direction The direction of the watchlist.
 * @param fa The basic configuration for URLs, methods and constants and units.
 * @return Returns the page number associated to the watchlist page.
 */
fun watchlistPages(user: String, direction: Direction, fa: FA) =
        generateSequence(1 to watchlistPage(user, direction, 1, fa)) { (p, j) ->
            if (j.more)
                (p + 1) to watchlistPage(user, direction, p + 1, fa)
            else
                null
        }

/**
 * Gets all watches for the user.
 * @param user The user to get the watches for.
 * @param direction The direction of the watchlist.
 * @param fa The basic configuration for URLs, methods and constants and units.
 * @return Returns all watches.
 */
fun watchlists(user: String, direction: Direction, fa: FA) =
        watchlistPages(user, direction, fa).flatMap { (_, j) -> j.users.asSequence() }