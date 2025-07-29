package me.bilolib.bobcase



import me.bilolib.bobcase.BobcaseMenu.ANA_MENU_BASLIK
import me.bilolib.bobcase.sqlite.MenuRenkDAO
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

object MenuRenkAyarMenu : Listener {

    private val renkKayitlari = mutableMapOf<UUID, List<Material>>()  // Her oyuncunun cam kayıtları
    private val aktifMenuler = mutableMapOf<UUID, String>() // Oyuncuya özel başlıklar


    val RENK_MENU_TITLE: String
        get() {
            val secilenChatRenk = GenelAyarlar.renkler.random()
            return "${secilenChatRenk}${LangManager.msg("color_settings_title")}"
        }
    fun ac(player: Player) {
        val plugin = Bobcase.instance
        val inv = Bukkit.createInventory(null, 54, RENK_MENU_TITLE)

        // Kayıtlı camları ya da default beyaz camları koy
        val camlar = getCamlar(player).ifEmpty { listOf(Material.WHITE_STAINED_GLASS_PANE) }
        for (i in 0 until 45) {
            val cam = if (i < camlar.size) ItemStack(camlar[i]) else null
            inv.setItem(i, cam)
        }

        // Alt satır siyah cam sabit
        val camRenkleri = MenuRenkAyarMenu.getCamlar(player)

// Önce 45–52 arasını özel olarak camla doldur (butonun olduğu 53 hariç)
        for (i in 45..52) {
            val cam = ItemStack(camRenkleri.random()).apply {
                itemMeta = itemMeta?.apply { setDisplayName(" ") }
            }
            inv.setItem(i, cam)
        }

        // Kaydet ve çık butonu 49. slotta
        val okItem = ItemStack(Material.ARROW).apply {
            itemMeta = itemMeta?.apply {
                val backTitle =  LangManager.msg("save_back_button")
                setDisplayName(ChatColor.translateAlternateColorCodes('&', backTitle))
            }
        }
        inv.setItem(53, okItem)

        player.openInventory(inv)
    }
    @EventHandler
    fun onTiklama(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val title = ChatColor.stripColor(event.view.title) ?: return
        if (title != ChatColor.stripColor(RENK_MENU_TITLE)) return

        val slot = event.rawSlot
        val clickedInv = event.clickedInventory
        val cursorItem = event.cursor
        val currentItem = event.currentItem

        // ✅ 1. Öncelik: OK butonuna tıklama kontrolü
        if (slot == 53 && currentItem?.type == Material.ARROW) {
            event.isCancelled = true
            kaydet(player, event.inventory)
            player.closeInventory()
            PluginAyarMenu.ac(player)
            return
        }

        // ⛔ 2. Alt satıra müdahaleyi engelle (OK harici)
        if (slot in 45..53) {
            event.isCancelled = true
            return
        }

        // ✅ 3. Üst menüde cam koyma kontrolü
        if (clickedInv == event.view.topInventory) {
            // Eğer elindeki item boşsa (camı kaldırmak istiyor) → izin ver
            if (cursorItem == null || cursorItem.type.isAir) return

            // Elinde item varsa ve bu cam değilse → engelle
            if (!cursorItem.type.name.endsWith("_STAINED_GLASS_PANE")) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onKapat(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val title = ChatColor.stripColor(event.view.title) ?: return
        if (title != ChatColor.stripColor(RENK_MENU_TITLE)) return

        kaydet(player, event.inventory) // Menü kapandığında da kaydet

        aktifMenuler.remove(player.uniqueId)
    }

    private fun kaydet(player: Player, inventory: Inventory) {
        val camlar = mutableListOf<Material>()
        for (i in 0..44) {
            val item = inventory.getItem(i)
            if (item != null && item.type.name.endsWith("_STAINED_GLASS_PANE")) {
                camlar.add(item.type)
            }
        }

        if (camlar.isNotEmpty()) {
            renkKayitlari[player.uniqueId] = camlar
            MenuRenkDAO.kaydet(player.uniqueId, camlar)
        } else {
            renkKayitlari.remove(player.uniqueId)
        }
    }

    fun getCamlar(player: Player): List<Material> {
        return renkKayitlari[player.uniqueId] ?: listOf(
            Material.WHITE_STAINED_GLASS_PANE,
            Material.RED_STAINED_GLASS_PANE,
            Material.ORANGE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE,
            Material.LIME_STAINED_GLASS_PANE,
            Material.LIGHT_BLUE_STAINED_GLASS_PANE,
            Material.MAGENTA_STAINED_GLASS_PANE,
            Material.CYAN_STAINED_GLASS_PANE
        )
    }
    fun renkKayitYukle(uuid: UUID, camlar: List<Material>) {
        renkKayitlari[uuid] = camlar
    }
}

