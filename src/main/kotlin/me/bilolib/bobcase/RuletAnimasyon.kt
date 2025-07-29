import me.bilolib.bobcase.Bobcase
import me.bilolib.bobcase.GenelAyarlar
import me.bilolib.bobcase.Kasa
import me.bilolib.bobcase.KasaAcmaListener
import me.bilolib.bobcase.LangManager
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.*
import kotlin.collections.set

object RuletKasa : Listener {

    private val ruletSlotlari = listOf(11, 12, 13, 14, 15, 24, 33, 42, 41, 40, 39, 38, 29, 20)
    private val aktifMenuler = mutableMapOf<Player, Inventory>()
    private val oyuncuEsyalari = mutableMapOf<Player, List<ItemStack>>()
    private val animasyonBitti = mutableMapOf<Player, Boolean>()
    private val slotIndex = mutableMapOf<Player, Int>()
    private val slotItemMap = mutableMapOf<Player, List<ItemStack>>()
    private val sesTekrarlamaGorev = mutableMapOf<Player, BukkitTask>()
    private val renkDegistirmeGorev = mutableMapOf<Player, BukkitTask>()


    val ruletcase: String
        get() {
            val secilenChatRenk = GenelAyarlar.renkler.random()
            return "${secilenChatRenk}${LangManager.msg("opening_title")}"
        }

    fun ac(player: Player, kasa: Kasa, plugin: JavaPlugin) {
        val plugin = Bobcase.instance
        val inv = Bukkit.createInventory(null, 54, ruletcase)

        val esyalar = kasa.esyaListesi.map { it.item }

        aktifMenuler[player] = inv
        oyuncuEsyalari[player] = esyalar
        animasyonBitti[player] = false
        slotIndex[player] = 0

        val siyahCam = ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta?.apply { setDisplayName(" ") }
        }
        for (i in 0 until 54) inv.setItem(i, siyahCam)

        val esyaListesi = mutableListOf<ItemStack>()
        repeat(ruletSlotlari.size) {
            val item = esyalar.random().clone()
            item.itemMeta = item.itemMeta?.apply {
                val chancePrefix = ChatColor.translateAlternateColorCodes('&', LangManager.msg("chance_prefix_raw"))

// lore'dan şans içeren satırları çıkar
                lore = lore?.filterNot { it.contains(chancePrefix, ignoreCase = true) }

            }
            esyaListesi.add(item)
        }
        slotItemMap[player] = esyaListesi

        ruletSlotlari.forEachIndexed { i, slot ->
            inv.setItem(slot, esyaListesi[i])
        }

        player.openInventory(inv)

        object : BukkitRunnable() {
            var tick = 0
            val maxTick = 40 + Random().nextInt(20)
            var delayCounter = 0L
            var delay = 1L
            val yavaslamaBaslangicTick = maxTick * (0.4 + Math.random() * 0.3)

