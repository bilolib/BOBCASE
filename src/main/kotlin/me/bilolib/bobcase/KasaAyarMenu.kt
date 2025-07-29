package me.bilolib.bobcase

import me.bilolib.bobcase.menu.KasaAnimasyonAyarMenu.oyuncuyaAnimasyonSecimMenusuAc
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack

object KasaAyarMenu : Listener {
    private fun renkliMesaj(key: String): String {
        return ChatColor.translateAlternateColorCodes('&', LangManager.msg(key))
    }
    private val openMenus = mutableMapOf<Player, String>()

    // Menü başlığını dinamik, config ve renkli yapıyoruz
    val KASA_AYAR_MENU_BASLIK: String
        get() {
            val secilenChatRenk = GenelAyarlar.renkler.random()
            return "${secilenChatRenk}${LangManager.msg("crate_settings_title")}"
        }

    fun ac(player: Player, kasaIsmi: String?) {
        if (kasaIsmi == null) {
            player.sendMessage(LangManager.msg("invalid_crate_name"))
            return
        }

        // Menü başlığında kasa ismini renklendir ve {name} ile değiştir
        val title = KASA_AYAR_MENU_BASLIK

        val invSize = 27
        val inv = Bukkit.createInventory(null, invSize, title)

        inv.setItem(4, olusturYetkiItem(kasaIsmi))
        inv.setItem(10, olusturItem(Material.CHEST, renkliMesaj("crate_add_item")))
        inv.setItem(12, olusturItem(Material.DISPENSER, renkliMesaj("crate_drop_setting")))
        inv.setItem(8, olusturItem(Material.REDSTONE, renkliMesaj("crate_delete")))
        inv.setItem(26, olusturItem(Material.BOOK, renkliMesaj("back_button")))
        inv.setItem(22, olusturItem(Material.ENDER_EYE, renkliMesaj("crate_open_styles")))

        val kasa = Bobcase.kasalar[kasaIsmi]
        val gorunumItem = kasa?.gorunumItem?.clone()
            ?: olusturItem(Material.CHEST, renkliMesaj("crate_change_appearance"))
        val meta = gorunumItem.itemMeta
        meta?.setDisplayName(ChatColor.translateAlternateColorCodes('&', LangManager.msg("change_crate_appearance")))
        gorunumItem.itemMeta = meta
        inv.setItem(14, gorunumItem)

        // Boş slotları rastgele camlarla dolduruyoruz
        val camRenkleri = MenuRenkAyarMenu.getCamlar(player)
        for (i in 0 until invSize) {
            if (inv.getItem(i) == null) {
                val cam = ItemStack(camRenkleri.random())
                val camMeta = cam.itemMeta
                camMeta?.setDisplayName(" ")
                cam.itemMeta = camMeta
                inv.setItem(i, cam)
            }
        }

        openMenus[player] = kasaIsmi
        player.openInventory(inv)
    }

    private fun olusturItem(material: Material, isim: String): ItemStack {
        return ItemStack(material).apply {
            itemMeta = itemMeta?.apply { setDisplayName(isim) }
        }
    }

    private fun olusturYetkiItem(kasaIsmi: String): ItemStack {
        val kasa = Bobcase.kasalar[kasaIsmi]
        val izin = kasa?.permission ?: ChatColor.translateAlternateColorCodes('&', LangManager.msg("no_permission_assigned"))
        val item = ItemStack(Material.KNOWLEDGE_BOOK)
        val meta = item.itemMeta
        meta?.setDisplayName(ChatColor.translateAlternateColorCodes('&', LangManager.msg("permission_setting_title")))
        meta?.lore = listOf(
            ChatColor.translateAlternateColorCodes('&', LangManager.msg("permission_given")),
            "${ChatColor.AQUA}$izin"
        )
        item.itemMeta = meta
        return item
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val kasaIsmi = openMenus[player] ?: return
        val title = ChatColor.stripColor(event.view.title) ?: return
        if (title != ChatColor.stripColor(KASA_AYAR_MENU_BASLIK)) return

        event.isCancelled = true

        if (event.rawSlot == 4) {
            if (event.click.isLeftClick) {
                player.closeInventory()
                PermAyarListener.izinBeklemeyeAl(player, kasaIsmi)
            } else if (event.click.isRightClick) {
                val kasa = Bobcase.kasalar[kasaIsmi]
                if (kasa != null) {
                    kasa.permission = null
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', LangManager.msg("permission_removed")))
                    player.closeInventory()
                    Bukkit.getScheduler().runTaskLater(Bobcase.instance, Runnable {
                        ac(player, kasaIsmi)
                    }, 1L)
                }
            }
            return
        }

        when (event.rawSlot) {
            10 -> KasaEsyaEkleMenu.ac(player, kasaIsmi)
            12 -> {
                player.closeInventory()
                DusurmeAyarMenu.ac(player, kasaIsmi)
            }
            14 -> {
                player.closeInventory()
                KasaItemMenu.ac(player, kasaIsmi)
            }
            8 -> {
                if (Bobcase.kasalar.remove(kasaIsmi) != null) {
                    KasaDAO.kasaSil(kasaIsmi)
                    player.sendMessage(
                        ChatColor.translateAlternateColorCodes('&',
                            LangManager.msg("crate_deleted").replace("{name}", kasaIsmi)
                        )
                    )
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', LangManager.msg("crate_not_found")))
                }
                openMenus.remove(player)
                player.closeInventory()
            }
            26 -> {
                player.closeInventory()
                BobcaseMenu.openMenu(player)
            }
            22 -> {
                player.closeInventory()
                oyuncuyaAnimasyonSecimMenusuAc(player, kasaIsmi)
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        openMenus.remove(player)
    }
}
