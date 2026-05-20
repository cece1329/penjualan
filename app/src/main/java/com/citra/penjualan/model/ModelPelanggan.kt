package com.citra.penjualan.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ModelPelanggan(
    var idPelanggan: String? = "",
    var namaPelanggan: String? = "",
    var teleponPelanggan: String? = "",
    var alamatPelanggan: String? = ""
) : Parcelable
