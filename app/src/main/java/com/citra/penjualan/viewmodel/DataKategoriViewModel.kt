package com.citra.penjualan.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.citra.penjualan.kategori.Kategori
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DataKategoriViewModel : ViewModel() {

    // Menghubungkan ke Firebase Database
    private val database = FirebaseDatabase.getInstance()
    // Mengambil referensi ke tabel/node bernama "kategori"
    private val myRef = database.getReference("kategori")

    // Variabel untuk menampung list kategori yang bisa diamati oleh UI
    val kategoriList = MutableLiveData<ArrayList<Kategori>>()
    // Variabel untuk menyimpan data asli dari database sebagai cadangan
    private var originalKategoriList = ArrayList<Kategori>()

    // Variabel untuk status loading (sedang mengambil data atau tidak)
    val isLoading = MutableLiveData<Boolean>()
    // Variabel untuk status jika hasil pencarian tidak ditemukan
    val isSearchEmpty = MutableLiveData<Boolean>()

    init {
        // Menjalankan fungsi ambil data saat pertama kali ViewModel dibuat
        loadData()
    }

    private fun loadData() {
        // Set loading menjadi true (aktif)
        isLoading.value = true
        // Mendengarkan perubahan data di Firebase secara real-time
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = ArrayList<Kategori>()
                // Mengambil setiap data yang ada di dalam node kategori
                for (data in snapshot.children) {
                    val kategori = data.getValue(Kategori::class.java)
                    if (kategori != null) {
                        items.add(kategori) // Masukkan data ke list
                    }
                }
                // Simpan hasil ke list original dan list yang akan ditampilkan
                originalKategoriList = items
                kategoriList.value = items
                // Set loading menjadi false (berhenti) karena data sudah dapat
                isLoading.value = false
            }

            override fun onCancelled(error: DatabaseError) {
                // Jika proses gagal/dibatalkan, loading tetap harus berhenti
                isLoading.value = false
            }
        })
    }

    // Fungsi untuk menyaring data berdasarkan ketikan user
    fun filter(query: String) {
        val filteredList = if (query.isEmpty()) {
            // Jika kosong, balikkan ke data asli
            originalKategoriList
        } else {
            // Jika diisi, cari nama yang mengandung teks dari variabel query
            originalKategoriList.filter {
                it.nama.lowercase().contains(query.lowercase())
            } as ArrayList<Kategori>
        }
        // Update list kategori dengan hasil filter
        kategoriList.value = filteredList
        // Cek apakah hasil filternya kosong atau tidak
        isSearchEmpty.value = filteredList.isEmpty()
    }

    // Fungsi untuk menghapus data di Firebase berdasarkan ID
    fun deleteKategori(id: String) {
        myRef.child(id).removeValue()
    }
}