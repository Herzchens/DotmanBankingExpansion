package com.herzchen.dotmanbankingexpansion

import com.herzchen.dotmanbankingexpansion.command.DBECommand
import com.herzchen.dotmanbankingexpansion.command.StreakInfoCommand
import com.herzchen.dotmanbankingexpansion.command.TabComplete
import com.herzchen.dotmanbankingexpansion.config.ConfigManager
import com.herzchen.dotmanbankingexpansion.manager.ConfirmationManager
import com.herzchen.dotmanbankingexpansion.manager.StreakDataManager
import com.herzchen.dotmanbankingexpansion.manager.StreakTokenManager
import com.herzchen.dotmanbankingexpansion.notifier.DiscordBot
import com.herzchen.dotmanbankingexpansion.repository.YamlStreakRepository
import com.herzchen.dotmanbankingexpansion.scheduler.StreakScheduler
import com.herzchen.dotmanbankingexpansion.service.StreakService
import com.herzchen.dotmanbankingexpansion.ui.BossBarManager
import com.herzchen.dotmanbankingexpansion.util.PluginLogger

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class DotmanBankingExpansion : JavaPlugin() {

    lateinit var configManager: ConfigManager
    lateinit var pluginLogger: PluginLogger
    private lateinit var discordBot: DiscordBot

    private lateinit var streakRepo: YamlStreakRepository
    lateinit var streakService: StreakService
    lateinit var streakDataManager: StreakDataManager
    lateinit var streakTokenManager: StreakTokenManager
    private lateinit var bossBarManager: BossBarManager
    private lateinit var streakScheduler: StreakScheduler
    lateinit var confirmationManager: ConfirmationManager
    lateinit var streakInfoCommand: StreakInfoCommand

    override fun onEnable() {
        if (!dataFolder.exists()) dataFolder.mkdirs()
        saveDefaultConfig()

        configManager = ConfigManager(this).also { it.loadConfig() }
        pluginLogger = PluginLogger(this)
        pluginLogger.log("Starting plugin...")

        if (!isJDAAvailable()) {
            Bukkit.getConsoleSender().sendMessage("§c================= §6DotmanBankingExpansion §c==================")
            Bukkit.getConsoleSender().sendMessage("§c==§e                                                    §c==")
            Bukkit.getConsoleSender().sendMessage("§c==§e                     LỖI NGHIÊM TRỌNG                §c==")
            Bukkit.getConsoleSender().sendMessage("§c==§e              KHÔNG TÌM THẤY THƯ VIỆN JDA           §c==")
            Bukkit.getConsoleSender().sendMessage("§c==§e              VUI LÒNG CÀI ĐẶT JDA 5.0.0+           §c==")
            Bukkit.getConsoleSender().sendMessage("§c==§e                                                    §c==")
            Bukkit.getConsoleSender().sendMessage("§c========================§c§c===============================")
            pluginLogger.log("CRITICAL ERROR: JDA library not found! Please install JDA 5.0.0+")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        if (configManager.token.isBlank()) {
            Bukkit.getConsoleSender().sendMessage("§c================= §6DotmanBankingExpansion §c==================")
            Bukkit.getConsoleSender().sendMessage("§c==§e                                                    §c==")
            Bukkit.getConsoleSender().sendMessage("§c==§e                    CẢNH BÁO CẤU HÌNH               §c==")
            Bukkit.getConsoleSender().sendMessage("§c==§e              DISCORD TOKEN TRỐNG!                 §c==")
            Bukkit.getConsoleSender().sendMessage("§c==§e              VUI LÒNG KIỂM TRA CONFIG.YML          §c==")
            Bukkit.getConsoleSender().sendMessage("§c==§e                                                    §c==")
            Bukkit.getConsoleSender().sendMessage("§c========================§c§c===============================")
            pluginLogger.log("ERROR: Discord token is empty! Check config.yml")
        } else {
            try {
                discordBot = DiscordBot(this)
                discordBot.start()
            } catch (e: Exception) {
                Bukkit.getConsoleSender().sendMessage("§c================= §6DotmanBankingExpansion §c==================")
                Bukkit.getConsoleSender().sendMessage("§c==§e                                                    §c==")
                Bukkit.getConsoleSender().sendMessage("§c==§e                LỖI KẾT NỐI DISCORD                §c==")
                Bukkit.getConsoleSender().sendMessage("§c==§e              ${e.message?.take(42)?.padEnd(42)}              §c==")
                Bukkit.getConsoleSender().sendMessage("§c==§e                                                    §c==")
                Bukkit.getConsoleSender().sendMessage("§c========================§c§c===============================")
                pluginLogger.log("Failed to start Discord bot: ${e.message}")
            }
        }

        streakRepo      = YamlStreakRepository(dataFolder)
        streakService   = StreakService(streakRepo, configManager.streakCycleHours.toLong())
        streakDataManager = StreakDataManager(streakRepo)
        streakTokenManager = StreakTokenManager(streakRepo)
        bossBarManager  = BossBarManager(streakService)
        streakScheduler = StreakScheduler(this, streakService, bossBarManager)
        streakScheduler.start()
        confirmationManager = ConfirmationManager(this)
        streakInfoCommand = StreakInfoCommand(this)
        pluginLogger.log("Streak subsystem initialized")

        getCommand("dbe")?.setExecutor(DBECommand(this))
        getCommand("dbe")?.tabCompleter = TabComplete()

        Bukkit.getConsoleSender().sendMessage("§a=================== §6DotmanBankingExpansion §a==================")
        Bukkit.getConsoleSender().sendMessage("§a==§f                                                    §a==")
        Bukkit.getConsoleSender().sendMessage("§a==§f                   PLUGIN ĐÃ ĐƯỢC BẬT                §a==")
        Bukkit.getConsoleSender().sendMessage("§a==§f                 DISCORD BOT ĐÃ KẾT NỐI              §a==")
        Bukkit.getConsoleSender().sendMessage("§a==§f              MỌI VẤN ĐỀ XIN LIÊN HỆ DISCORD         §a==")
        Bukkit.getConsoleSender().sendMessage("§a==§f                    itztli_herzchen                  §a==")
        Bukkit.getConsoleSender().sendMessage("§a==§f                                                    §a==")
        Bukkit.getConsoleSender().sendMessage("§a============================§a§a=============================")
        pluginLogger.log("Plugin enabled successfully")
    }

    override fun onDisable() {
        if (::discordBot.isInitialized) {
            try { discordBot.shutdown() } catch (_: Exception) { }
        }

        Bukkit.getConsoleSender().sendMessage("§c=================== §6DotmanBankingExpansion §c==================")
        Bukkit.getConsoleSender().sendMessage("§c==§e                                                    §c==")
        Bukkit.getConsoleSender().sendMessage("§c==§e                   PLUGIN ĐÃ BỊ TẮT                 §c==")
        Bukkit.getConsoleSender().sendMessage("§c==§e                 DISCORD BOT ĐÃ NGẮT KẾT NỐI        §c==")
        Bukkit.getConsoleSender().sendMessage("§c==§e                                                    §c==")
        Bukkit.getConsoleSender().sendMessage("§c============================§c§c=============================")
        pluginLogger.log("Plugin disabled")
    }

    fun reload() {
        configManager.loadConfig()
        pluginLogger.log("Configuration reloaded")

        if (::discordBot.isInitialized) {
            try {
                discordBot.shutdown()
                discordBot = DiscordBot(this)
                discordBot.start()

                Bukkit.getConsoleSender().sendMessage("§a=================== §6DotmanBankingExpansion §a==================")
                Bukkit.getConsoleSender().sendMessage("§a==§f                                                    §a==")
                Bukkit.getConsoleSender().sendMessage("§a==§f                  CẤU HÌNH ĐÃ TẢI LẠI               §a==")
                Bukkit.getConsoleSender().sendMessage("§a==§f                DISCORD BOT ĐÃ KHỞI ĐỘNG LẠI       §a==")
                Bukkit.getConsoleSender().sendMessage("§a==§f                                                    §a==")
                Bukkit.getConsoleSender().sendMessage("§a============================§a§a=============================")
                pluginLogger.log("Discord bot restarted")
            } catch (e: Exception) {
                Bukkit.getConsoleSender().sendMessage("§c================= §6DotmanBankingExpansion §c==================")
                Bukkit.getConsoleSender().sendMessage("§c==§e                                                    §c==")
                Bukkit.getConsoleSender().sendMessage("§c==§e            LỖI KHI KHỞI ĐỘNG LẠI DISCORD BOT        §c==")
                Bukkit.getConsoleSender().sendMessage("§c==§e              ${e.message?.take(42)?.padEnd(42)}              §c==")
                Bukkit.getConsoleSender().sendMessage("§c==§e                                                    §c==")
                Bukkit.getConsoleSender().sendMessage("§c========================§c§c===============================")
                pluginLogger.log("Failed to restart Discord bot: ${e.message}")
            }
        }

        if (configManager.token.isBlank()) {
            logger.warning("Discord token is empty! Bot will not start.")
            return
        }

        Bukkit.getConsoleSender().sendMessage("§a=== Configuration reloaded ===")
    }

    private fun isJDAAvailable(): Boolean = try {
        Class.forName("net.dv8tion.jda.api.JDA")
        true
    } catch (_: ClassNotFoundException) {
        false
    }
}