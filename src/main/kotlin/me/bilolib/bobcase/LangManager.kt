package me.bilolib.bobcase

import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader

object LangManager {
    lateinit var dilDosyasi: YamlConfiguration
        private set

    fun loadLanguage(plugin: Bobcase) {
        val lang = plugin.config.getString("language") ?: "en"
        val langFolder = File(plugin.dataFolder, "lang")
        if (!langFolder.exists()) langFolder.mkdirs()

        val file = File(langFolder, "$lang.yml")

        if (!file.exists()) {
            val defaultLangStream = plugin.getResource("lang/en.yml") ?: plugin.getResource("en.yml")
            if (defaultLangStream != null) {
                file.parentFile?.mkdirs()
                InputStreamReader(defaultLangStream, Charsets.UTF_8).use { reader ->
                    file.writeText(reader.readText())
                }
            } else {
            }
        }

        dilDosyasi = YamlConfiguration.loadConfiguration(file)
    }

    fun reloadLanguage(plugin: Bobcase) {
        loadLanguage(plugin)
    }

    fun msg(key: String): String {
        val raw = dilDosyasi.getString(key) ?: "&c[key: $key]"
        return ChatColor.translateAlternateColorCodes('&', raw)
    }

    fun msg(key: String, vararg args: Pair<String, Any>): String {
        var raw = dilDosyasi.getString(key) ?: "&c[key: $key]"
        for ((k, v) in args) {
            raw = raw.replace("{$k}", v.toString())
        }
        return ChatColor.translateAlternateColorCodes('&', raw)
    }
}
