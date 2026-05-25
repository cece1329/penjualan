package com.citra.penjualan.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
// Model data untuk menyimpan informasi kontak pelanggan
data class ModelPelanggan(
    var idPelanggan: String? = "",
    var namaPelanggan: String? = "",
    var teleponPelanggan: String? = "",
    var alamatPelanggan: String? = "",
    var jenisPelanggan: String? = "Umum"
) : Parcelable
