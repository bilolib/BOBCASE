package me.bilolib.bobcase

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.ChatColor

object KasalarListMenu : Listener {

    private val aktifBasliklar = mutableSetOf<String>()

    val KASALAR_MENU_BASLIK: String
        get() {
            val secilenChatRenk = GenelAyarlar.renkler.random()
            return "$secilenChatRenk${LangManager.msg("menu_crates_title")}"
        }

    fun ac(player: Player) {
        val menuBaslik = KASALAR_MENU_BASLIK
        aktifBasliklar.add(menuBaslik)

        val kasalar = Bobcase.kasalar.values.toList()
        val invSize = ((kasalar.size) / 9 + 1) * 9
        val inv = Bukkit.createInventory(null, invSize, menuBaslik)

        for ((index, kasa) in kasalar.withIndex()) {
            if (index >= invSize - 1) break

            val item = kasa.gorunumItem.clone()
            val meta = item.itemMeta ?: continue

            meta.setDisplayName(kasa.renkliIsim)

            val loreSag = LangManager.msg("case_take")
            val loreSol = LangManager.msg("case_settings")

            meta.lore = listOf(loreSag, loreSol)
            item.itemMeta = meta
            inv.setItem(index, item)
        }

        // Geri butonu
        val geriItem = ItemStack(Material.ARROW).apply {
            val meta = itemMeta
            meta?.setDisplayName(LangManager.msg("back_button"))
            itemMeta = meta
        }
        inv.setItem(invSize - 1, geriItem)

        // Dekor camları
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

        player.openInventory(inv)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val gelenBaslik = event.view.title

        if (!aktifBasliklar.contains(gelenBaslik)) return

        event.isCancelled = true

        val clickedItem = event.currentItem ?: return

        if (event.rawSlot == event.inventory.size - 1 && clickedItem.type == Material.ARROW) {
            BobcaseMenu.openMenu(player)
            return
        }

        val kasaIsmi = clickedItem.itemMeta?.displayName?.removeColorCodes() ?: return
        val kasa = Bobcase.kasalar[kasaIsmi] ?: return
        val gorunumItem = kasa.gorunumItem

        if (clickedItem.type != gorunumItem.type) return

        when (event.click) {
            ClickType.LEFT -> {
                player.closeInventory()
                KasaAyarMenu.ac(player, kasaIsmi)
            }
            ClickType.RIGHT -> {
                val kasaItem = kasa.gorunumItem.clone()
                val meta = kasaItem.itemMeta ?: return
                val key = NamespacedKey(Bobcase.instance, "kasa")

                val renkliIsim = ChatColor.translateAlternateColorCodes('&', kasa.renkliIsim)
                meta.setDisplayName(renkliIsim)
                meta.lore = listOf(ChatColor.translateAlternateColorCodes('&', LangManager.msg("case_item_open_lore")))
                meta.persistentDataContainer.set(key, PersistentDataType.STRING, kasa.temizIsim)

                kasaItem.itemMeta = meta
                player.inventory.addItem(kasaItem)
            }
            else -> return
        }
    }

    private fun String.removeColorCodes(): String {
        return this.replace(Regex("§[0-9a-fk-or]", RegexOption.IGNORE_CASE), "")
    }
}
