package com.citra.penjualan.kategori

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ActivityTambahKategoriBinding
import com.citra.penjualan.model.ModelKategori
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TambahKategoriActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTambahKategoriBinding
    private val db = FirebaseDatabase.getInstance().getReference("kategori")
    private val dbCabang = FirebaseDatabase.getInstance().getReference("cabang")
    private var dataEdit: ModelKategori? = null
    
    private val listCabang = ArrayList<String>()
    private lateinit var adapterCabang: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahKategoriBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Spinner
        adapterCabang = ArrayAdapter(this, R.layout.item_spinner_selected, listCabang)
        adapterCabang.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spinnerCabangKategori.adapter = adapterCabang

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
                
                adapterCabang.notifyDataSetChanged()

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
            chipStatus.text = dataEdit?.statusKategori
            btnSimpan.text = getString(R.string.kategori_update_btn)

            // Set selected branch in spinner
            val branchIndex = listCabang.indexOf(dataEdit?.cabangKategori)
            if (branchIndex != -1) {
                spinnerCabangKategori.setSelection(branchIndex)
            }
        }
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_confirm_title))
            .setMessage(getString(R.string.delete_confirm_msg))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                dataEdit?.idKategori?.let { id ->
                    db.child(id).removeValue().addOnSuccessListener {
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
        val cabang = binding.spinnerCabangKategori.selectedItem?.toString()?.trim() ?: ""
        val status = binding.chipStatus.text.toString()

        if (nama.isEmpty() || cabang.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_complete_all_data), Toast.LENGTH_SHORT).show()
            return
        }

        val id = dataEdit?.idKategori ?: db.push().key

        val kategori = ModelKategori(
            idKategori = id,
            namaKategori = nama,
            statusKategori = status,
            cabangKategori = cabang
        )

        if (id != null) {
            db.child(id).setValue(kategori).addOnSuccessListener {
                Toast.makeText(this, getString(R.string.kategori_save_success), Toast.LENGTH_SHORT).show()
                finish() // Kembali ke halaman list
            }.addOnFailureListener {
                Toast.makeText(this, getString(R.string.msg_failed, it.message), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
