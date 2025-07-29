package me.bilolib.bobcase

import org.bukkit.ChatColor
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.persistence.PersistentDataType

object BobCaseEvent : Listener {

    @EventHandler
    fun onMobDeath(event: EntityDeathEvent) {
        val entity = event.entity
        val killer = entity.killer ?: return

        val mobAdi = entity.type.name
        val eslesenKasa = Bobcase.kasalar.values.find { kasa ->
            mobAdi in kasa.dusurmeMobList
        } ?: return

        val sans = eslesenKasa.dusurmeMobSanslari[mobAdi] ?: 0.0
        if (Math.random() > sans) return

        val kasaItem = eslesenKasa.gorunumItem.clone()
        val meta = kasaItem.itemMeta ?: return

        val renkliIsim = ChatColor.translateAlternateColorCodes('&', eslesenKasa.renkliIsim)
        meta.setDisplayName(renkliIsim)
        meta.lore = listOf(ChatColor.translateAlternateColorCodes('&', LangManager.msg("case_item_open_lore")))
            .map { ChatColor.translateAlternateColorCodes('&', it) }


        val key = NamespacedKey(Bobcase.instance, "kasa")
        meta.persistentDataContainer.set(key, PersistentDataType.STRING, eslesenKasa.temizIsim)

        kasaItem.itemMeta = meta

        killer.world.dropItemNaturally(entity.location, kasaItem)
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val blockType = event.block.type.name
        val eslesenKasa = Bobcase.kasalar.values.find { kasa ->
            blockType in kasa.dusurmeBlockList
        } ?: return

        val sans = eslesenKasa.dusurmeBlockSanslari[blockType] ?: 0.0
        if (Math.random() > sans) return

        val kasaItem = eslesenKasa.gorunumItem.clone()
        val meta = kasaItem.itemMeta ?: return

        val renkliIsim = ChatColor.translateAlternateColorCodes('&', eslesenKasa.renkliIsim)
        meta.setDisplayName(renkliIsim)
        meta.lore = listOf(ChatColor.translateAlternateColorCodes('&', LangManager.msg("case_item_open_lore")))
            .map { ChatColor.translateAlternateColorCodes('&', it) }

        val key = NamespacedKey(Bobcase.instance, "kasa")
        meta.persistentDataContainer.set(key, PersistentDataType.STRING, eslesenKasa.temizIsim)

        kasaItem.itemMeta = meta

        event.block.world.dropItemNaturally(event.block.location, kasaItem)
    }

}
