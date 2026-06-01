package com.citra.penjualan.model

data class Pengumuman(
    var id: String = "",
    var judul: String = "",
    var pesan: String = "",
    var timestamp: Long = 0,
    var pengirim: String = "",
    var targetCabang: String = "all" // "all" or specific branch name
)
