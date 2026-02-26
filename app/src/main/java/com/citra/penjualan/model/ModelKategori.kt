package com.citra.penjualan.kategori

import android.os.Parcel
import android.os.Parcelable

data class ModelKategori(
    var idKategori: String? = null,
    var namaKategori: String? = null,
    var statusKategori: String? = null
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(idKategori)
        parcel.writeString(namaKategori)
        parcel.writeString(statusKategori)
    }

    companion object CREATOR : Parcelable.Creator<ModelKategori> {
        override fun createFromParcel(parcel: Parcel): ModelKategori {
            return ModelKategori(parcel)
        }

        override fun newArray(size: Int): Array<ModelKategori?> {
            return arrayOfNulls(size)
        }
    }
}