package me.bilolib.bobcase


import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack

object KasaItemMenu : Listener {

    private val openMenus = mutableMapOf<Player, String>() // Player -> kasaIsmi

    val ITEM_STYLE: String
        get() {
            val secilenChatRenk = GenelAyarlar.renkler.random()
            return "${secilenChatRenk}${LangManager.msg("case_appearance_title")}"
        }

    fun ac(player: Player, kasaIsmi: String) {
        val inv = Bukkit.createInventory(null, 9, ITEM_STYLE)

        val kasa = Bobcase.kasalar[kasaIsmi]

        val gorunumItem = kasa?.gorunumItem ?: ItemStack(Material.CHEST).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(ChatColor.translateAlternateColorCodes('&', LangManager.msg("default_case_appearance")))

            }
        }




        for (slot in 0 until inv.size) {
            when (slot) {
                3 -> inv.setItem(slot, gorunumItem) // Görünüm ayarlama slotu 3
                5 -> { // Geri butonu slotu 5
                    val geriButon = ItemStack(Material.ARROW).apply {
                        itemMeta = itemMeta?.apply {
                            setDisplayName(ChatColor.translateAlternateColorCodes('&', LangManager.msg("back_button")))

                        }
                    }
                    inv.setItem(slot, geriButon)
                }
                else -> {
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
                }
            }
        }

        openMenus[player] = kasaIsmi
        player.openInventory(inv)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val kasaIsmi = openMenus[player] ?: return
        val title = ChatColor.stripColor(event.view.title) ?: return

        if (title != ChatColor.stripColor(ITEM_STYLE)) return

        if (event.rawSlot >= event.view.topInventory.size) return // Oyuncunun kendi envanteri = serbest bırak

        when (event.rawSlot) {
            3 -> {
                val kasa = Bobcase.kasalar[kasaIsmi] ?: return
                if (event.action.name.startsWith("PICKUP") || event.action.name.startsWith("PLACE")) {
                    Bukkit.getScheduler().runTaskLater(Bobcase.instance, Runnable {
                        val yeniItem = event.inventory.getItem(3)
                        if (yeniItem != null) {
                            if (yeniItem.amount > 1) {
                                val fazlalik = yeniItem.amount - 1
                                yeniItem.amount = 1
                                if (fazlalik > 0) {
                                    player.inventory.addItem(yeniItem.clone().apply { amount = fazlalik })
                                }
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', LangManager.msg("only_one_item_per_slot")))
                            }
                            kasa.gorunumItem = yeniItem.clone()
                            KasaDAO.kasaKaydet(kasa)
                        }
                    }, 1L)
                }
                event.isCancelled = false
            }
            5 -> {
                event.isCancelled = true
                player.closeInventory()
                KasaAyarMenu.ac(player, kasaIsmi)
            }
            else -> {
                event.isCancelled = true // Diğer tüm slotlara tıklamayı engelle
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val kasaIsmi = openMenus.remove(player) ?: return
        val title = ChatColor.stripColor(event.view.title) ?: return

        if (title != ChatColor.stripColor(ITEM_STYLE)) return

        val kasa = Bobcase.kasalar[kasaIsmi] ?: return
        val yeniItem = event.inventory.getItem(3) ?: return

        kasa.gorunumItem = yeniItem.clone()
        KasaDAO.kasaKaydet(kasa)
    }


}
