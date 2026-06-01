package com.citra.penjualan.cabang

import android.os.Bundle
import android.widget.Toast
import com.citra.penjualan.BaseActivity
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ActivityTambahCabangBinding
import com.citra.penjualan.model.ModelCabang
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

class TambahCabangActivity : BaseActivity() {

    private lateinit var binding: ActivityTambahCabangBinding
    private val db = FirebaseDatabase.getInstance().getReference("cabang")
    private var dataEdit: ModelCabang? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahCabangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataEdit = intent.getParcelableExtra("DATA_CABANG")

        if (dataEdit != null) {
            setupViewEdit()
            binding.btnHapusCabang.visibility = android.view.View.VISIBLE
            binding.btnHapusCabang.setOnClickListener {
                showDeleteConfirmation()
            }
        } else {
            binding.btnHapusCabang.visibility = android.view.View.GONE
        }

        binding.btnSimpanCabang.setOnClickListener {
            validateAndSave()
        }
    }

    private fun setupViewEdit() {
        binding.apply {
            etNamaCabang.setText(dataEdit?.namaCabang)
            etKotaCabang.setText(dataEdit?.kotaCabang)
            etAlamatCabang.setText(dataEdit?.alamatCabang)
            btnSimpanCabang.text = getString(R.string.cabang_update_btn)
        }
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.cabang_delete_title))
            .setMessage(getString(R.string.cabang_delete_msg))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                dataEdit?.idCabang?.let { id ->
                    val oldBranchName = dataEdit?.namaCabang
                    db.child(id).removeValue().addOnSuccessListener {
                        if (oldBranchName != null) {
                            // 1. Cabang dihapus dari daftar cabang Kategori
                            val dbKategori = FirebaseDatabase.getInstance().getReference("kategori")
                            dbKategori.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val updates = HashMap<String, Any>()
                                    for (katSnap in snapshot.children) {
                                        val katId = katSnap.key ?: continue
                                        val cabangKat = katSnap.child("cabangKategori").value?.toString() ?: continue
                                        if (cabangKat.isNotEmpty() && cabangKat != "Semua Cabang") {
                                            val branches = cabangKat.split(",").map { it.trim() }
                                            if (branches.contains(oldBranchName)) {
                                                val filteredBranches = branches.filter { it != oldBranchName }
                                                updates["$katId/cabangKategori"] = if (filteredBranches.isEmpty()) "Belum Ada Cabang" else filteredBranches.joinToString(", ")
                                            }
                                        }
                                    }
                                    if (updates.isNotEmpty()) {
                                        dbKategori.updateChildren(updates)
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            })

                            // 2. Cabang dihapus dari daftar cabang Produk. Jika kosong, dinonaktifkan
                            val dbProduk = FirebaseDatabase.getInstance().getReference("produk")
                            dbProduk.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val updates = HashMap<String, Any>()
                                    for (prodSnap in snapshot.children) {
                                        val prodId = prodSnap.key ?: continue
                                        val cabangProd = prodSnap.child("cabangProduk").value?.toString() ?: continue
                                        if (cabangProd.isNotEmpty() && cabangProd != "Semua Cabang") {
                                            val branches = cabangProd.split(",").map { it.trim() }
                                            if (branches.contains(oldBranchName)) {
                                                val filteredBranches = branches.filter { it != oldBranchName }
                                                if (filteredBranches.isEmpty()) {
                                                    updates["$prodId/cabangProduk"] = "Belum Ada Cabang"
                                                    updates["$prodId/statusProduk"] = getString(R.string.msg_inactive)
                                                } else {
                                                    updates["$prodId/cabangProduk"] = filteredBranches.joinToString(", ")
                                                }
                                            }
                                        }
                                    }
                                    if (updates.isNotEmpty()) {
                                        dbProduk.updateChildren(updates)
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            })

                            // 3. Karyawan di cabang tersebut dinonaktifkan
                            val dbPegawai = FirebaseDatabase.getInstance().getReference("pegawai")
                            dbPegawai.orderByChild("cabangPegawai").equalTo(oldBranchName)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val updates = HashMap<String, Any>()
                                        for (pegSnap in snapshot.children) {
                                            val pegId = pegSnap.key ?: continue
                                            updates["$pegId/cabangPegawai"] = "Belum Ada Cabang"
                                            updates["$pegId/statusPegawai"] = getString(R.string.msg_inactive)
                                        }
                                        if (updates.isNotEmpty()) {
                                            dbPegawai.updateChildren(updates)
                                        }
                                    }
                                    override fun onCancelled(error: DatabaseError) {}
                                })
                        }
                        Toast.makeText(this, getString(R.string.cabang_delete_success), Toast.LENGTH_SHORT).show()
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this, getString(R.string.msg_failed, it.message), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun validateAndSave() {
        val nama = binding.etNamaCabang.text.toString().trim()
        val kota = binding.etKotaCabang.text.toString().trim()
        val alamat = binding.etAlamatCabang.text.toString().trim()

        if (nama.isEmpty() || kota.isEmpty() || alamat.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_complete_all_data), Toast.LENGTH_SHORT).show()
            return
        }

        val id = dataEdit?.idCabang ?: db.push().key
        val oldBranchName = dataEdit?.namaCabang
        val newBranchName = nama

        val cabang = ModelCabang(
            idCabang = id,
            namaCabang = nama,
            kotaCabang = kota,
            alamatCabang = alamat
        )

        if (id != null) {
            db.child(id).setValue(cabang).addOnSuccessListener {
                if (dataEdit != null && oldBranchName != null && oldBranchName != newBranchName) {
                    // 1. Update nama cabang di Kategori
                    val dbKategori = FirebaseDatabase.getInstance().getReference("kategori")
                    dbKategori.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val updates = HashMap<String, Any>()
                            for (katSnap in snapshot.children) {
                                val katId = katSnap.key ?: continue
                                val cabangKat = katSnap.child("cabangKategori").value?.toString() ?: continue
                                if (cabangKat.isNotEmpty() && cabangKat != "Semua Cabang") {
                                    val branches = cabangKat.split(",").map { it.trim() }
                                    if (branches.contains(oldBranchName)) {
                                        val updatedBranches = branches.map { if (it == oldBranchName) newBranchName else it }
                                        updates["$katId/cabangKategori"] = updatedBranches.joinToString(", ")
                                    }
                                }
                            }
                            if (updates.isNotEmpty()) {
                                dbKategori.updateChildren(updates)
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })

                    // 2. Update nama cabang di Produk
                    val dbProduk = FirebaseDatabase.getInstance().getReference("produk")
                    dbProduk.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val updates = HashMap<String, Any>()
                            for (prodSnap in snapshot.children) {
                                val prodId = prodSnap.key ?: continue
                                val cabangProd = prodSnap.child("cabangProduk").value?.toString() ?: continue
                                if (cabangProd.isNotEmpty() && cabangProd != "Semua Cabang") {
                                    val branches = cabangProd.split(",").map { it.trim() }
                                    if (branches.contains(oldBranchName)) {
                                        val updatedBranches = branches.map { if (it == oldBranchName) newBranchName else it }
                                        updates["$prodId/cabangProduk"] = updatedBranches.joinToString(", ")
                                    }
                                }
                            }
                            if (updates.isNotEmpty()) {
                                dbProduk.updateChildren(updates)
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })

                    // 3. Update nama cabang di Pegawai
                    val dbPegawai = FirebaseDatabase.getInstance().getReference("pegawai")
                    dbPegawai.orderByChild("cabangPegawai").equalTo(oldBranchName)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val updates = HashMap<String, Any>()
                                for (pegSnap in snapshot.children) {
                                    val pegId = pegSnap.key ?: continue
                                    updates["$pegId/cabangPegawai"] = newBranchName
                                }
                                if (updates.isNotEmpty()) {
                                    dbPegawai.updateChildren(updates)
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
                Toast.makeText(this, getString(R.string.cabang_save_success), Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, getString(R.string.msg_failed, it.message), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
