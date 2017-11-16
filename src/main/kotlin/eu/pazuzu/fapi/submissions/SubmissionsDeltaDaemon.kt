package eu.pazuzu.fapi.submissions

import eu.pazuzu.fapi.FA
import eu.pazuzu.fapi.util.Delta
import eu.pazuzu.fapi.util.DeltaAdd
import eu.pazuzu.fapi.util.DeltaChange
import eu.pazuzu.fapi.util.DeltaRemove
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import java.util.*


/**
 * Starts a daemon pulling the submissions of a user.
 * @param user The user to scan
 * @param fa The basic configuration for URLs, methods and constants and units.
 */
fun submissionsDeltaDaemon(user: String, fa: FA) =
        produce<Delta<Submission>> {
            // Backing for delta mapping
            val data = hashMapOf<String, Submission>()

            // While producer is active, repeat
            while (isActive) {

                // Get current submissions
                val submissions = submissions(user, fa).toList().asReversed()

                // Until there is a submission that the producer has already seen, send new ones.
                val unseen = HashSet<String>(data.keys)

                // Handle additions and changes
                for (j in submissions) {
                    // Prepare removal
                    unseen -= j.id

                    val old = data[j.id]
                    if (old == null)
                    // Old was not present, submission was added
                        channel.send(DeltaAdd(j))
                    else if (!fa.submissionEq(fa, old, j))
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
