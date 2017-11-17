package eu.pazuzu.fapi.commwatch

import com.sun.org.apache.xpath.internal.operations.Bool
import eu.pazuzu.fapi.FA
import eu.pazuzu.fapi.journals.journalsDaemon
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ProducerJob
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.launch
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.TelegramBotsApi
import org.telegram.telegrambots.api.methods.send.SendMessage
import org.telegram.telegrambots.api.objects.Update
import org.telegram.telegrambots.bots.TelegramLongPollingBot


fun main(args: Array<String>) {
    ApiContextInitializer.init()
    val botsApi = TelegramBotsApi()
    botsApi.registerBot(object : TelegramLongPollingBot() {
        private val inward = hashMapOf<String, Job>()

        private val outward = hashMapOf<Long, Set<String>>()

        override fun getBotToken() =
                "462590283:AAGD0kRDGnGccpgIMAi1BbL7dDr0jMosP5k"

        override fun getBotUsername() =
                "commissioneerbot"

        override fun onUpdateReceived(update: Update) {
            with(update) {
                if (hasMessage() && message.hasText()) {
                    val user = update.message.chatId

                    if (message.text.startsWith("/add ")) {
                        val users = message.text
                                .substringAfter("/add ")
                                .split(',')
                                .map { it.trim() }
                                .filter { !it.isBlank() }

                        outward[user] = (outward[user] ?: emptySet()) + users
                        updateJobs()
                    } else if (message.text.startsWith("/remove ")) {
                        val users = message.text
                                .substringAfter("/add ")
                                .split(',')
                                .map { it.trim() }
                                .filter { !it.isBlank() }

                        outward[user] = (outward[user] ?: emptySet()) - users
                        updateJobs()
                    } else if (message.text == "/list") {
                        val list = (outward[user] ?: emptySet())
                        if (list.isEmpty())
                            sendApiMethod(SendMessage(user, "Not yet listening to anyone, see /add."))
                        else
                            sendApiMethod(SendMessage(user, "Listening to: " + list.joinToString()))
                    }
                }
            }
        }

        private fun updateJobs() {
            val required = outward.values.flatten()

            val removed = inward.keys - required
            val added = required - inward.keys

            println("New requirements $required, adding $added, removing $removed")

            for (k in removed) {
                inward.remove(k)?.cancel()
                println("Cancelled job for $k")
            }
            for (a in added) {
                inward[a] = launch {
                    var last: Boolean? = null
                    journalsDaemon(a, FA.default).consumeEach {
                        val open = it
                                .lastOrNull { "open" in it.title.toLowerCase() || "closed" in it.title.toLowerCase() }
                                ?.title?.contains("open") ?: false

                        if (last != open) {
                            last = open
                            val text = if (open)
                                "Commission for $a are open"
                            else
                                "Commissions for $a are closed"

                            for ((c, xs) in outward)
                                if (a in xs)
                                    sendApiMethod(SendMessage(c, text))
                        }
                    }
                }
                println("Added job for $a")
            }
        }
    })
}