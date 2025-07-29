import me.bilolib.bobcase.LangManager
import org.bukkit.ChatColor
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.*
import java.util.*

object ItemSerializer {

    fun itemStackListToBase64(itemList: List<ItemStack>): String {
        ByteArrayOutputStream().use { byteOutput ->
            BukkitObjectOutputStream(byteOutput).use { output ->
                output.writeInt(itemList.size)
                for (item in itemList) {
                    output.writeObject(item)
                }
            }
            return Base64.getEncoder().encodeToString(byteOutput.toByteArray())
        }
    }

    fun itemStackListFromBase64(data: String): List<ItemStack> {
        ByteArrayInputStream(Base64.getDecoder().decode(data)).use { byteInput ->
            BukkitObjectInputStream(byteInput).use { input ->
                val size = input.readInt()
                val itemList = mutableListOf<ItemStack>()
                for (i in 0 until size) {
                    val item = input.readObject() as ItemStack
                    itemList.add(item)
                }
                return itemList
            }
        }
    }
    fun ItemStack.temizSansLore(): ItemStack {
        val meta = this.itemMeta ?: return this
        val lore = meta.lore?.toMutableList() ?: mutableListOf()

        val prefixRaw = LangManager.msg("chance_prefix_raw") // Örn: "Chance:"
        val temizlenmisLore = lore.filterNot {
            ChatColor.stripColor(it)?.startsWith(prefixRaw) == true
        }.toMutableList()


        meta.lore = temizlenmisLore
        this.itemMeta = meta
        return this
    }


}

