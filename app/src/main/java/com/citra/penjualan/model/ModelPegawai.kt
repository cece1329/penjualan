package com.citra.penjualan.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ModelPegawai(
    var idPegawai: String? = "",
    var namaPegawai: String? = "",
    var jabatanPegawai: String? = "",
    var teleponPegawai: String? = ""
) : Parcelable
