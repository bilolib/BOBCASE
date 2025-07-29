package me.bilolib.bobcase

import me.bilolib.bobcase.sqlite.BobSQLite
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

object PluginAyarMenu : Listener {

    private val bekleyenler = mutableSetOf<UUID>()
    private val aktifMenuler = mutableMapOf<UUID, String>() // Oyuncu UUID -> Menü Başlığı

    private val plugin = Bobcase.instance

    val MENU_BASLIK: String
        get() {
            val secilenChatRenk = GenelAyarlar.renkler.random()
            return "${secilenChatRenk}${LangManager.msg("menu_options_title")}"
        }

    fun ac(player: Player) {
        val title = MENU_BASLIK
        aktifMenuler[player.uniqueId] = title

        val inv: Inventory = Bukkit.createInventory(null, 54, title)

        // Kasa bekleme süresi itemi
        val delayItem = ItemStack(Material.CLOCK).apply {
            itemMeta = itemMeta?.apply {
                val titleRaw = LangManager.msg("case_cooldown_time")
                val loreRaw = LangManager.msg("case_cooldown_time_lore")
                setDisplayName(ChatColor.translateAlternateColorCodes('&', titleRaw))
                lore = listOf(ChatColor.translateAlternateColorCodes('&', loreRaw.replace("{time}", (KasaAcmaListener.DELAY_MS / 1000).toString())))
            }
        }
        inv.setItem(10, delayItem)

        // Menü renk ayarı itemi
        val renkItem = ItemStack(Material.PAINTING).apply {
            itemMeta = itemMeta?.apply {
                val titleRaw = LangManager.msg("menu_color_settings")
                val loreRaw = LangManager.msg("menu_color_settings_lore")
                setDisplayName(ChatColor.translateAlternateColorCodes('&', titleRaw))
                lore = listOf(ChatColor.translateAlternateColorCodes('&', loreRaw))
            }
        }
        inv.setItem(12, renkItem)

        // Geri butonu (slot 53)
        val geriItem = ItemStack(Material.ARROW).apply {
            itemMeta = itemMeta?.apply {
                val backTitle = LangManager.msg("back_button")
                setDisplayName(ChatColor.translateAlternateColorCodes('&', backTitle))
            }
        }
        inv.setItem(53, geriItem)

        // Boş yerleri camlarla doldur
        val camRenkleri = MenuRenkAyarMenu.getCamlar(player)
        for (i in 0 until inv.size) {
            if (inv.getItem(i) == null) {
                val cam = ItemStack(camRenkleri.random())
                val camMeta = cam.itemMeta
                camMeta?.setDisplayName(" ")
                cam.itemMeta = camMeta
                inv.setItem(i, cam)
            }
        }

        player.openInventory(inv)
    }

    @EventHandler
    fun tiklama(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val title = aktifMenuler[player.uniqueId] ?: return

        if (event.view.title != title) return

        event.isCancelled = true
        val item = event.currentItem ?: return

        when (item.type) {
            Material.CLOCK -> {
                player.closeInventory()
                val msg = LangManager.msg("enter_new_delay_time")
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg))
                bekleyenler.add(player.uniqueId)
            }
            Material.PAINTING -> {
                MenuRenkAyarMenu.ac(player)
            }
            Material.ARROW -> {
                player.closeInventory()
                BobcaseMenu.openMenu(player)
            }
            else -> {
                // Boş bırakabiliriz
            }
        }
    }

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        if (!bekleyenler.contains(player.uniqueId)) return

        event.isCancelled = true

        try {
            val saniye = event.message.toInt()
            if (saniye !in 1..60) throw NumberFormatException()

            Bukkit.getScheduler().runTask(Bobcase.instance, Runnable {
                val delayMs = saniye * 1000L
                KasaAcmaListener.setDelay(delayMs)
                BobSQLite.setDelay(delayMs)
                val msg = LangManager.msg("messages_crate_cooldown_set").replace("{time}", saniye.toString())
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg))
            })
        } catch (e: NumberFormatException) {
            player.sendMessage(LangManager.msg("cooldown_invalid_number"))
        }

        bekleyenler.remove(player.uniqueId)
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        aktifMenuler.remove(player.uniqueId)
    }
}
