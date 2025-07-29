package me.bilolib.bobcase

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

object KasaOnizlemeMenu : Listener {


    val onizleme: String
        get() {
            val secilenChatRenk = GenelAyarlar.renkler.random()
            return "${secilenChatRenk}${LangManager.msg("case_contents_title")}"
        }
    fun ac(player: Player, kasaIsmi: String) {
        val kasa = Bobcase.kasalar[kasaIsmi] ?: return



        val camlar = MenuRenkAyarMenu.getCamlar(player)
        val secilenCam = camlar.random()

        val inv = Bukkit.createInventory(null, 27, onizleme)

        val cam = ItemStack(secilenCam).apply {
            itemMeta = itemMeta?.apply { setDisplayName(" ") }
        }

        // Eşyaları 0. slottan itibaren sırayla yerleştir
        kasa.esyaListesi.take(54).forEachIndexed { index, kasaEsya ->
            inv.setItem(index, kasaEsya.item.clone())
        }

        // Geri kalan boş slotlara cam yerleştir
        for (i in 0 until 54) {
            if (inv.getItem(i) == null || inv.getItem(i)?.type == Material.AIR) {
                inv.setItem(i, cam)
            }
        }

        player.openInventory(inv)
    }
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val inv = event.view.title
        val title = ChatColor.stripColor(event.view.title) ?: return

        if (title != ChatColor.stripColor(onizleme)) return
            event.isCancelled = true
        }
    }
