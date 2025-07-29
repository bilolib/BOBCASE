package me.bilolib.bobcase.sqlite

import org.bukkit.Material

import java.sql.SQLException
import java.util.*

object MenuRenkDAO {

    /**
     * Bir oyuncunun seçtiği cam renklerini veritabanına kaydeder.
     */
    fun kaydet(uuid: UUID, camlar: List<Material>) {
        val conn = BobSQLite.getConnection()
        try {
            // Transaction başlat
            conn.autoCommit = false

            // Eski kayıtları sil
            conn.prepareStatement("DELETE FROM menu_renkleri WHERE uuid = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeUpdate()
            }

            // Yeni cam renklerini ekle
            conn.prepareStatement("INSERT INTO menu_renkleri (uuid, cam_rengi) VALUES (?, ?)").use { stmt ->
                for (cam in camlar) {
                    stmt.setString(1, uuid.toString())
                    stmt.setString(2, cam.name)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }

            conn.commit()
        } catch (e: SQLException) {
            conn.rollback()
            e.printStackTrace()
        } finally {
            conn.autoCommit = true
        }
    }

    /**
     * Belirli bir oyuncunun cam rengi tercihlerini getirir.
     */
    fun yukle(uuid: UUID): List<Material> {
        val camlar = mutableListOf<Material>()
        val conn = BobSQLite.getConnection()

        try {
            conn.prepareStatement("SELECT cam_rengi FROM menu_renkleri WHERE uuid = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    val ad = rs.getString("cam_rengi")
                    val mat = Material.matchMaterial(ad)
                    if (mat != null) {
                        camlar.add(mat)
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }

        return camlar
    }

    /**
     * Tüm kayıtlı oyuncuların cam verilerini döner.
     * Bu opsiyonel bir fonksiyon ama lazım olabilir.
     */
    fun tumCamRenkleriniYukle(): Map<UUID, List<Material>> {
        val sonuc = mutableMapOf<UUID, MutableList<Material>>()
        val conn = BobSQLite.getConnection()

        try {
            conn.prepareStatement("SELECT uuid, cam_rengi FROM menu_renkleri").use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val uuid = UUID.fromString(rs.getString("uuid"))
                    val mat = Material.matchMaterial(rs.getString("cam_rengi"))
                    if (mat != null) {
                        sonuc.computeIfAbsent(uuid) { mutableListOf() }.add(mat)
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }

        return sonuc
    }
}
