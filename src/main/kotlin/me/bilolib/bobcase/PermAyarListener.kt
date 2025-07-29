package me.bilolib.bobcase

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

object PermAyarListener : Listener {

    // Player → kasaIsmi, izin ayarlamak için beklemede olan oyuncular
    private val izinBekleyen = mutableMapOf<Player, String>()

    // Bu fonksiyonla başka yerden beklemeye alabiliriz
    fun izinBeklemeyeAl(player: Player, kasaIsmi: String) {
        izinBekleyen[player] = kasaIsmi
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', LangManager.msg("enter_permission_message")))

    }

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val kasaIsmi = izinBekleyen[player] ?: return

        event.isCancelled = true
        val mesaj = event.message.trim()

        if (mesaj.equals(LangManager.msg("cancel_keyword"), ignoreCase = true)){
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', LangManager.msg("permission_setup_cancelled")))

            izinBekleyen.remove(player)
            return
        }

        // İzin adı olarak kabul et
        val kasa = Bobcase.kasalar[kasaIsmi]
        if (kasa != null) {
            kasa.permission = mesaj
            player.sendMessage(
                ChatColor.translateAlternateColorCodes('&', LangManager.msg("case_permission_set").replace("{permission}", mesaj))
            )

        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', LangManager.msg("case_not_found")))

        }

        izinBekleyen.remove(player)

        // Menü tekrar açılabilir, main thread’de çalıştır
        Bukkit.getScheduler().runTask(Bobcase.instance, Runnable {
            KasaAyarMenu.ac(player, kasaIsmi)
        })
    }

}
