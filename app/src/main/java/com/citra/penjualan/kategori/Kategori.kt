package com.citra.penjualan.kategori

data class Kategori(
    val id: String = "",
    val nama: String = "",
    val status: String = "Aktif"
) {

    constructor() : this("", "", "Aktif")
}