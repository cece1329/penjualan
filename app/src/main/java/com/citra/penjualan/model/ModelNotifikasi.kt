package com.citra.penjualan.model

data class Notifikasi(
    var id: String = "",
    var jenis: String = "", // "stok_habis", "pengumuman"
    var judul: String = "",
    var pesan: String = "",
    var timestamp: Long = 0,
    var targetCabang: String = "all", // "all" or specific branch name
    var dibaca: Boolean = false
)
