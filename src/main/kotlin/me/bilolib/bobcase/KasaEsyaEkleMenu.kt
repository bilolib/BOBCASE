package me.bilolib.bobcase


import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object KasaEsyaEkleMenu : Listener {

    private val aktifMenuler = mutableMapOf<Player, String>() // Oyuncu → Kasa ismi

    private val dekorSlots = 45..52

    val ESYA_EKLE_MENU: String
        get() {
            val secilenChatRenk = GenelAyarlar.renkler.random()
            // LangManager'dan menü başlığını alıp, renk kodlarını çeviriyoruz
            return secilenChatRenk.toString() + ChatColor.translateAlternateColorCodes('&', LangManager.msg("items_menu_title"))
        }

    fun ac(player: Player, kasaIsmi: String) {
        val replacedTitle = ESYA_EKLE_MENU
        val inv: Inventory = Bukkit.createInventory(null, 54, replacedTitle)

        // Eski eşyaları yerleştir
        val kasa = Bobcase.kasalar[kasaIsmi]
        if (kasa != null) {
            kasa.esyaListesi.forEachIndexed { index, kasaEsya ->
                if (index < 45) { // Yalnızca 0–44 arası eşya için
                    val itemWithLore = kasaEsya.item.clone().setSansLore(kasaEsya.sans)
                    inv.setItem(index, itemWithLore)
                }
            }
        }

        // 45–52: Dekoratif camları koy (siyah cam değil, renkli camlar)
        val camRenkleri = MenuRenkAyarMenu.getCamlar(player)
        for (i in dekorSlots) {
            val cam = ItemStack(camRenkleri.random()).apply {
                itemMeta = itemMeta?.apply { setDisplayName(" ") }
            }
            inv.setItem(i, cam)
        }

        // Geri butonu (53)
        val geriItem = ItemStack(Material.ARROW).apply {
            val meta = itemMeta
            val geriIsim = ChatColor.translateAlternateColorCodes('&', LangManager.msg("save_back_button"))
            meta?.setDisplayName(geriIsim)
            itemMeta = meta
        }
        inv.setItem(53, geriItem)

        val bilgi = ItemStack(Material.PAPER).apply {
            val meta = itemMeta
            val bilgiIsim = ChatColor.translateAlternateColorCodes('&', LangManager.msg("info_title"))
            meta?.setDisplayName(bilgiIsim)
            meta?.lore = listOf(
                ChatColor.translateAlternateColorCodes('&', LangManager.msg("chance_info.line1")),
                ChatColor.translateAlternateColorCodes('&', LangManager.msg("chance_info.line2"))
            )
            itemMeta = meta
        }
        inv.setItem(45, bilgi)

        aktifMenuler[player] = kasaIsmi
        player.openInventory(inv)
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val title = ChatColor.stripColor(event.view.title) ?: return
        if (title != ChatColor.stripColor(ESYA_EKLE_MENU)) return

        val kasaIsmi = aktifMenuler.remove(player) ?: return
        val inventory = event.inventory

        val itemler = inventory.contents
            .filterIndexed { i, it -> i in 0..44 && it != null && it.type.isItem }
            .map { it!! }

        val kasa = Bobcase.kasalar[kasaIsmi]
        if (kasa != null) {
            val yeniEsyaListesi = mutableListOf<KasaEsya>()
            itemler.forEachIndexed { index, item ->
                val eskiSans = kasa.esyaListesi.getOrNull(index)?.sans ?: 1.0
                yeniEsyaListesi.add(KasaEsya(item, eskiSans))
            }
            kasa.esyaListesi = yeniEsyaListesi
            player.sendMessage(LangManager.msg("items_saved_successfully").replace("{count}", yeniEsyaListesi.size.toString()))
            KasaDAO.kasaKaydet(kasa)
        } else {
            player.sendMessage(LangManager.msg("crate_not_found_save_failed"))

        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val title = ChatColor.stripColor(event.view.title)
        if (title != ChatColor.stripColor(ESYA_EKLE_MENU)) return

        val slot = event.rawSlot
        val clickedItem = event.currentItem ?: return
        val kasaIsmi = aktifMenuler[player] ?: return

        // Geri butonuna tıklandıysa
        if (slot == 53 && clickedItem.type == Material.ARROW) {
            event.isCancelled = true
            player.closeInventory()
            KasaAyarMenu.ac(player, kasaIsmi)
            return
        }

        // Dekor camlara tıklama engeli
        if (slot in dekorSlots) {
            event.isCancelled = true
            return
        }

        // Sağ tık ile şans ayarı
        if (event.click == ClickType.RIGHT && slot in 0..44) {
            val kasa = Bobcase.kasalar[kasaIsmi] ?: return
            if (slot >= kasa.esyaListesi.size) {
                player.sendMessage(LangManager.msg("must_save_before_chance"))
                return
            }

            event.isCancelled = true
            player.closeInventory()
            player.sendMessage(LangManager.msg("chance_input_instruction"))
            SansAyarListener.beklemeyeAl(player, kasaIsmi, slot)
        }
    }

    // ItemStack'e lore eklemek için extension fonksiyon (eski şans satırını temizler)
    private fun ItemStack.setSansLore(sans: Double): ItemStack {
        val meta = this.itemMeta ?: return this
        val sansYuzde = (sans * 100).coerceIn(0.0, 100.0)

        val sansPrefix = LangManager.msg("chance_prefix_raw")
        val yeniLore = (meta.lore ?: listOf())
            .filterNot { ChatColor.stripColor(it)?.startsWith(sansPrefix) == true }
            .toMutableList()

        val chanceLine = LangManager.msg("chance_line").replace("%chance%", "%.2f".format(sansYuzde))
        yeniLore.add(ChatColor.GRAY.toString() + chanceLine)
        meta.lore = yeniLore
        this.itemMeta = meta
        return this
    }

}
