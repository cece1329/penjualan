package com.citra.penjualan.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ModelTransaksi(
    var idTransaksi: String? = "",
    var namaProduk: String? = "",
    var jumlah: Int = 0,
    var totalHarga: Int = 0,
    var tanggal: String? = ""
) : Parcelable
