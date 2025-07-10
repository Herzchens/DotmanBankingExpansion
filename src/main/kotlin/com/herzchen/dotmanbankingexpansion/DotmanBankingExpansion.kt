package com.herzchen.dotmanbankingexpansion

import org.bukkit.plugin.java.JavaPlugin
import com.herzchen.dotmanbankingexpansion.command.ReloadCommand
import org.bukkit.Bukkit

class DotmanBankingExpansion : JavaPlugin() {
    lateinit var configManager: ConfigManager
    lateinit var pluginLogger: PluginLogger
    private lateinit var discordBot: DiscordBot

    override fun onEnable() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        saveResource("config.yml", false)
        saveResource("log.yml", false)

        configManager = ConfigManager(this)
        configManager.loadConfig()

        pluginLogger = PluginLogger(this)
        pluginLogger.log("Starting plugin...")

        if (!isJDAAvailable()) {
            server.consoleSender.sendMessage("§c================= §6DotmanBankingExpansion §c==================")
            server.consoleSender.sendMessage("§c==§e                                                    §c==")
            server.consoleSender.sendMessage("§c==§e                     LỖI NGHIÊM TRỌNG                §c==")
            server.consoleSender.sendMessage("§c==§e              KHÔNG TÌM THẤY THƯ VIỆN JDA           §c==")
            server.consoleSender.sendMessage("§c==§e              VUI LÒNG CÀI ĐẶT JDA 5.0.0+           §c==")
            server.consoleSender.sendMessage("§c==§e                                                    §c==")
            server.consoleSender.sendMessage("§c========================§c§c===============================")

            pluginLogger.log("CRITICAL ERROR: JDA library not found! Please install JDA 5.0.0+")
            pluginLogger.log("Download: https://github.com/DV8FromTheWorld/JDA/releases")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        if (configManager.token.isEmpty()) {
            server.consoleSender.sendMessage("§c================= §6DotmanBankingExpansion §c==================")
            server.consoleSender.sendMessage("§c==§e                                                    §c==")
            server.consoleSender.sendMessage("§c==§e                    CẢNH BÁO CẤU HÌNH               §c==")
            server.consoleSender.sendMessage("§c==§e              DISCORD TOKEN TRỐNG!                 §c==")
            server.consoleSender.sendMessage("§c==§e              VUI LÒNG KIỂM TRA CONFIG.YML          §c==")
            server.consoleSender.sendMessage("§c==§e                                                    §c==")
            server.consoleSender.sendMessage("§c========================§c§c===============================")

            pluginLogger.log("ERROR: Discord token is empty! Check config.yml")
        } else {
            try {
                discordBot = DiscordBot(this)
                discordBot.start()
            } catch (e: Exception) {
                server.consoleSender.sendMessage("§c================= §6DotmanBankingExpansion §c==================")
                server.consoleSender.sendMessage("§c==§e                                                    §c==")
                server.consoleSender.sendMessage("§c==§e                LỖI KẾT NỐI DISCORD                §c==")
                server.consoleSender.sendMessage("§c==§e              ${e.message?.take(42)?.padEnd(42)}              §c==")
                server.consoleSender.sendMessage("§c==§e                                                    §c==")
                server.consoleSender.sendMessage("§c========================§c§c===============================")

                pluginLogger.log("Failed to start Discord bot: ${e.message}")
            }
        }

        getCommand("dbankreload")?.setExecutor(ReloadCommand(this))

        server.consoleSender.sendMessage("§a=================== §6DotmanBankingExpansion §a==================")
        server.consoleSender.sendMessage("§a==§f                                                    §a==")
        server.consoleSender.sendMessage("§a==§f                   PLUGIN ĐÃ ĐƯỢC BẬT                §a==")
        server.consoleSender.sendMessage("§a==§f                 DISCORD BOT ĐÃ KẾT NỐI              §a==")
        server.consoleSender.sendMessage("§a==§f              MỌI VẤN ĐỀ XIN LIÊN HỆ DISCORD         §a==")
        server.consoleSender.sendMessage("§a==§f                    itztli_herzchen                  §a==")
        server.consoleSender.sendMessage("§a==§f                                                    §a==")
        server.consoleSender.sendMessage("§a============================§a§a=============================")

        pluginLogger.log("Plugin enabled successfully")
    }

    private fun isJDAAvailable(): Boolean {
        return try {
            Class.forName("net.dv8tion.jda.api.JDA")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    override fun onDisable() {
        if (::discordBot.isInitialized) {
            discordBot.shutdown()
        }

        server.consoleSender.sendMessage("§c=================== §6DotmanBankingExpansion §c==================")
        server.consoleSender.sendMessage("§c==§e                                                    §c==")
        server.consoleSender.sendMessage("§c==§e                   PLUGIN ĐÃ BỊ TẮT                 §c==")
        server.consoleSender.sendMessage("§c==§e                 DISCORD BOT ĐÃ NGẮT KẾT NỐI        §c==")
        server.consoleSender.sendMessage("§c==§e                                                    §c==")
        server.consoleSender.sendMessage("§c============================§c§c=============================")

        pluginLogger.log("Plugin disabled")
    }

    fun reload() {
        configManager.loadConfig()
        pluginLogger.reloadConfig()

        if (::discordBot.isInitialized) {
            discordBot.shutdown()
            try {
                discordBot = DiscordBot(this)
                discordBot.start()

                server.consoleSender.sendMessage("§a=================== §6DotmanBankingExpansion §a==================")
                server.consoleSender.sendMessage("§a==§f                                                    §a==")
                server.consoleSender.sendMessage("§a==§f                  CẤU HÌNH ĐÃ TẢI LẠI               §a==")
                server.consoleSender.sendMessage("§a==§f                DISCORD BOT ĐÃ KHỞI ĐỘNG LẠI       §a==")
                server.consoleSender.sendMessage("§a==§f                                                    §a==")
                server.consoleSender.sendMessage("§a============================§a§a=============================")

            } catch (e: Exception) {
                server.consoleSender.sendMessage("§c================= §6DotmanBankingExpansion §c==================")
                server.consoleSender.sendMessage("§c==§e                                                    §c==")
                server.consoleSender.sendMessage("§c==§e            LỖI KHI KHỞI ĐỘNG LẠI DISCORD BOT        §c==")
                server.consoleSender.sendMessage("§c==§e              ${e.message?.take(42)?.padEnd(42)}              §c==")
                server.consoleSender.sendMessage("§c==§e                                                    §c==")
                server.consoleSender.sendMessage("§c========================§c§c===============================")

                pluginLogger.log("Failed to restart Discord bot: ${e.message}")
            }
        }

        pluginLogger.log("Configuration reloaded")
    }
}