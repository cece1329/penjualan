package com.citra.penjualan.kategori

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import com.citra.penjualan.BaseActivity
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ActivityTambahKategoriBinding
import com.citra.penjualan.model.ModelKategori
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TambahKategoriActivity : BaseActivity() {

    private lateinit var binding: ActivityTambahKategoriBinding
    private val db = FirebaseDatabase.getInstance().getReference("kategori")
    private val dbCabang = FirebaseDatabase.getInstance().getReference("cabang")
    private var dataEdit: ModelKategori? = null

    private val listCabang = ArrayList<String>()
    private var selectedBranches = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahKategoriBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Cek apakah ada data yang dikirim (berarti mode EDIT)
        dataEdit = intent.getParcelableExtra("DATA_KATEGORI")

        // Load branches first
        loadBranches()

        // Logic ganti status via Chip
        binding.chipStatus.setOnClickListener {
            if (binding.chipStatus.text == getString(R.string.msg_active)) {
                binding.chipStatus.text = getString(R.string.msg_inactive)
            } else {
                binding.chipStatus.text = getString(R.string.msg_active)
            }
        }

        binding.btnPilihCabang.setOnClickListener {
            showMultiBranchDialog()
        }

        // Tombol Simpan atau Update Data
        binding.btnSimpan.setOnClickListener {
            validateAndSave()
        }

        // Fitur Hapus (Hanya muncul jika dalam mode Edit)
        if (dataEdit != null) {
            binding.btnHapus.visibility = android.view.View.VISIBLE
            binding.btnHapus.setOnClickListener {
                showDeleteConfirmation()
            }
        } else {
            binding.btnHapus.visibility = android.view.View.GONE
        }
    }

    private fun loadBranches() {
        dbCabang.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listCabang.clear()
                for (data in snapshot.children) {
                    val nama = data.child("namaCabang").value?.toString()
                    if (nama != null) {
                        listCabang.add(nama)
                    }
                }
                
                // Add a default if empty
                if (listCabang.isEmpty()) {
                    listCabang.add(getString(R.string.msg_center))
                }

                if (dataEdit != null) {
                    setupViewEdit()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TambahKategoriActivity, getString(R.string.kategori_load_branch_failed, error.message), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupViewEdit() {
        binding.apply {
            etNamaKategori.setText(dataEdit?.namaKategori)
            chipStatus.text = dataEdit?.statusKategori ?: getString(R.string.msg_active)
            btnSimpan.text = getString(R.string.kategori_update_btn)
            selectedBranches = dataEdit?.cabangKategori ?: ""
            tvSelectedCabang.text = if (selectedBranches.isEmpty()) "Pilih Cabang" else selectedBranches
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
            .setTitle("Pilih Cabang")
            .setMultiChoiceItems(branchesArray, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Simpan") { _, _ ->
                val result = mutableListOf<String>()
                checkedItems.forEachIndexed { index, b -> if (b) result.add(branchesArray[index]) }
                if (result.isEmpty()) {
                    Toast.makeText(this, "Minimal pilih 1 cabang", Toast.LENGTH_SHORT).show()
                }
                selectedBranches = if (result.size == listCabang.size) "Semua Cabang" else result.joinToString(", ")
                binding.tvSelectedCabang.text = selectedBranches
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_confirm_title))
            .setMessage(getString(R.string.delete_confirm_msg))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                dataEdit?.idKategori?.let { id ->
                    val oldName = dataEdit?.namaKategori
                    db.child(id).removeValue().addOnSuccessListener {
                        if (oldName != null) {
                            val dbProduk = FirebaseDatabase.getInstance().getReference("produk")
                            dbProduk.orderByChild("namaKategori").equalTo(oldName)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val updates = HashMap<String, Any>()
                                        for (productSnap in snapshot.children) {
                                            val productId = productSnap.key ?: continue
                                            updates["$productId/statusProduk"] = getString(R.string.msg_inactive)
                                        }
                                        if (updates.isNotEmpty()) {
                                            dbProduk.updateChildren(updates)
                                        }
                                    }
                                    override fun onCancelled(error: DatabaseError) {}
                                })
                        }
                        Toast.makeText(this, "Kategori berhasil dihapus", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun validateAndSave() {
        val nama = binding.etNamaKategori.text.toString().trim()
        val cabang = selectedBranches
        val status = binding.chipStatus.text.toString()
        if (nama.isEmpty() || cabang.isEmpty()) {
            Toast.makeText(this, "Lengkapi data dan pilih minimal 1 cabang", Toast.LENGTH_SHORT).show()
            return
        }

        val id = dataEdit?.idKategori ?: db.push().key
        val oldName = dataEdit?.namaKategori
        val newName = nama
        val isDeactivated = (status == getString(R.string.msg_inactive))

        val kategori = ModelKategori(
            idKategori = id,
            namaKategori = nama,
            statusKategori = status,
            cabangKategori = cabang
        )

        if (id != null) {
            db.child(id).setValue(kategori).addOnSuccessListener {
                if (dataEdit != null && oldName != null) {
                    val dbProduk = FirebaseDatabase.getInstance().getReference("produk")
                    dbProduk.orderByChild("namaKategori").equalTo(oldName)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val updates = HashMap<String, Any>()
                                for (productSnap in snapshot.children) {
                                    val productId = productSnap.key ?: continue
                                    if (oldName != newName) {
                                        updates["$productId/namaKategori"] = newName
                                    }
                                    if (isDeactivated) {
                                        updates["$productId/statusProduk"] = getString(R.string.msg_inactive)
                                    }
                                }
                                if (updates.isNotEmpty()) {
                                    dbProduk.updateChildren(updates)
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
                Toast.makeText(this, getString(R.string.kategori_save_success), Toast.LENGTH_SHORT).show()
                finish() // Kembali ke halaman list
            }.addOnFailureListener {
                Toast.makeText(this, getString(R.string.msg_failed, it.message), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
