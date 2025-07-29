package me.bilolib.bobcase

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

object SansAyarListener : Listener {

    private data class SansBekleme(val kasaIsmi: String, val index: Int)

    private val bekleyenler = mutableMapOf<Player, SansBekleme>()

    fun beklemeyeAl(player: Player, kasaIsmi: String, index: Int) {
        bekleyenler[player] = SansBekleme(kasaIsmi, index)
    }

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val bekleme = bekleyenler[player] ?: return
        event.isCancelled = true

        val mesaj = event.message.trim()
        val cancelWord = LangManager.msg("cancel_keyword")
        if (mesaj.equals(cancelWord, ignoreCase = true)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', LangManager.msg("chance_setting_cancelled")))

            bekleyenler.remove(player)
            return
        }

        val sansDegerRaw = mesaj.toDoubleOrNull()
        if (sansDegerRaw == null || sansDegerRaw < 1.0 || sansDegerRaw > 100.0) {
            player.sendMessage(LangManager.msg("invalid_number"))

            return
        }




        val kasa = Bobcase.kasalar[bekleme.kasaIsmi]
        if (kasa == null || bekleme.index !in kasa.esyaListesi.indices) {
            player.sendMessage(LangManager.msg("case_or_item_not_found"))
            bekleyenler.remove(player)
            return
        }
        val sansDeger = sansDegerRaw / 100.0
        kasa.esyaListesi[bekleme.index].sans = sansDeger
        player.sendMessage(
            ChatColor.translateAlternateColorCodes('&',
                LangManager.msg("chance_value_set").replace("%value%", "$sansDegerRaw")
            )
        )

        bekleyenler.remove(player)

        // Menü yeniden açılabilir
        Bukkit.getScheduler().runTask(Bobcase.instance, Runnable {
            KasaEsyaEkleMenu.ac(player, bekleme.kasaIsmi)
        })

    }
}
