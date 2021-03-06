package dev.giuliopime.helpdesk.utils

import dev.giuliopime.helpdesk.bot.HelpDesk
import dev.giuliopime.helpdesk.bot.internals.Settings
import dev.giuliopime.helpdesk.cache.handlers.GuildsHandler
import dev.giuliopime.helpdesk.data.topgg.TopggStatsD
import dev.giuliopime.helpdesk.database.managers.GuildsForRemovalManager
import dev.giuliopime.helpdesk.timeseriesDB.controllers.GuildStatsController
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import kotlin.concurrent.fixedRateTimer


private val logger = KotlinLogging.logger {  }

object FixedRateTimers {
    private val guildsForRemoval = fixedRateTimer("Guild for removal handler", false, 0L, 86400000) {
        val guildsForRemoval = GuildsForRemovalManager.getAll()
        val guildIDsToRemove = mutableListOf<String>()

        val currentTime = System.currentTimeMillis()

        guildsForRemoval.forEach {
            if (currentTime - it.timestampAdded >= 604800000)
                guildIDsToRemove.add(it.guildID)
        }

        if (guildIDsToRemove.isNotEmpty()) {
            GuildsForRemovalManager.deleteAll(guildIDsToRemove)
            GuildsHandler.deleteAll(guildIDsToRemove)
        }
    }

    private val topggStatsPoster = fixedRateTimer("Top.gg stats poster", false, 0L, 43200000) {
        if (!Settings.testing) {
            GlobalScope.async {
                ApisConsumer.postServerCount(
                    TopggStatsD(HelpDesk.shardsManager.guilds.size.toLong(), HelpDesk.shardsManager.shardsTotal)
                )
            }
        }
    }

    private val totalGuildsPoster = fixedRateTimer("Total guilds poster", false, 0L, 60000) {
        if (HelpDesk.shardsManager.statuses.all { it.value == JDA.Status.CONNECTED })
            GuildStatsController.writeTotalGuilds(HelpDesk.shardsManager.guilds.size.toLong())
    }

    init {
        logger.info("Started fixed rate timers!")
    }

    fun shutdown() {
        guildsForRemoval.cancel()
        topggStatsPoster.cancel()
        totalGuildsPoster.cancel()
        logger.info("Fixed rate timers shutdown!")
    }
}
