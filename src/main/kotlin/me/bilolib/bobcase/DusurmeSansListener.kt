package me.bilolib.bobcase

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

object SansDusurmeListener : Listener {

    private data class Bekleme(val kasaIsmi: String, val hedef: String)

    private val bekleyenler = mutableMapOf<Player, Bekleme>()

    fun beklemeyeAl(player: Player, kasaIsmi: String, hedef: String) {
        bekleyenler[player] = Bekleme(kasaIsmi, hedef)


    }

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val bekleme = bekleyenler[player] ?: return
        event.isCancelled = true

        val mesaj = event.message.trim()
        val cancelKeyword = LangManager.msg("cancel_keyword") // Örneğin "cancel"
        if (mesaj.equals(cancelKeyword, ignoreCase = true)){
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', LangManager.msg("operation_cancelled")))

            bekleyenler.remove(player)
            return
        }

        val sayi = mesaj.toDoubleOrNull()
        if (sayi == null || sayi < 1.0 || sayi > 100.0) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', LangManager.msg("invalid_number")))
            return
        }

        val sans = sayi / 100.0
        val kasa = Bobcase.kasalar[bekleme.kasaIsmi] ?: run {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', LangManager.msg("drop_crate_not_found")))
            bekleyenler.remove(player)
            return
        }

        // Hedefin mob mu yoksa block mu olduğunu kontrol et
        if (bekleme.hedef in kasa.dusurmeMobList) {
            kasa.dusurmeMobSanslari[bekleme.hedef] = sans
        } else if (bekleme.hedef in kasa.dusurmeBlockList) {
            kasa.dusurmeBlockSanslari[bekleme.hedef] = sans
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', LangManager.msg("must_save_before_chance")))


            bekleyenler.remove(player)
            return
        }
        KasaDAO.kasaKaydet(kasa)
        val msg = LangManager.msg("drop_chance_set")
            .replace("{target}", bekleme.hedef)
            .replace("{value}", sayi.toString())

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg))
        bekleyenler.remove(player)

        Bukkit.getScheduler().runTask(Bobcase.instance, Runnable {
            DusurmeAyarMenu.ac(player, bekleme.kasaIsmi)
        })

    }
}
