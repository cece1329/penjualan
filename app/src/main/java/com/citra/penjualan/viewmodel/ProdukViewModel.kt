package com.citra.penjualan.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.citra.penjualan.model.ModelProduk
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProdukViewModel : ViewModel() {
    private val dbRef = FirebaseDatabase.getInstance().getReference("produk")

    private val _listProduk = MutableLiveData<List<ModelProduk>>()
    val listProduk: LiveData<List<ModelProduk>> = _listProduk

    private var originalProdukList: List<ModelProduk> = emptyList()

    fun fetchProduk() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<ModelProduk>()
                for (data in snapshot.children) {
                    val produk = data.getValue(ModelProduk::class.java)
                    if (produk != null) {
                        produk.idProduk = data.key
                        items.add(produk)
                    }
                }
                originalProdukList = items
                _listProduk.value = items
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun filter(query: String) {
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) {
            originalProdukList
        } else {
            originalProdukList.filter {
                it.namaProduk.lowercase().contains(q) ||
                    it.namaKategori?.lowercase()?.contains(q) == true ||
                    it.cabangProduk?.lowercase()?.contains(q) == true
            }
        }
        _listProduk.value = filtered
    }
}

