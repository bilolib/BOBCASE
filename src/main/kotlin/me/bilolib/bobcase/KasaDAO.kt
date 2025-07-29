package me.bilolib.bobcase

import com.google.gson.Gson
import me.bilolib.bobcase.model.AnimasyonTuru
import me.bilolib.bobcase.sqlite.BobSQLite
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.sql.SQLException
import java.util.Base64

object KasaDAO {

    private val gson = Gson()

    @Throws(SQLException::class)
    fun kasaKaydet(kasa: Kasa) {
        val conn = BobSQLite.getConnection()
        conn.createStatement().use { stmt ->
            val rs = stmt.executeQuery("PRAGMA table_info(kasalar);")
            val columns = mutableSetOf<String>()
            while (rs.next()) {
                columns.add(rs.getString("name"))
            }
            if (!columns.contains("animasyonTuru")) {
                stmt.executeUpdate("ALTER TABLE kasalar ADD COLUMN animasyonTuru TEXT DEFAULT 'CSGO';")
            }
        }

        val sql = """
            REPLACE INTO kasalar (
                isim, renkliIsim, esyalar, gorunumItem, 
                dusurmeMobList, dusurmeBlockList, 
                dusurmeMobSanslari, dusurmeBlockSanslari,
                permission, animasyonTuru
            ) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, kasa.temizIsim)
            stmt.setString(2, kasa.renkliIsim)
            stmt.setString(3, kasaEsyaListToBase64(kasa.esyaListesi))
            stmt.setString(4, itemStackToBase64(kasa.gorunumItem))
            stmt.setString(5, gson.toJson(kasa.dusurmeMobList))
            stmt.setString(6, gson.toJson(kasa.dusurmeBlockList))
            stmt.setString(7, gson.toJson(kasa.dusurmeMobSanslari))
            stmt.setString(8, gson.toJson(kasa.dusurmeBlockSanslari))
            stmt.setString(9, kasa.permission)
            stmt.setString(10, kasa.animasyonTuru.name)
            stmt.executeUpdate()
        }
    }

    fun kasaYukle(): MutableMap<String, Kasa> {
        val map = mutableMapOf<String, Kasa>()
        val conn = BobSQLite.getConnection()
        conn.createStatement().use { stmt ->
            val rsCol = stmt.executeQuery("PRAGMA table_info(kasalar);")
            val columns = mutableSetOf<String>()
            while (rsCol.next()) {
                columns.add(rsCol.getString("name"))
            }
            if (!columns.contains("animasyonTuru")) {
                stmt.executeUpdate("ALTER TABLE kasalar ADD COLUMN animasyonTuru TEXT DEFAULT 'CSGO';")
            }

            val rs = stmt.executeQuery("SELECT * FROM kasalar")
            while (rs.next()) {
                val temizIsim = rs.getString("isim")
                val renkliIsim = rs.getString("renkliIsim") ?: temizIsim
                val base64Esya = rs.getString("esyalar")
                val base64Gorunum = rs.getString("gorunumItem")
                val dusurmeMobListJson = rs.getString("dusurmeMobList")
                val dusurmeBlockListJson = rs.getString("dusurmeBlockList")
                val dusurmeMobSanslariJson = rs.getString("dusurmeMobSanslari") ?: "{}"
                val dusurmeBlockSanslariJson = rs.getString("dusurmeBlockSanslari") ?: "{}"
                val permission = rs.getString("permission")
                val animasyonTuruStr = rs.getString("animasyonTuru") ?: "CSGO"

                val kasaEsyaListesi = kasaEsyaListFromBase64(base64Esya).map { it.copy(sans = 100.0) }.toMutableList()

                val gorunumItem = base64ToItemStack(base64Gorunum) ?: ItemStack(Material.CHEST)

                val dusurmeMobList = if (dusurmeMobListJson.isNullOrEmpty()) mutableListOf()
                else gson.fromJson(dusurmeMobListJson, Array<String>::class.java).toMutableList()

                val dusurmeBlockList = if (dusurmeBlockListJson.isNullOrEmpty()) mutableListOf()
                else gson.fromJson(dusurmeBlockListJson, Array<String>::class.java).toMutableList()

                val dusurmeMobSanslari = if (dusurmeMobSanslariJson.isNullOrEmpty()) mutableMapOf<String, Double>()
                else gson.fromJson(dusurmeMobSanslariJson, MutableMap::class.java) as MutableMap<String, Double>

                val dusurmeBlockSanslari = if (dusurmeBlockSanslariJson.isNullOrEmpty()) mutableMapOf<String, Double>()
                else gson.fromJson(dusurmeBlockSanslariJson, MutableMap::class.java) as MutableMap<String, Double>

                val animasyonTuru = try {
                    AnimasyonTuru.valueOf(animasyonTuruStr)
                } catch (ex: Exception) {
                    AnimasyonTuru.CSGO
                }

                val kasa = Kasa(
                    renkliIsim = renkliIsim,
                    temizIsim = temizIsim,
                    esyaListesi = kasaEsyaListesi,
                    gorunumItem = gorunumItem,
                    permission = permission,
                    animasyonTuru = animasyonTuru
                ).apply {
                    this.dusurmeMobList = dusurmeMobList
                    this.dusurmeBlockList = dusurmeBlockList
                    this.dusurmeMobSanslari = dusurmeMobSanslari
                    this.dusurmeBlockSanslari = dusurmeBlockSanslari
                }

                map[temizIsim] = kasa
            }
            rs.close()
        }
        return map
    }

    fun itemStackToBase64(item: ItemStack): String {
        val outputStream = ByteArrayOutputStream()
        BukkitObjectOutputStream(outputStream).use { dataOutput ->
            dataOutput.writeObject(item)
        }
        return Base64.getEncoder().encodeToString(outputStream.toByteArray())
    }

    fun base64ToItemStack(data: String): ItemStack? {
        val bytes = Base64.getDecoder().decode(data)
        ByteArrayInputStream(bytes).use { byteInputStream ->
            BukkitObjectInputStream(byteInputStream).use { dataInput ->
                return dataInput.readObject() as? ItemStack
            }
        }
    }

    fun getDelay(): Long {
        val conn = BobSQLite.getConnection()
        val rs = conn.prepareStatement("SELECT deger FROM ayarlar WHERE anahtar = 'kasa_delay_ms'").executeQuery()
        return if (rs.next()) rs.getString("deger").toLongOrNull() ?: 5000L else 5000L
    }

    fun setDelay(delayMs: Long) {
        val conn = BobSQLite.getConnection()
        val ps = conn.prepareStatement("REPLACE INTO ayarlar (anahtar, deger) VALUES ('kasa_delay_ms', ?)")
        ps.setString(1, delayMs.toString())
        ps.executeUpdate()
    }

    fun kasaSil(kasaIsmi: String) {
        val connection = BobSQLite.getConnection()
        val statement = connection.prepareStatement("DELETE FROM kasalar WHERE isim = ?")
        statement.setString(1, kasaIsmi)
        statement.executeUpdate()
        statement.close()
    }
}
