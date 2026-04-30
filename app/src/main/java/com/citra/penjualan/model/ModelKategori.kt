package com.citra.penjualan.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ModelKategori(
    val idKategori: String? = null,
    val namaKategori: String? = null,
    val statusKategori: String? = null,
    val cabangKategori: String? = null
) : Parcelable