package me.bilolib.bobcase


import me.bilolib.bobcase.menu.KazikazanMenu
import me.bilolib.bobcase.model.AnimasyonTuru
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import java.util.*

object KasaAcmaListener : Listener {

    private val sonAcilisZamani = mutableMapOf<UUID, Long>()
    private var delayMs = 5000L

    val DELAY_MS: Long get() = delayMs

    fun setDelay(ms: Long) {
        delayMs = ms
    }

    fun setSonAcilis(player: Player, zaman: Long) {
        sonAcilisZamani[player.uniqueId] = zaman
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return
        if (item.type == Material.AIR || !item.hasItemMeta()) return

        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        val kasaKey = NamespacedKey(Bobcase.instance, "kasa")
        val kasaIsmi = container.get(kasaKey, PersistentDataType.STRING) ?: return
        val kasa = Bobcase.kasalar[kasaIsmi] ?: return

        when (event.action) {
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
                event.isCancelled = true

                // Permission kontrolü
                val perm = kasa.permission
                if (!perm.isNullOrBlank() && !player.hasPermission(perm)) {
                    val msg = LangManager.msg("no_permission_open_case")
                        .replace("{permission}", perm)
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg))

                    return
                }

                // Gecikme kontrolü
                val simdi = System.currentTimeMillis()
                val onceki = sonAcilisZamani[player.uniqueId] ?: 0L
                if (simdi - onceki < delayMs) {
                    val kalan = ((delayMs - (simdi - onceki)) / 1000.0).coerceAtLeast(1.0)
                    val saniye = "%.1f".format(kalan)
                    player.sendMessage(
                        ChatColor.translateAlternateColorCodes('&',
                            LangManager.msg("wait_to_open_case").replace("{seconds}", saniye)
                        )
                    )
                    return
                }

                sonAcilisZamani[player.uniqueId] = simdi

                if (kasa.esyaListesi.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', LangManager.msg("case_empty")))
                    return
                }

                // Eşyayı eksilt
                val mainItem = player.inventory.itemInMainHand
                if (mainItem.amount > 1) {
                    mainItem.amount -= 1
                } else {
                    player.inventory.setItemInMainHand(null)
                }

                // Animasyona göre aç
                when (kasa.animasyonTuru) {
                    AnimasyonTuru.CSGO -> CsgoAnimasyon.ac(player, kasa)
                    AnimasyonTuru.RULET -> RuletKasa.ac(player, kasa, Bobcase.instance)
                    AnimasyonTuru.KAZIKAZAN -> KazikazanMenu.ac(player, kasa)
                }
            }

            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
                event.isCancelled = true
                KasaOnizlemeMenu.ac(player, kasaIsmi)
            }

            else -> return
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        sonAcilisZamani.remove(event.player.uniqueId)
    }
}
