package eu.pazuzu.fapi.util

import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

val Element.width: Int
    get() =
        attr("width")?.substringBefore('%')?.toInt() ?: 100

tailrec fun Element.parent(num: Int): Element =
        when {
            num < 0 -> throw IllegalArgumentException("num less than zero")
            num == 0 -> this
            else -> parent().parent(num - 1)
        }

fun Element.textBefore() = (previousSibling() as? TextNode)?.wholeText

fun Element.textAfter() = (nextSibling() as? TextNode)?.wholeText
