package me.bilolib.bobcase

import RuletKasa
import me.bilolib.bobcase.menu.KasaAnimasyonAyarMenu

import me.bilolib.bobcase.menu.KazikazanMenu
import me.bilolib.bobcase.sqlite.BobSQLite
import me.bilolib.bobcase.sqlite.MenuRenkDAO
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin


class Bobcase : JavaPlugin() {

    companion object {
        lateinit var instance: Bobcase
            private set

        val kasalar = mutableMapOf<String, Kasa>()
    }

    data class KasaEsya(
        val item: ItemStack,
        var sans: Double = 1.0  // % olarak 0.0 - 1.0 arasında, default 100%
    )

    override fun onEnable() {
        instance = this

        saveDefaultConfig()
        LangManager.loadLanguage(this)

        // Veritabanı başlat
        BobSQLite.baslat(this)
        KasaAcmaListener.setDelay(BobSQLite.getDelay())

        // Kasa verilerini yükle
        kasalar.putAll(KasaDAO.kasaYukle())

        // Renk verilerini yükle
        val tumRenkler = MenuRenkDAO.tumCamRenkleriniYukle()
        for ((uuid, camList) in tumRenkler) {
            MenuRenkAyarMenu.renkKayitYukle(uuid, camList)
        }

        // /bobcase komutu
        getCommand("bobcase")?.setExecutor { sender, _, _, args ->
            if (!sender.hasPermission("bobcase.admin")) {
                sender.sendMessage(LangManager.msg("no_permission"))
                return@setExecutor true
            }

            if (args.isNotEmpty() && args[0].equals("reload", ignoreCase = true)) {
                reloadPlugin(sender)
                return@setExecutor true
            }

            if (sender is Player) {
                BobcaseMenu.openMenu(sender)
            } else {
                sender.sendMessage(LangManager.msg("only_players"))
            }
            true
        }

        // Event kayıtları
        val pm = server.pluginManager
        listOf(
            BobCaseEvent,
            BobcaseMenu,
            KasaOlusturmaListener,
            KasaAyarMenu,
            KasaEsyaEkleMenu,
            KasalarListMenu,
            KasaAcmaListener,
            SansAyarListener,
            KasaItemMenu,
            DusurmeAyarMenu,
            SansDusurmeListener,
            KasaOnizlemeMenu,
            PermAyarListener,
            PluginAyarMenu,
            MenuRenkAyarMenu,
            KasaAnimasyonAyarMenu,
            CsgoAnimasyon,
            RuletKasa,
            KazikazanMenu
        ).forEach { pm.registerEvents(it, this) }
    }

    override fun onDisable() {
        BobSQLite.kapat()
    }

    private fun reloadPlugin(sender: CommandSender) {
        reloadConfig()
        LangManager.reloadLanguage(this)
        sender.sendMessage(LangManager.msg("plugin_reloaded"))
    }
}