package eu.pazuzu.fapi.util

/**
 * True if link is "semantically equivalent".
 */
infix fun String.semanticEq(otherLink: String): Boolean {
    val effectiveThis = if (this.endsWith('/')) this else this + "/"
    val effectiveOtherLink = if (otherLink.endsWith('/')) otherLink else otherLink + "/"
    return effectiveThis == effectiveOtherLink
}