            override fun run() {
                if (!player.isOnline || !aktifMenuler.containsKey(player)) {
                    cancel()
                    return
                }

                delayCounter++
                if (delayCounter < delay) return
                delayCounter = 0

                val index = slotIndex[player] ?: 0
                val currentSlot = ruletSlotlari[index % ruletSlotlari.size]

                // Önceki yeşil camı orijinal eşya ile değiştir
                ruletSlotlari.forEachIndexed { i, slot ->
                    val item = inv.getItem(slot)

                    val spinningText = ChatColor.translateAlternateColorCodes('&', LangManager.msg("spinning_glass_display"))

                    if (item?.type?.name?.endsWith("_STAINED_GLASS_PANE") == true && item.itemMeta?.displayName == spinningText){
                        val orjinalItem = slotItemMap[player]?.get(i)
                        if (orjinalItem != null) inv.setItem(slot, orjinalItem)
                    }
                }

                // Her animasyon başında farklı renk seç
                val camRenkleri = Material.values()
                    .filter { it.name.endsWith("_STAINED_GLASS_PANE") && it != Material.BLACK_STAINED_GLASS_PANE }
                val renk = Material.LIME_STAINED_GLASS_PANE

                val camItem = ItemStack(renk).apply {
                    val donuyorText = ChatColor.translateAlternateColorCodes('&', LangManager.msg("spinning_glass_display"))
                    itemMeta = itemMeta?.apply { setDisplayName(donuyorText) }

                }
                inv.setItem(currentSlot, camItem)

                player.playSound(player.location, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0f, 1.0f)

                slotIndex[player] = (index + 1) % ruletSlotlari.size
                tick++

                if (tick > yavaslamaBaslangicTick && delay < 6L) {
                    if (Math.random() < 0.6) delay++
                }

                if (tick >= maxTick) {
                    animasyonBitti[player] = true
                    cancel()

                    val kazananIndex = (slotIndex[player]?.minus(1) ?: ruletSlotlari.size - 1).let {
                        if (it < 0) ruletSlotlari.size - 1 else it
                    }
                    val kazananSlot = ruletSlotlari[kazananIndex]

                    val kazanilanItem = slotItemMap[player]?.get(kazananIndex)?.clone()?.apply {
                        itemMeta = itemMeta?.apply {
                            val chancePrefix = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', LangManager.msg("chance_prefix_raw")))?.removeSuffix(":")?.trim() ?: ""
                            lore = lore?.filterNot {
                                ChatColor.stripColor(it)?.startsWith(chancePrefix) == true
                            }

                        }
                    }
                    if (kazanilanItem != null) {
                        inv.setItem(kazananSlot, kazanilanItem)
                    }

                    player.updateInventory()

                    sesTekrarlamaGorev[player]?.cancel()
                    val sesTask = object : BukkitRunnable() {
                        override fun run() {
                            if (!player.isOnline || !aktifMenuler.containsKey(player)) {
                                cancel()
                                sesTekrarlamaGorev.remove(player)
                                return
                            }
                            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f)
                        }
                    }.runTaskTimer(plugin, 0L, 5L)
                    sesTekrarlamaGorev[player] = sesTask

                    renkDegistirmeGorev[player]?.cancel()
                    val renkTask = object : BukkitRunnable() {
                        val camRenkleri = Material.values()
                            .filter {
                                it.name.endsWith("_STAINED_GLASS_PANE")
                                        && it != Material.LIME_STAINED_GLASS_PANE
                                        && it != Material.BLACK_STAINED_GLASS_PANE
                            }
                        var renkIndex = 0
                        var sure = 0
                        val maxSure = 30

                        override fun run() {
                            if (!player.isOnline || !aktifMenuler.containsKey(player)) {
                                cancel()
                                renkDegistirmeGorev.remove(player)
                                return
                            }

                            ruletSlotlari.forEach { slot ->
                                if (slot == kazananSlot) return@forEach
                                val renk = camRenkleri[renkIndex % camRenkleri.size]
                                val cam = ItemStack(renk).apply {
                                    itemMeta = itemMeta?.apply { setDisplayName(" ") }
                                }
                                inv.setItem(slot, cam)
                            }
                            player.updateInventory()

                            renkIndex++
                            sure++
                            if (sure >= maxSure) {
                                cancel()
                                renkDegistirmeGorev.remove(player)
                                player.closeInventory()
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 1L)
                    renkDegistirmeGorev[player] = renkTask
                }
            }
        }.runTaskTimer(plugin, 0L, 1L)
    }

    @EventHandler
    fun onClick(e: InventoryClickEvent) {
        val player = e.whoClicked as? Player ?: return
        if (aktifMenuler[player] != null && e.view.topInventory == aktifMenuler[player]) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onClose(e: InventoryCloseEvent) {
        val player = e.player as? Player ?: return
        val inv = aktifMenuler[player] ?: return
        if (e.inventory != inv) return
        KasaAcmaListener.setSonAcilis(player, System.currentTimeMillis())

        sesTekrarlamaGorev[player]?.cancel()
        sesTekrarlamaGorev.remove(player)
        renkDegistirmeGorev[player]?.cancel()
        renkDegistirmeGorev.remove(player)

        val index = ((slotIndex[player]?.minus(1) ?: 0) + ruletSlotlari.size) % ruletSlotlari.size
        val kazanilanItem = slotItemMap[player]?.get(index)?.clone()?.apply {
            itemMeta = itemMeta?.apply {
                val chancePrefix = ChatColor.translateAlternateColorCodes('&', LangManager.msg("chance_prefix_raw"))

                lore = lore?.filterNot { it.contains(chancePrefix, ignoreCase = true) }

            }
        }

        if (animasyonBitti[player] == true && kazanilanItem != null) {
            if (player.inventory.firstEmpty() != -1) {
                player.inventory.addItem(kazanilanItem)
            } else {
                player.world.dropItemNaturally(player.location, kazanilanItem)
            }
            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        } else {
            val fallback = oyuncuEsyalari[player]?.random()?.clone()?.apply {
                itemMeta = itemMeta?.apply {
                    val chancePrefix = ChatColor.translateAlternateColorCodes('&', LangManager.msg("chance_prefix_raw"))
                    lore = lore?.filterNot { it.contains(chancePrefix, ignoreCase = true) }

                }
            }
            if (fallback != null) {
                if (player.inventory.firstEmpty() != -1) {
                    player.inventory.addItem(fallback)
                } else {
                    player.world.dropItemNaturally(player.location, fallback)
                }
                player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
            }
        }

        aktifMenuler.remove(player)
        oyuncuEsyalari.remove(player)
        animasyonBitti.remove(player)
        slotIndex.remove(player)
        slotItemMap.remove(player)
    }

}
