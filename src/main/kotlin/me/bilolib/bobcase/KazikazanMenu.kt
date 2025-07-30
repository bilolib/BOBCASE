package me.bilolib.bobcase.menu

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
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

object KazikazanMenu : Listener {

    private const val KAZANMA_LIMIT = 2
    private const val MAKS_HAK = 3

    private val aktifMenuler = mutableMapOf<UUID, Inventory>()
    private val tiklananSlotlar = mutableMapOf<UUID, MutableSet<Int>>()
    private val odulMap = mutableMapOf<UUID, MutableMap<Int, ItemStack>>()
    private val acimHakki = mutableMapOf<UUID, Int>()
    private val kazandiMi = mutableMapOf<UUID, Boolean>()
    private val sesTasklari = mutableMapOf<UUID, BukkitRunnable>()

    val kazikazanBaslik: String
        get() {
            val renk = GenelAyarlar.renkler.random()
            return "$renk${LangManager.msg("scratch_case_title")}"
        }

    fun ac(player: Player, kasa: Kasa) {
        val inv = Bukkit.createInventory(null, 27, kazikazanBaslik)
        val oduller = kasa.esyaListesi.map { it.item.clone().temizSansLore() }
        if (oduller.isEmpty()) return

        val slotOdul = mutableMapOf<Int, ItemStack>()
        for (slot in 0 until 27) {
            val item = oduller.random().clone()
            item.itemMeta?.persistentDataContainer?.set(
                NamespacedKey(Bobcase.instance, "kazikazan_odul"),
                PersistentDataType.STRING,
                item.type.name
            )
            slotOdul[slot] = item
            inv.setItem(slot, kapaliCam())
        }

        aktifMenuler[player.uniqueId] = inv
        odulMap[player.uniqueId] = slotOdul
        tiklananSlotlar[player.uniqueId] = mutableSetOf()
        acimHakki[player.uniqueId] = 0
        kazandiMi[player.uniqueId] = false

        player.openInventory(inv)
    }

    private fun kapaliCam(): ItemStack {
        return ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(ChatColor.translateAlternateColorCodes('&', LangManager.msg("scratch_to_win")))
            }
        }
    }

    @EventHandler
    fun onClick(e: InventoryClickEvent) {
        val p = e.whoClicked as? Player ?: return
        val uuid = p.uniqueId
        val rawTitle = ChatColor.stripColor(e.view.title) ?: return
        val expectedTitle = ChatColor.stripColor(LangManager.msg("scratch_case_title")) ?: return
        if (rawTitle != expectedTitle) return

        e.isCancelled = true

        if (kazandiMi[uuid] == true) return
        if ((acimHakki[uuid] ?: 0) >= MAKS_HAK) return

        val slot = e.slot
        if (slot !in 0..26 || tiklananSlotlar[uuid]?.contains(slot) == true) return

        val odul = odulMap[uuid]?.get(slot)?.clone() ?: return

        p.playSound(p.location, Sound.BLOCK_LEVER_CLICK, 1f, 1f)

        e.inventory.setItem(slot, odul)
        tiklananSlotlar[uuid]?.add(slot)
        acimHakki[uuid] = (acimHakki[uuid] ?: 0) + 1

        val gorunen = tiklananSlotlar[uuid]!!
            .mapNotNull { odulMap[uuid]?.get(it)?.type }

        val sayim = gorunen.groupingBy { it }.eachCount()
        val kazananMaterial = sayim.entries.firstOrNull { it.value >= KAZANMA_LIMIT }?.key

        if (kazananMaterial != null) {
            kazandiMi[uuid] = true

            val kazananSlot = tiklananSlotlar[uuid]?.firstOrNull {
                odulMap[uuid]?.get(it)?.type == kazananMaterial
            }

            val kazandigiItem = kazananSlot?.let { odulMap[uuid]?.get(it)?.clone()?.temizSansLore() } ?: return

            // Sesli kutlama
            val task = object : BukkitRunnable() {
                override fun run() {
                    p.playSound(p.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f)
                }
            }
            task.runTaskTimer(Bobcase.instance, 0L, 5L)
            sesTasklari[uuid] = task

            // Menüyü kapat (ödül verme işlemi onClose'ta)
            Bukkit.getScheduler().runTaskLater(Bobcase.instance, Runnable {
                p.closeInventory()
            }, 30L)
        } else if ((acimHakki[uuid] ?: 0) >= MAKS_HAK) {
            Bukkit.getScheduler().runTaskLater(Bobcase.instance, Runnable {
                p.closeInventory()
            }, 30L)
        }
    }

    @EventHandler
    fun onClose(e: InventoryCloseEvent) {
        val player = e.player as? Player ?: return
        val uuid = player.uniqueId

        val rawTitle = ChatColor.stripColor(e.view.title) ?: return
        val expectedTitle = ChatColor.stripColor(LangManager.msg("scratch_case_title")) ?: return
        if (rawTitle != expectedTitle) return

        val inv = aktifMenuler[uuid] ?: return
        if (e.inventory != inv) return

        KasaAcmaListener.setSonAcilis(player, System.currentTimeMillis())

        if (kazandiMi[uuid] == true) {
            val kazananMaterial = tiklananSlotlar[uuid]
                ?.mapNotNull { odulMap[uuid]?.get(it)?.type }
                ?.groupingBy { it }?.eachCount()
                ?.entries?.firstOrNull { it.value >= KAZANMA_LIMIT }?.key

            val kazananSlot = tiklananSlotlar[uuid]?.firstOrNull {
                odulMap[uuid]?.get(it)?.type == kazananMaterial
            }

            val kazananItem = kazananSlot?.let { odulMap[uuid]?.get(it)?.clone()?.temizSansLore() }

            kazananItem?.let {
                if (player.inventory.firstEmpty() != -1) {
                    player.inventory.addItem(it)
                } else {
                    player.world.dropItemNaturally(player.location, it)
                }
            }
        }

        // Eğer erken kapattıysa tekrar aç
        if (kazandiMi[uuid] == false && (acimHakki[uuid] ?: 0) < MAKS_HAK) {
            Bukkit.getScheduler().runTaskLater(Bobcase.instance, Runnable {
                player.openInventory(inv)
            }, 0L)
            return
        }

        // Temizleme
        aktifMenuler.remove(uuid)
        odulMap.remove(uuid)
        tiklananSlotlar.remove(uuid)
        acimHakki.remove(uuid)
        kazandiMi.remove(uuid)
        sesTasklari.remove(uuid)?.cancel()
    }

    fun ItemStack.temizSansLore(): ItemStack {
        val meta = this.itemMeta ?: return this
        val sansKelimesi = LangManager.msg("chance_prefix_raw")
        meta.lore = meta.lore?.filterNot {
            ChatColor.stripColor(it)?.contains(sansKelimesi, ignoreCase = true) == true
        }
        this.itemMeta = meta
        return this
    }
}
