package com.citra.penjualan.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
// Model data untuk menyimpan informasi pegawai beserta hak akses login
data class ModelPegawai(
    var idPegawai: String? = "",
    var namaPegawai: String? = "",
    var jabatanPegawai: String? = "",
    var teleponPegawai: String? = "",
    var passwordPegawai: String? = ""
) : Parcelable
