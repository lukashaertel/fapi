package eu.pazuzu.fapi.util

/**
 * A node with children.
 */
data class Node<T>(val item: T, val children: List<Node<T>>) {
    /**
     * Internal visitor function with depth.
     */
    private fun visit(depth: Int, acceptor: (Int, T) -> Unit) {
        acceptor(depth, item)
        for (c in children)
            c.visit(depth + 1, acceptor)
    }

    /**
     * Visits the item and the children recursively, takes depth into account
     */
    fun visit(acceptor: (Int, T) -> Unit) =
            visit(0, acceptor)
}