package me.bilolib.bobcase.sqlite

import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object BobSQLite {
    private var connection: Connection? = null
    private lateinit var dbFile: File

    // Sunucu açıldığında çağrılmalı
    fun baslat(plugin: JavaPlugin) {
        dbFile = File(plugin.dataFolder, "kasalar.db")
        if (!dbFile.exists()) {
            dbFile.parentFile.mkdirs()
            dbFile.createNewFile()
        }

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
            createTables()
        } catch (e: SQLException) {
            e.printStackTrace()
            throw IllegalStateException("Veritabanı başlatılamadı!")
        }
    }

    // Tabloları oluştur
    private fun createTables() {
        getConnection().createStatement().use { stmt ->
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS kasalar (
                    isim TEXT PRIMARY KEY,
                    renkliIsim TEXT,
                    esyalar TEXT,
                    gorunumItem TEXT,
                    dusurmeMobList TEXT,
                    dusurmeBlockList TEXT,
                    dusurmeMobSanslari TEXT,
                    dusurmeBlockSanslari TEXT,
                    permission TEXT,
                    animasyonTuru TEXT DEFAULT 'CSGO'
                )
                """.trimIndent()
            )

            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS ayarlar (
                    anahtar TEXT PRIMARY KEY,
                    deger TEXT
                )
                """.trimIndent()
            )

            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS menu_renkleri (
                    uuid TEXT NOT NULL,
                    cam_rengi TEXT NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    // Plugin kapatıldığında çağrılmalı
    fun kapat() {
        try {
            connection?.close()
            connection = null
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    // Veritabanı bağlantısını döndür
    fun getConnection(): Connection {
        if (connection == null || connection?.isClosed == true) {
            try {
                connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
            } catch (e: SQLException) {
                e.printStackTrace()
                throw IllegalStateException("Veritabanı bağlantısı açılamadı!")
            }
        }
        return connection!!
    }

    // ✅ Ayar: Kasa açılış gecikmesi (ms)
    fun setDelay(ms: Long) {
        val conn = getConnection()
        conn.prepareStatement(
            "REPLACE INTO ayarlar (anahtar, deger) VALUES (?, ?)"
        ).use { stmt ->
            stmt.setString(1, "delay")
            stmt.setString(2, ms.toString())
            stmt.executeUpdate()
        }
    }

    fun getDelay(): Long {
        val conn = getConnection()
        conn.prepareStatement(
            "SELECT deger FROM ayarlar WHERE anahtar = ?"
        ).use { stmt ->
            stmt.setString(1, "delay")
            val rs = stmt.executeQuery()
            if (rs.next()) {
                return rs.getString("deger").toLongOrNull() ?: 3000L
            }
        }
        return 3000L // Varsayılan: 3 saniye
    }
    private fun migrateDatabase(conn: Connection) {
        // Örnek: animasyonTuru alanı yoksa ekle
        val stmt = conn.createStatement()
        val rs = stmt.executeQuery("PRAGMA table_info(kasalar);")
        val columns = mutableSetOf<String>()
        while (rs.next()) {
            columns.add(rs.getString("name"))
        }
        if (!columns.contains("animasyonTuru")) {
            stmt.executeUpdate("ALTER TABLE kasalar ADD COLUMN animasyonTuru TEXT DEFAULT 'CSGO';")
        }
        rs.close()
        stmt.close()
    }

}
