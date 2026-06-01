package com.citra.penjualan.model

data class CatatanHarian(
    var id: String = "",
    var catatan: String = "",
    var timestamp: Long = 0,
    var penulis: String = "",
    var cabang: String = "all" // "all" or specific branch name
)
