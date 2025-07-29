package me.bilolib.bobcase

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.inventory.ItemStack

object KasaOlusturmaListener : Listener {

    private val bekleyenler = mutableMapOf<Player, Int>() // Player -> taskId

    fun oyuncuyuBeklemeyeAl(player: Player) {
        // Önce varsa eski timeout iptal edilir
        bekleyenler[player]?.let { Bukkit.getScheduler().cancelTask(it) }

        player.sendMessage(LangManager.msg("enter_crate_name"))

        val taskId = Bukkit.getScheduler().runTaskLater(Bobcase.instance, Runnable {
            if (bekleyenler.containsKey(player)) {
                bekleyenler.remove(player)
                player.sendMessage(LangManager.msg("crate_creation_timeout"))
            }
        }, 20L * 10L).taskId // 20 tick = 1 saniye, 10 saniye için 20*10

        bekleyenler[player] = taskId
    }

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        if (!bekleyenler.containsKey(player)) return

        event.isCancelled = true
        val orijinalMesaj = event.message
        val mesaj = orijinalMesaj.trim()

        if (mesaj.equals("cancel", ignoreCase = true)) {
            player.sendMessage(LangManager.msg("crate_creation_cancelled"))

            bekleyenler[player]?.let { Bukkit.getScheduler().cancelTask(it) }
            bekleyenler.remove(player)
            return
        }

        // Renk kodu ve varsayılan renk
        val renkliIsim = if (mesaj.startsWith("&")) {
            ChatColor.translateAlternateColorCodes('&', mesaj)
        } else {
            ChatColor.translateAlternateColorCodes('&', "&6&l$mesaj")
        }

        val temizIsimRaw = ChatColor.stripColor(renkliIsim)

        // Başta boşluk kontrolü burda, renk kodları temizlendikten sonra
        if (temizIsimRaw!!.startsWith(" ")) {
            player.sendMessage(LangManager.msg("crate_name_no_leading_space"))

            return
        }

        val temizIsim = temizIsimRaw.trim()

        if (temizIsim.isEmpty()) {
            player.sendMessage(LangManager.msg("crate_name_cannot_be_empty"))
            return
        }

        if (Bobcase.kasalar.containsKey(temizIsim)) {
            player.sendMessage(LangManager.msg("crate_name_already_exists"))

            return
        }

        bekleyenler[player]?.let { Bukkit.getScheduler().cancelTask(it) }
        bekleyenler.remove(player)

        val gorunumItem = ItemStack(Material.CHEST)
        val yeniKasa = Kasa(
            renkliIsim = renkliIsim,
            temizIsim = temizIsim,
            esyaListesi = mutableListOf(),
            gorunumItem = gorunumItem
        )
        Bobcase.kasalar[temizIsim] = yeniKasa

        player.sendMessage(LangManager.msg("crate_name_created", "name" to renkliIsim))


        Bukkit.getScheduler().runTask(Bobcase.instance, Runnable {
            KasaAyarMenu.ac(player, yeniKasa.temizIsim)
        })
    }
}
