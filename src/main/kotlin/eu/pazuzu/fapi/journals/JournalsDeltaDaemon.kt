package eu.pazuzu.fapi.journals

import eu.pazuzu.fapi.FA
import eu.pazuzu.fapi.util.Delta
import eu.pazuzu.fapi.util.DeltaAdd
import eu.pazuzu.fapi.util.DeltaChange
import eu.pazuzu.fapi.util.DeltaRemove
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import java.util.HashSet


/**
 * Starts a daemon pulling the journals of a user.
 * @param user The user to scan
 * @param fa The basic configuration for URLs, methods and constants and units.
 */
fun journalsDeltaDaemon(user: String, fa: FA) =
        produce<Delta<Journal>> {
            // Backing for delta mapping
            val data = hashMapOf<String, Journal>()

            // While producer is active, repeat
            while (isActive) {

                // Get current journals
                val journals = journals(user, fa).toList().asReversed()

                // Until there is a journal that the producer has already seen, send new ones.
                val unseen = HashSet<String>(data.keys)

                // Handle additions and changes
                for (j in journals) {
                    // Prepare removal
                    unseen -= j.id

                    val old = data[j.id]
                    if (old == null)
                    // Old was not present, journal was added
                        channel.send(DeltaAdd(j))
                    else if (!fa.journalEq(fa, old, j))
                    // Old was present but changed
                        channel.send(DeltaChange(old, j))

                    // Write data
                    data[j.id] = j
                }

                // Handle removal
                for (id in unseen) {
                    val j = data.remove(id)
                    if (j != null)
                        channel.send(DeltaRemove(j))
                }

                // Delay for a certain period of time
                delay(fa.delay, fa.delayUnit)
            }
        }
