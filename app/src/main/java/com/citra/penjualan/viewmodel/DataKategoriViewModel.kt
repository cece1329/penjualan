package com.citra.penjualan.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.citra.penjualan.model.ModelKategori
import com.google.firebase.database.*

class DataKategoriViewModel : ViewModel() {
    private val myRef = FirebaseDatabase.getInstance().getReference("kategori")

    val kategoriList = MutableLiveData<ArrayList<ModelKategori>>()
    private var originalKategoriList = ArrayList<ModelKategori>()

    init { loadData() }

    private fun loadData() {
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = ArrayList<ModelKategori>()
                for (data in snapshot.children) {
                    val kategori = data.getValue(ModelKategori::class.java)
                    if (kategori != null) items.add(kategori.copy(idKategori = data.key))
                }
                originalKategoriList = items
                kategoriList.value = items
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun filter(query: String) {
        val filteredList = if (query.isEmpty()) originalKategoriList
        else ArrayList(originalKategoriList.filter {
            it.namaKategori?.lowercase()?.contains(query.lowercase()) == true ||
                it.cabangKategori?.lowercase()?.contains(query.lowercase()) == true
        })
        kategoriList.value = filteredList
    }
}
