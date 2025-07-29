

package me.bilolib.bobcase

import org.bukkit.ChatColor
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64



data class KasaEsya(
    val item: ItemStack,
    var sans: Double = 1.0 // 0.0 ile 1.0 arasında, default %100
)
fun kasaEsyaListToBase64(list: List<KasaEsya>): String {
    val baos = ByteArrayOutputStream()
    val out = BukkitObjectOutputStream(baos)
    out.writeInt(list.size)
    for (ke in list) {
        out.writeObject(ke.item)
        out.writeDouble(ke.sans)
    }
    out.close()
    return Base64.getEncoder().encodeToString(baos.toByteArray())
}

fun kasaEsyaListFromBase64(data: String): List<KasaEsya> {
    val bais = ByteArrayInputStream(Base64.getDecoder().decode(data))
    val input = BukkitObjectInputStream(bais)
    val size = input.readInt()
    val list = mutableListOf<KasaEsya>()
    for (i in 0 until size) {
        val item = input.readObject() as ItemStack
        val sans = input.readDouble()
        list.add(KasaEsya(item, sans))
    }
    input.close()
    return list
}
fun ItemStack.setSansLore(sans: Double): ItemStack {
    val meta = this.itemMeta ?: return this
    val lore = meta.lore?.toMutableList() ?: mutableListOf()

    // Öncelikle eski "Şans:" satırlarını temizle
    val sansPrefix = ChatColor.translateAlternateColorCodes('&', LangManager.msg("chance_prefix"))
    val temizlenmisLore = lore.filterNot { it.startsWith(sansPrefix) }.toMutableList()

    val raw = LangManager.msg("chance_line")
    val sansYuzde = String.format("%.2f", sans * 100)
    val sansSatiri = ChatColor.translateAlternateColorCodes('&', raw.replace("%chance%", sansYuzde))

    temizlenmisLore.add(sansSatiri)

    meta.lore = temizlenmisLore
    this.itemMeta = meta
    return this
}

