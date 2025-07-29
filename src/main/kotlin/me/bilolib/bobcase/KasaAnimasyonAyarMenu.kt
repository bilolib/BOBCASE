package me.bilolib.bobcase.menu

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import me.bilolib.bobcase.model.AnimasyonTuru
import me.bilolib.bobcase.Bobcase
import me.bilolib.bobcase.GenelAyarlar
import me.bilolib.bobcase.KasaAyarMenu
import me.bilolib.bobcase.KasaDAO
import me.bilolib.bobcase.LangManager
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

object KasaAnimasyonAyarMenu : Listener {

    private val aktifMenuler = mutableMapOf<Player, String>()


    val stylemanu: String
        get() {
            val secilenChatRenk = GenelAyarlar.renkler.random()
            return "${secilenChatRenk}${LangManager.msg("style_select_title")}"
        }

    fun oyuncuyaAnimasyonSecimMenusuAc(player: Player, kasaIsmi: String) {
        val kasa = Bobcase.kasalar[kasaIsmi] ?: return
        val menu = Bukkit.createInventory(null, 9, stylemanu)

        aktifMenuler[player] = kasaIsmi

        val cam = ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta?.apply { setDisplayName(" ") }
        }
        repeat(9) { i -> menu.setItem(i, cam) }

        fun animasyonItem(tur: AnimasyonTuru, isim: String): ItemStack {
            val mat = if (kasa.animasyonTuru == tur) Material.LIME_DYE else Material.GRAY_DYE
            return ItemStack(mat).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName("§b$isim")
                    val lore = mutableListOf(
                        ChatColor.translateAlternateColorCodes('&', LangManager.msg("select_this_style"))
                    )
                    if (kasa.animasyonTuru == tur) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', LangManager.msg("selected")))
                    }
                    this.lore = lore
                }
            }
        }

        menu.setItem(0, animasyonItem(AnimasyonTuru.CSGO, LangManager.msg("animation_type.csgo")))
        menu.setItem(1, animasyonItem(AnimasyonTuru.RULET, LangManager.msg("animation_type.spin")))
        menu.setItem(2, animasyonItem(AnimasyonTuru.KAZIKAZAN, LangManager.msg("animation_type.Scratch")))

        val geri = ItemStack(Material.ARROW).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(ChatColor.translateAlternateColorCodes('&', LangManager.msg("back_button")))
            }
        }
        menu.setItem(8, geri)

        player.openInventory(menu)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val kasaIsmi = aktifMenuler[player] ?: return
        val kasa = Bobcase.kasalar[kasaIsmi] ?: return

        val title = ChatColor.stripColor(event.view.title) ?: return
        if (title != ChatColor.stripColor(stylemanu)) return

        event.isCancelled = true

        val clicked = event.currentItem ?: return
        val meta = clicked.itemMeta ?: return
        val displayName = ChatColor.stripColor(meta.displayName ?: "") ?: return

        val csgoText = ChatColor.stripColor(LangManager.msg("animation_type.csgo"))
        val ruletText = ChatColor.stripColor(LangManager.msg("animation_type.spin"))
        val scratchText = ChatColor.stripColor(LangManager.msg("animation_type.Scratch"))
        val backText = ChatColor.stripColor(LangManager.msg("back_button"))

        when (displayName) {
            csgoText -> {
                kasa.animasyonTuru = AnimasyonTuru.CSGO
                KasaDAO.kasaKaydet(kasa)
                oyuncuyaAnimasyonSecimMenusuAc(player, kasaIsmi)
            }
            ruletText -> {
                kasa.animasyonTuru = AnimasyonTuru.RULET
                KasaDAO.kasaKaydet(kasa)
                oyuncuyaAnimasyonSecimMenusuAc(player, kasaIsmi)
            }
            scratchText -> {
                kasa.animasyonTuru = AnimasyonTuru.KAZIKAZAN
                KasaDAO.kasaKaydet(kasa)
                oyuncuyaAnimasyonSecimMenusuAc(player, kasaIsmi)
            }
            backText -> {
                aktifMenuler.remove(player)
                player.closeInventory()
                Bukkit.getScheduler().runTaskLater(Bobcase.instance, Runnable {
                    KasaAyarMenu.ac(player, kasaIsmi)
                }, 2L)
            }
        }
    }
}
