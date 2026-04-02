package com.citra.penjualan.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ModelProduk(
    // GUNAKAN 'var' supaya bisa diisi manual di ViewModel
    var idProduk: String? = null,
    var namaProduk: String? = null,
    var hargaProduk: Int? = 0,
    var idKategori: String? = null,
    var idCabang: String? = null,
    var fotoProduk: String? = null,
    var stokProduk: Int? = 0,
    var tanpaBatas: Boolean? = false,
    var statusProduk: String? = null,
    var createdAt: String? = null,
    var updateAt: String? = null
) : Parcelable