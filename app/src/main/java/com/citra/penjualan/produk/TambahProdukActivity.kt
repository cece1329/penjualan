package com.citra.penjualan.produk

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ActivityTambahProdukBinding
import com.citra.penjualan.model.ModelKategori
import com.citra.penjualan.model.ModelProduk
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TambahProdukActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTambahProdukBinding
    private val db = FirebaseDatabase.getInstance().getReference("produk")
    private val dbCabang = FirebaseDatabase.getInstance().getReference("cabang")
    private val dbKategori = FirebaseDatabase.getInstance().getReference("kategori")
    private var dataEdit: ModelProduk? = null

    private val listCabang = ArrayList<String>()
    private val listAllKategori = ArrayList<ModelKategori>()
    private val listFilteredKategoriNames = ArrayList<String>()

    private lateinit var adapterCabang: ArrayAdapter<String>
    private lateinit var adapterKategori: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahProdukBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Spinners
        adapterCabang = ArrayAdapter(this, R.layout.item_spinner_selected, listCabang)
        adapterCabang.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spinnerCabangProduk.adapter = adapterCabang

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

        // Fitur Hapus (Muncul hanya saat mode Edit)
        if (dataEdit != null) {
            binding.btnHapusProduk.visibility = View.VISIBLE
            binding.btnHapusProduk.setOnClickListener {
                showDeleteConfirmation()
            }
        } else {
            binding.btnHapusProduk.visibility = View.GONE
        }

        // Filter categories when a branch is selected
        binding.spinnerCabangProduk.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedBranch = listCabang.getOrNull(position) ?: ""
                filterCategoriesByBranch(selectedBranch)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
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
                adapterCabang.notifyDataSetChanged()

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

                        // Trigger initial filtering based on selected branch
                        val initialBranch = binding.spinnerCabangProduk.selectedItem?.toString() ?: ""
                        filterCategoriesByBranch(initialBranch)

                        // If Edit Mode, set selections
                        if (dataEdit != null) {
                            setupViewEdit()
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

    private fun filterCategoriesByBranch(branchName: String) {
        listFilteredKategoriNames.clear()
        
        // Filter categories that belong to the selected branch
        val filtered = listAllKategori.filter { it.cabangKategori?.equals(branchName, ignoreCase = true) == true }
        for (k in filtered) {
            val name = k.namaKategori
            if (name != null) {
                listFilteredKategoriNames.add(name)
            }
        }

        // Fallback to all category names if none match the branch, to ensure user has choices
        if (listFilteredKategoriNames.isEmpty()) {
            for (k in listAllKategori) {
                val name = k.namaKategori
                if (name != null) {
                    listFilteredKategoriNames.add(name)
                }
            }
        }

        // Add a default if totally empty
        if (listFilteredKategoriNames.isEmpty()) {
            listFilteredKategoriNames.add(getString(R.string.msg_general))
        }

        adapterKategori.notifyDataSetChanged()
    }

    private fun setupViewEdit() {
        binding.apply {
            inNamaProduk.setText(dataEdit?.namaProduk)
            inHargaBeli.setText(dataEdit?.hargaProduk?.toString() ?: "")
            inStokProduk.setText(dataEdit?.stokProduk?.toString() ?: "")
            chipStatus.text = dataEdit?.statusProduk
            btnSimpanProduk.text = getString(R.string.produk_update_btn)

            // Set selected branch in spinner
            val branchIndex = listCabang.indexOf(dataEdit?.cabangProduk)
            if (branchIndex != -1) {
                spinnerCabangProduk.setSelection(branchIndex)
                
                // Immediately filter and set category
                filterCategoriesByBranch(dataEdit?.cabangProduk ?: "")
                val categoryIndex = listFilteredKategoriNames.indexOf(dataEdit?.namaKategori)
                if (categoryIndex != -1) {
                    spinnerKategoriProduk.setSelection(categoryIndex)
                }
            }
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
        val hargaStr = binding.inHargaBeli.text.toString().trim()
        val stokStr = binding.inStokProduk.text.toString().trim()
        val cabang = binding.spinnerCabangProduk.selectedItem?.toString()?.trim() ?: ""
        val kategori = binding.spinnerKategoriProduk.selectedItem?.toString()?.trim() ?: ""
        val status = binding.chipStatus.text.toString()

        // Validasi jika ada field yang kosong
        if (nama.isEmpty() || hargaStr.isEmpty() || stokStr.isEmpty() || cabang.isEmpty() || kategori.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_complete_all_data), Toast.LENGTH_SHORT).show()
            return
        }

        val harga = hargaStr.toIntOrNull() ?: 0
        val stok = stokStr.toIntOrNull() ?: 0
        val id = dataEdit?.idProduk ?: db.push().key

        // Menyiapkan model produk
        val produk = ModelProduk(
            idProduk = id,
            namaProduk = nama,
            hargaProduk = harga,
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
