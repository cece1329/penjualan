package com.citra.penjualan.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.citra.penjualan.model.ModelProduk
import com.google.firebase.database.*

class ProdukViewModel : ViewModel() {
    // Path disesuaikan dengan Realtime Database kamu
    private val dbRef = FirebaseDatabase.getInstance().getReference("produk")

    private val _listProduk = MutableLiveData<List<ModelProduk>>()
    val listProduk: LiveData<List<ModelProduk>> = _listProduk

    fun fetchProduk() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<ModelProduk>()
                for (data in snapshot.children) {
                    val produk = data.getValue(ModelProduk::class.java)
                    if (produk != null) {
                        // Mengambil key dari Firebase sebagai idProduk
                        produk.idProduk = data.key
                        items.add(produk)
                    }
                }
                _listProduk.value = items
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}