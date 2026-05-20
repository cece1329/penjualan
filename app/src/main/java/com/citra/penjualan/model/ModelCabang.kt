package com.citra.penjualan.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ModelCabang(
    var idCabang: String? = "",
    var namaCabang: String? = "",
    var kotaCabang: String? = "",
    var alamatCabang: String? = ""
) : Parcelable
