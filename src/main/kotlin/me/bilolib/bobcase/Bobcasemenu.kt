package me.bilolib.bobcase

import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
object BobcaseMenu : Listener {


    val ANA_MENU_BASLIK: String
        get() {
            val secilenChatRenk = GenelAyarlar.renkler.random()
            return "${secilenChatRenk}${LangManager.msg("menu_main_title")}"
        }


    fun openMenu(player: Player) {
        val inv = Bukkit.createInventory(null, 27, ANA_MENU_BASLIK)

        // 10. slot - Kasa Oluştur
        val slimeBall = ItemStack(Material.SLIME_BALL).apply {
            val meta = itemMeta
            meta?.setDisplayName(LangManager.msg("menu_create_case"))
            itemMeta = meta
        }
        inv.setItem(10, slimeBall)

        // 13. slot - Kasalar
        val chest = ItemStack(Material.CHEST).apply {
            val meta = itemMeta
            meta?.setDisplayName(LangManager.msg("menu_case"))
            itemMeta = meta
        }
        inv.setItem(13, chest)

        // 16. slot - Ayarlar
        val heart = ItemStack(Material.HEART_OF_THE_SEA).apply {
            val meta = itemMeta
            meta?.setDisplayName(LangManager.msg("menu_options"))
            itemMeta = meta
        }
        inv.setItem(16, heart)

        // Boş slotları camla doldur
        val camRenkleri = MenuRenkAyarMenu.getCamlar(player)
        for (i in 0 until inv.size) {
            if (inv.getItem(i) == null) {
                val cam = ItemStack(camRenkleri.random()).apply {
                    val meta = itemMeta
                    meta?.setDisplayName(" ")
                    itemMeta = meta
                }
                inv.setItem(i, cam)
            }
        }

        player.openInventory(inv)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val title = ChatColor.stripColor(event.view.title) ?: return

        if (title != ChatColor.stripColor(ANA_MENU_BASLIK)) return

        event.isCancelled = true

        when (event.rawSlot) {
            10 -> {
                player.closeInventory()
                KasaOlusturmaListener.oyuncuyuBeklemeyeAl(player)
            }
            13 -> {
                player.closeInventory()
                KasalarListMenu.ac(player)
            }
            16 -> {
                player.closeInventory()
                PluginAyarMenu.ac(player)
            }
        }
    }


}
