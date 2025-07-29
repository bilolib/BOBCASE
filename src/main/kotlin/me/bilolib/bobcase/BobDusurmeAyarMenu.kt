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

object DusurmeAyarMenu : Listener {

    private val aktifMenuler = mutableMapOf<Player, String>()

    private fun ItemStack.addOrUpdateSansLore(sans: Double): ItemStack {
        val meta = itemMeta ?: return this
        val lore = meta.lore?.toMutableList() ?: mutableListOf()

        val chanceText = LangManager.msg("chance_prefix").replace("{value}", "${(sans * 100).toInt()}")
        lore.add(ChatColor.translateAlternateColorCodes('&', chanceText))

        meta.lore = lore
        itemMeta = meta
        return this
    }

    private fun mobNameToMaterial(mobName: String): Material? {
        return when (mobName) {
            "ENDER_DRAGON" -> Material.DRAGON_EGG
            "WITHER" -> Material.WITHER_SKELETON_SKULL
            else -> Material.getMaterial("${mobName}_SPAWN_EGG")
        }
    }

    private fun materialToMobName(material: Material): String? {
        return when (material) {
            Material.DRAGON_EGG -> "ENDER_DRAGON"
            Material.WITHER_SKELETON_SKULL -> "WITHER"
            else -> {
                val name = material.name
                if (name.endsWith("_SPAWN_EGG")) name.removeSuffix("_SPAWN_EGG")
                else null
            }
        }
    }


    val secilenChatRenk = GenelAyarlar.renkler.random()

    val DROP_BASLIK: String
        get() {
            val secilenChatRenk = GenelAyarlar.renkler.random()
            return "${secilenChatRenk}${LangManager.msg("drop_menu_title")}"
        }
    fun ac(player: Player, kasaIsmi: String) {
        val inv = Bukkit.createInventory(null, 54, DROP_BASLIK)

        val camRenkleri = MenuRenkAyarMenu.getCamlar(player)

// Önce 45–52 arasını özel olarak camla doldur (butonun olduğu 53 hariç)
        for (i in 45..52) {
            val cam = ItemStack(camRenkleri.random()).apply {
                itemMeta = itemMeta?.apply { setDisplayName(" ") }
            }
            inv.setItem(i, cam)
        }

        val bilgi = ItemStack(Material.PAPER).apply {
            val meta = itemMeta
            meta?.setDisplayName(ChatColor.translateAlternateColorCodes('&', LangManager.msg("info_title")))
            meta?.lore = listOf(
                ChatColor.translateAlternateColorCodes('&', LangManager.msg("chance_info.line1")),
                ChatColor.translateAlternateColorCodes('&', LangManager.msg("chance_info.line2"))
            )
            itemMeta = meta
        }
        inv.setItem(45, bilgi)

        val geriButon = ItemStack(Material.ARROW).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(ChatColor.translateAlternateColorCodes('&', LangManager.msg("save_back_button")))
            }
        }
        inv.setItem(53, geriButon)

        val kasa = Bobcase.kasalar[kasaIsmi] ?: return

        val itemler = mutableListOf<ItemStack>()

        // Moblar için itemler ve şans lore ekleme
        kasa.dusurmeMobList.forEach { mobName ->
            mobNameToMaterial(mobName)?.let { mat ->
                val item = ItemStack(mat)
                val sans = kasa.dusurmeMobSanslari[mobName] ?: 1.0
                item.addOrUpdateSansLore(sans)
                itemler.add(item)
            }
        }

        // Bloklar için itemler ve şans lore ekleme
        kasa.dusurmeBlockList.forEach { blockName ->
            Material.getMaterial(blockName)?.let { mat ->
                val item = ItemStack(mat)
                val sans = kasa.dusurmeBlockSanslari[blockName] ?: 1.0
                item.addOrUpdateSansLore(sans)
                itemler.add(item)
            }
        }

        itemler.take(45).forEachIndexed { index, item -> inv.setItem(index, item) }

        aktifMenuler[player] = kasaIsmi
        player.openInventory(inv)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val kasaIsmi = aktifMenuler[player] ?: return
        val title = ChatColor.stripColor(event.view.title) ?: return
        if (title != ChatColor.stripColor(DROP_BASLIK)) return
        if (event.inventory != event.view.topInventory) return

        val slot = event.rawSlot
        if (slot in 45..53) event.isCancelled = true

        val currentItem = event.currentItem ?: return

        if (event.click.isRightClick && slot in 0..44) {
            event.isCancelled = true

            val currentType = currentItem.type

            val target = when {
                currentType == Material.DRAGON_EGG -> "ENDER_DRAGON"
                currentType == Material.WITHER_SKELETON_SKULL -> "WITHER"
                currentType.name.endsWith("_SPAWN_EGG") -> currentType.name.removeSuffix("_SPAWN_EGG")
                currentType.isBlock -> currentType.name // Blok ise direkt adı al
                else -> null
            }

            if (target != null) {
                SansDusurmeListener.beklemeyeAl(player, kasaIsmi, target)
                player.closeInventory()
                val msg = LangManager.msg("drop_chance_prompt").replace("{target}", target)
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg))
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', LangManager.msg("chance_cannot_set")))

            }
        }

        if (slot == 53) {
            event.isCancelled = true
            player.closeInventory()
            Bukkit.getScheduler().runTaskLater(Bobcase.instance, Runnable {
                KasaAyarMenu.ac(player, kasaIsmi)
            }, 2L)
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val kasaIsmi = aktifMenuler.remove(player) ?: return
        val title = ChatColor.stripColor(event.view.title) ?: return
        if (title != ChatColor.stripColor(DROP_BASLIK)) return

        val inv = event.inventory
        val mobList = mutableListOf<String>()
        val blockList = mutableListOf<String>()

        for (slot in 0..44) {
            val item = inv.getItem(slot) ?: continue
            if (item.type.name.endsWith("_SPAWN_EGG") || item.type == Material.DRAGON_EGG || item.type == Material.WITHER_SKELETON_SKULL) {
                materialToMobName(item.type)?.let { mobList.add(it) }
            } else if (item.type.isBlock) {
                blockList.add(item.type.name)
            }
        }

        val kasa = Bobcase.kasalar[kasaIsmi] ?: return
        kasa.dusurmeMobList = mobList
        kasa.dusurmeBlockList = blockList

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', LangManager.msg("drop_settings_saved")))
        KasaDAO.kasaKaydet(kasa)
    }
}
