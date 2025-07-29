package me.bilolib.bobcase

import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

object CsgoAnimasyon : Listener {
    private val bekleyenler = mutableSetOf<UUID>()
    private val acikMenuler = mutableMapOf<Player, Inventory>()
    private val acilanKasaEsyalari = mutableMapOf<Player, List<ItemStack>>()
    private val animasyonBittiMi = mutableMapOf<Player, Boolean>()

    fun ac(player: Player, kasa: Kasa) {
        val esyalar = kasa.esyaListesi.map { it.item.clone().temizSansLore() }

        val secilenChatRenk = GenelAyarlar.renkler.random()
        val rawTitle = LangManager.msg("opening_title")
        val title = "$secilenChatRenk${ChatColor.translateAlternateColorCodes('&', rawTitle)}"
        val inv = Bukkit.createInventory(null, 27, title)

        val siyahCam = ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta?.apply { setDisplayName(" ") }
        }

        for (i in 0 until 27) inv.setItem(i, siyahCam)

        val renkliCamlar = listOf(
            Material.LIME_STAINED_GLASS_PANE
        )
        val camRenk = renkliCamlar.random()
        val tekRenkCam = ItemStack(camRenk).apply {
            itemMeta = itemMeta?.apply { setDisplayName(" ") }
        }

        inv.setItem(4, tekRenkCam)
        inv.setItem(22, tekRenkCam)

        for (i in 10..16) {
            inv.setItem(i, esyalar.random())
        }

        acikMenuler[player] = inv
        acilanKasaEsyalari[player] = esyalar
        animasyonBittiMi[player] = false

        player.openInventory(inv)
        baslatAnimasyon(player, 0)
    }

    private fun baslatAnimasyon(player: Player, tick: Int) {
        val inv = acikMenuler[player] ?: return
        val maxTick = 50

        if (tick >= maxTick) {
            animasyonBittiMi[player] = true
            gosterKazanan(player)
            return
        }

        for (i in 16 downTo 11) {
            val oncekiItem = inv.getItem(i - 1)
            inv.setItem(i, oncekiItem?.clone()?.temizSansLore())
        }
        inv.setItem(10, rastgeleEsya(player))
        player.playSound(player.location, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1f, 1f)

        val delay = when {
            tick < 20 -> 2L
            tick < 35 -> 3L
            else -> 5L
        }

        object : BukkitRunnable() {
            override fun run() {
                baslatAnimasyon(player, tick + 1)
            }
        }.runTaskLater(Bobcase.instance, delay)
    }

    private fun gosterKazanan(player: Player) {
        val inv = acikMenuler[player] ?: return
        val kazanilan = inv.getItem(13)?.clone()?.temizSansLore() ?: rastgeleEsya(player).temizSansLore()
        inv.setItem(13, kazanilan)

        val camRenkleri = listOf(
            Material.LIME_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE,
            Material.ORANGE_STAINED_GLASS_PANE,
            Material.RED_STAINED_GLASS_PANE
        )

        var index = 0
        object : BukkitRunnable() {
            override fun run() {
                if (!player.isOnline) {
                    cancel()
                    return
                }

                val renk = camRenkleri[index % camRenkleri.size]
                val camItem = ItemStack(renk).apply {
                    itemMeta = itemMeta?.apply { setDisplayName(" ") }
                }

                inv.setItem(4, camItem)
                inv.setItem(22, camItem)

                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f)

                if (++index >= 6) {
                    player.closeInventory()
                    temizle(player)
                    cancel()
                }
            }
        }.runTaskTimer(Bobcase.instance, 0L, 5L)
    }

    private fun rastgeleEsya(player: Player): ItemStack {
        val esyalar = acilanKasaEsyalari[player] ?: return ItemStack(Material.BARRIER)

        val toplamSans = esyalar.sumOf { item ->
            // Sans değerini lore'dan çıkarmayı istersen, onu da buraya koyabilirsin.
            1.0 // Şu an her eşyanın sansı eşit
        }
        val rastgele = Math.random() * toplamSans

        // Basit random seç, eğer sansı ekleyeceksen bunu değiştir
        return esyalar.random().clone().temizSansLore()
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        if (!acikMenuler.containsKey(player)) return

        val inv = acikMenuler[player] ?: return
        val animasyonBitti = animasyonBittiMi[player] ?: false

        val esyaVerilecek = if (animasyonBitti) {
            inv.getItem(13)?.clone()?.temizSansLore() ?: rastgeleEsya(player)
        } else {
            acilanKasaEsyalari[player]?.random()?.clone()?.temizSansLore() ?: ItemStack(Material.BARRIER)
        }

        if (player.inventory.firstEmpty() != -1) {
            player.inventory.addItem(esyaVerilecek)
            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        } else {
            player.world.dropItemNaturally(player.location, esyaVerilecek)
            player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1f, 1f)
        }

        bekleyenler.add(player.uniqueId)
        temizle(player)
    }
    fun ItemStack.temizSansLore(): ItemStack {
        val itemMeta = this.itemMeta ?: return this
        val lore = itemMeta.lore?.toMutableList() ?: return this

        // "Şans: %" veya benzeri bir şey içeren satırları sil
        val sansKelimesi = LangManager.msg("chance_prefix_raw")
        val temizLore = lore.filterNot { ChatColor.stripColor(it)?.contains(sansKelimesi, ignoreCase = true) == true }


        itemMeta.lore = temizLore
        this.itemMeta = itemMeta
        return this
    }
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (acikMenuler[player] == event.view.topInventory) {
            event.isCancelled = true
        }
    }

    private fun temizle(player: Player) {
        acikMenuler.remove(player)
        acilanKasaEsyalari.remove(player)
        animasyonBittiMi.remove(player)
    }
}
