package me.bilolib.bobcase

import me.bilolib.bobcase.model.AnimasyonTuru

import org.bukkit.inventory.ItemStack

data class Kasa(
    var renkliIsim: String,
    var temizIsim: String,
    var esyaListesi: MutableList<KasaEsya>,
    var gorunumItem: ItemStack,
    var permission: String? = null,
    var animasyonTuru: AnimasyonTuru = AnimasyonTuru.CSGO
) {
    var dusurmeMobList = mutableListOf<String>()
    var dusurmeBlockList = mutableListOf<String>()

    var dusurmeMobSanslari = mutableMapOf<String, Double>()
    var dusurmeBlockSanslari = mutableMapOf<String, Double>()
}