package com.citra.penjualan.produk

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.citra.penjualan.BaseActivity
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ActivityTambahProdukBinding
import com.citra.penjualan.model.ModelKategori
import com.citra.penjualan.model.ModelProduk
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TambahProdukActivity : BaseActivity() {

    private lateinit var binding: ActivityTambahProdukBinding
    private val db = FirebaseDatabase.getInstance().getReference("produk")
    private val dbCabang = FirebaseDatabase.getInstance().getReference("cabang")
    private val dbKategori = FirebaseDatabase.getInstance().getReference("kategori")
    private var dataEdit: ModelProduk? = null

    private val listCabang = ArrayList<String>()
    private val listAllKategori = ArrayList<ModelKategori>()
    private val listFilteredKategoriNames = ArrayList<String>()
    private var selectedBranches = ""
    private lateinit var adapterKategori: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahProdukBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapterKategori = ArrayAdapter(this, R.layout.item_spinner_selected, listFilteredKategoriNames)
        adapterKategori.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spinnerKategoriProduk.adapter = adapterKategori

        // Cek apakah ada data yang dikirim (berarti mode EDIT)
        dataEdit = intent.getParcelableExtra("DATA_PRODUK")

        // Load data in order: 1. Cabang, 2. Kategori, then bind if edit
        loadCabangAndKategori()

        // Logic ganti status via Chip
        binding.chipStatus.setOnClickListener {
            if (binding.chipStatus.text == getString(R.string.msg_active)) {
                binding.chipStatus.text = getString(R.string.msg_inactive)
            } else {
                binding.chipStatus.text = getString(R.string.msg_active)
            }
        }

        // Tombol Simpan atau Update Data
        binding.btnSimpanProduk.setOnClickListener {
            validateAndSave()
        }

        binding.btnPilihCabangProduk.setOnClickListener {
            showMultiBranchDialog()
        }

        // Fitur Hapus (Muncul hanya saat mode Edit)
        if (dataEdit != null) {
            binding.btnHapusProduk.visibility = View.VISIBLE
            binding.btnHapusProduk.setOnClickListener {
                showDeleteConfirmation()
            }
        } else {
            binding.btnHapusProduk.visibility = View.GONE
        }
    }

    private fun loadCabangAndKategori() {
        // Fetch Branches
        dbCabang.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(branchSnapshot: DataSnapshot) {
                listCabang.clear()
                for (data in branchSnapshot.children) {
                    val nama = data.child("namaCabang").value?.toString()
                    if (nama != null) {
                        listCabang.add(nama)
                    }
                }
                if (listCabang.isEmpty()) {
                    listCabang.add(getString(R.string.msg_center))
                }

                // Now fetch Categories
                dbKategori.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(kategoriSnapshot: DataSnapshot) {
                        listAllKategori.clear()
                        for (data in kategoriSnapshot.children) {
                            val kat = data.getValue(ModelKategori::class.java)
                            if (kat != null) {
                                listAllKategori.add(kat)
                            }
                        }

                        // If Edit Mode, set selections
                        if (dataEdit != null) {
                            setupViewEdit()
                        } else {
                            filterCategoriesByBranch("") // Tampilkan semua kategori di awal
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@TambahProdukActivity, getString(R.string.produk_load_category_failed, error.message), Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TambahProdukActivity, getString(R.string.produk_load_branch_failed, error.message), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterCategoriesByBranch(selectedBranchesString: String) {
        val currentSelected = binding.spinnerKategoriProduk.selectedItem?.toString()
        listFilteredKategoriNames.clear()
        val selectedList = selectedBranchesString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        val filtered = listAllKategori.filter { kat ->
            val katBranches = kat.cabangKategori?.split(",")?.map { it.trim() } ?: emptyList()
            val selectedList = selectedBranchesString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            
            // Logika: Tampilkan kategori jika:
            // 1. Belum ada cabang produk dipilih (tampilkan semua agar tidak kosong)
            // 2. Kategori tersebut tersedia di "Semua Cabang"
            // 3. Ada kecocokan antara salah satu cabang produk dan cabang kategori
            selectedList.isEmpty() || 
            katBranches.contains("Semua Cabang") || 
            selectedList.any { katBranches.contains(it) } ||
            selectedList.contains("Semua Cabang")
        }
        
        val filteredNames = filtered.mapNotNull { it.namaKategori }.distinct()
        listFilteredKategoriNames.addAll(filteredNames)
        
        if (listFilteredKategoriNames.isEmpty()) {
            listFilteredKategoriNames.add("Umum")
        }
        adapterKategori.notifyDataSetChanged()

        // Logika mempertahankan seleksi: 
        // 1. Jika mode edit, prioritaskan kategori asli produk
        // 2. Jika tidak, coba pertahankan apa yang sudah dipilih user sebelumnya
        val toSelect = if (dataEdit != null && listFilteredKategoriNames.contains(dataEdit?.namaKategori)) {
            dataEdit?.namaKategori
        } else if (currentSelected != null && listFilteredKategoriNames.contains(currentSelected)) {
            currentSelected
        } else null

        // Gunakan post untuk memastikan adapter sudah siap sebelum set selection
        binding.spinnerKategoriProduk.post {
            toSelect?.let {
                val index = listFilteredKategoriNames.indexOf(it)
                if (index >= 0) binding.spinnerKategoriProduk.setSelection(index)
            }
        }
    }

    private fun showMultiBranchDialog() {
        val branchesArray = listCabang.toTypedArray()
        val checkedItems = BooleanArray(branchesArray.size)
        val currentList = selectedBranches.split(",").map { it.trim() }
        
        branchesArray.forEachIndexed { index, s ->
            if (currentList.contains(s)) checkedItems[index] = true
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Pilih Cabang Produk")
            .setMultiChoiceItems(branchesArray, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Simpan") { _, _ ->
                val result = mutableListOf<String>()
                checkedItems.forEachIndexed { index, b -> if (b) result.add(branchesArray[index]) }
                
                if (result.isEmpty()) {
                    Toast.makeText(this, "Minimal pilih 1 cabang", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                selectedBranches = if (result.size == listCabang.size && listCabang.size > 1) "Semua Cabang" 
                                 else result.joinToString(", ")
                
                binding.tvSelectedCabangProduk.text = if (selectedBranches.isEmpty()) "Pilih Cabang" else selectedBranches
                filterCategoriesByBranch(selectedBranches)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupViewEdit() {
        with(binding) {
            inNamaProduk.setText(dataEdit?.namaProduk ?: "")
            inHargaProduk.setText(dataEdit?.hargaProduk?.toString() ?: "")
            inHargaBeli.setText(if ((dataEdit?.hargaBeli ?: 0) > 0) dataEdit?.hargaBeli?.toString() else "")
            inStokProduk.setText(dataEdit?.stokProduk?.toString() ?: "")
            chipStatus.text = dataEdit?.statusProduk ?: getString(R.string.msg_active)
            btnSimpanProduk.text = getString(R.string.produk_update_btn)

            selectedBranches = dataEdit?.cabangProduk ?: ""
            tvSelectedCabangProduk.text = if (selectedBranches.isEmpty()) "Pilih Cabang" else selectedBranches
            
            filterCategoriesByBranch(selectedBranches)
        }
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_confirm_title))
            .setMessage(getString(R.string.delete_confirm_msg))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                dataEdit?.idProduk?.let { id ->
                    db.child(id).removeValue().addOnSuccessListener { // Menghapus data dari Firebase
                        Toast.makeText(this, getString(R.string.produk_delete_success), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun validateAndSave() {
        val nama = binding.inNamaProduk.text.toString().trim()
        val hargaStr = binding.inHargaProduk.text.toString().trim()
        val hargaBeliStr = binding.inHargaBeli.text.toString().trim()
        val stokStr = binding.inStokProduk.text.toString().trim()
        val cabang = selectedBranches
        val kategori = binding.spinnerKategoriProduk.selectedItem?.toString()?.trim() ?: ""
        val status = binding.chipStatus.text.toString()

        // Validasi jika ada field yang kosong
        if (nama.isEmpty() || hargaStr.isEmpty() || stokStr.isEmpty() || cabang.isEmpty() || kategori.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_complete_all_data), Toast.LENGTH_SHORT).show()
            return
        }

        val harga = hargaStr.toIntOrNull() ?: 0
        val hargaBeli = hargaBeliStr.toIntOrNull() ?: 0
        val stok = stokStr.toIntOrNull() ?: 0
        val id = dataEdit?.idProduk ?: (db.push().key ?: System.currentTimeMillis().toString())

        // Menyiapkan model produk
        val produk = ModelProduk(
            idProduk = id,
            namaProduk = nama,
            hargaProduk = harga,
            hargaBeli = hargaBeli,
            stokProduk = stok,
            cabangProduk = cabang,
            statusProduk = status,
            namaKategori = kategori
        )

        // Menyimpan atau Update ke Firebase Database
        if (id != null) {
            db.child(id).setValue(produk).addOnSuccessListener {
                Toast.makeText(this, getString(R.string.produk_save_success), Toast.LENGTH_SHORT).show()
                finish() // Kembali ke halaman sebelumnya
            }.addOnFailureListener {
                Toast.makeText(this, getString(R.string.msg_failed, it.message), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
