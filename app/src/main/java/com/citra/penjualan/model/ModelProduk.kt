package com.citra.penjualan.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ModelProduk(
    var idProduk: String? = "",
    var namaProduk: String = "",
    var hargaProduk: Int = 0,
    var stokProduk: Int = 0,
    var cabangProduk: String = "",
    var statusProduk: String = "",
    var namaKategori: String = "" // Tambahkan baris ini
) : Parcelable