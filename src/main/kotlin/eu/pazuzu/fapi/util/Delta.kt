package eu.pazuzu.fapi.util

/**
 * Variants of delta, see [DeltaRemove], [DeltaChange] and [DeltaAdd].
 */
sealed class Delta<out T>

/**
 * [item] removed.
 */
data class DeltaRemove<out T>(val item: T) : Delta<T>()

/**
 * [itemFrom] changed to [itemTo].
 */
data class DeltaChange<out T>(val itemFrom: T, val itemTo: T) : Delta<T>()

/**
 * [item] added.
 */
data class DeltaAdd<out T>(val item: T) : Delta<T>()