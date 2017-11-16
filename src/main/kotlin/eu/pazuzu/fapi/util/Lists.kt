package eu.pazuzu.fapi.util

inline fun <T, U> List<T>.mapWithBefore(block: (T?, T) -> U) =
        ((listOf<T?>(null) + this) zip this).map { (l, r) -> block(l, r) }