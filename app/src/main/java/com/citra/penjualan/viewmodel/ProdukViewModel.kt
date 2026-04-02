package com.citra.penjualan.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData // INI YANG BENER (Bukan MutableCorner)
import androidx.lifecycle.ViewModel
import com.citra.penjualan.model.ModelProduk
import com.google.firebase.database.*

class ProdukViewModel : ViewModel() {
    // Pastikan nama path di Firebase sesuai, misal "Produk"
    private val dbRef = FirebaseDatabase.getInstance().getReference("Produk")

    // Inisialisasi LiveData
    private val _listProduk = MutableLiveData<List<ModelProduk>>()
    val listProduk: LiveData<List<ModelProduk>> = _listProduk

    fun fetchProduk() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<ModelProduk>()
                for (data in snapshot.children) {
                    val produk = data.getValue(ModelProduk::class.java)

                    // Kita set idProduk dari key yang ada di Firebase
                    if (produk != null) {
                        produk.idProduk = data.key
                        items.add(produk)
                    }
                }
                _listProduk.value = items
            }

            override fun onCancelled(error: DatabaseError) {
                // Bisa tambahin log error di sini kalau perlu
            }
        })
    }
}