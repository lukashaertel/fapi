package eu.pazuzu.fapi.submissions

import eu.pazuzu.fapi.FA
import eu.pazuzu.fapi.listEq
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay

/**
 * Starts a daemon pulling the submissions of a user.
 * @param user The user to scan
 * @param fa The basic configuration for URLs, methods and constants and units.
 * @return Returns a producer of lists of submissions, where elements are polled based on the configuration in [fa]. New
 * items will only be generated if an actual change occurred.
 */
fun submissionsDaemon(user: String, fa: FA) =
        produce<List<Submission>> {
            // Memorize last output
            var last: List<Submission>? = null

            // While producer is active, repeat
            while (isActive) {
                // Get current submissions
                val current = submissions(user, fa).toList()
                if (last == null || fa.listEq(last, current, fa.submissionEq)) {
                    channel.send(current)
                    last = current
                }

                // Delay for a certain period of time
                delay(fa.delay, fa.delayUnit)
            }
        }