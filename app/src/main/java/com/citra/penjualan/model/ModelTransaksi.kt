package com.citra.penjualan.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ModelTransaksi(
    var idTransaksi: String? = "",
    var namaProduk: String? = "",
    var jumlah: Int = 0,
    var totalHarga: Int = 0,
    var tanggal: String? = "",
    var metodePembayaran: String? = "",
    var uangDiterima: Int? = null,
    var kembalian: Int? = null,
    var noGopay: String? = null,
    var idPelanggan: String? = null,
    var namaPelanggan: String? = "Pelanggan Umum",
    var teleponPelanggan: String? = null,
    var jenisPelanggan: String? = "Umum",
    var idKasir: String? = null,
    var namaKasir: String? = null,
    var jabatanKasir: String? = null,
    var cabangKasir: String? = null,
    var namaToko: String? = null,
    var isPrinted: Boolean = false
) : Parcelable
