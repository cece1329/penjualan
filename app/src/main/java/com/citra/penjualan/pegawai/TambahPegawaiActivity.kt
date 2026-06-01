package com.citra.penjualan.pegawai

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import com.citra.penjualan.BaseActivity
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ActivityTambahPegawaiBinding
import com.citra.penjualan.model.ModelPegawai
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TambahPegawaiActivity : BaseActivity() {

    private lateinit var binding: ActivityTambahPegawaiBinding
    private val db = FirebaseDatabase.getInstance().getReference("pegawai")
    private val dbCabang = FirebaseDatabase.getInstance().getReference("cabang")
    private var dataEdit: ModelPegawai? = null
    
    private val listCabang = ArrayList<String>()
    private val listJabatan = arrayListOf("Kasir", "Gudang", "Admin", "Supervisor")
    private lateinit var adapterCabang: ArrayAdapter<String>
    private lateinit var adapterJabatan: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahPegawaiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataEdit = intent.getParcelableExtra("DATA_PEGAWAI")

        // Setup Spinner
        adapterCabang = ArrayAdapter(this, R.layout.item_spinner_selected, listCabang)
        adapterCabang.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spinnerCabangPegawai.adapter = adapterCabang

        adapterJabatan = ArrayAdapter(this, R.layout.item_spinner_selected, listJabatan)
        adapterJabatan.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spinnerJabatanPegawai.adapter = adapterJabatan

        // Load branches first
        loadBranches()

        // Logika ganti status via Chip (Pastikan ID chipStatus ada di activity_tambah_pegawai.xml)
        binding.chipStatus.setOnClickListener {
            if (binding.chipStatus.text == getString(R.string.msg_active)) {
                binding.chipStatus.text = getString(R.string.msg_inactive)
            } else {
                binding.chipStatus.text = getString(R.string.msg_active)
            }
        }

        binding.btnSimpanPegawai.setOnClickListener {
            validateAndSave()
        }

        // Fitur Hapus (Hanya muncul jika dalam mode Edit)
        if (dataEdit != null) {
            binding.btnHapusPegawai.visibility = View.VISIBLE
            binding.btnHapusPegawai.setOnClickListener {
                showDeleteConfirmation()
            }
        } else {
            binding.btnHapusPegawai.visibility = View.GONE
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
                Toast.makeText(this@TambahPegawaiActivity, "Gagal memuat cabang: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupViewEdit() {
        binding.apply {
            etNamaPegawai.setText(dataEdit?.namaPegawai)
            etTeleponPegawai.setText(dataEdit?.teleponPegawai)
            etPasswordPegawai.setText(dataEdit?.passwordPegawai)
            chipStatus.text = dataEdit?.statusPegawai ?: getString(R.string.msg_active)
            btnSimpanPegawai.text = getString(R.string.pegawai_update_btn)

            val jabatanIndex = listJabatan.indexOfFirst { it.equals(dataEdit?.jabatanPegawai, ignoreCase = true) }
            spinnerJabatanPegawai.setSelection(if (jabatanIndex >= 0) jabatanIndex else 0)

            // Set selected branch in spinner
            val branchIndex = listCabang.indexOf(dataEdit?.cabangPegawai)
            if (branchIndex != -1) {
                spinnerCabangPegawai.setSelection(branchIndex)
            }
        }
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.pegawai_delete_title))
            .setMessage(getString(R.string.pegawai_delete_msg))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                dataEdit?.idPegawai?.let { id ->
                    db.child(id).removeValue().addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.pegawai_delete_success), Toast.LENGTH_SHORT).show()
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this, getString(R.string.pegawai_delete_failed, it.message), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun validateAndSave() {
        val nama = binding.etNamaPegawai.text.toString().trim()
        val jabatan = binding.spinnerJabatanPegawai.selectedItem?.toString()?.trim() ?: ""
        val telp = binding.etTeleponPegawai.text.toString().trim()
        val password = binding.etPasswordPegawai.text.toString().trim()
        val cabang = binding.spinnerCabangPegawai.selectedItem?.toString()?.trim() ?: ""
        val status = binding.chipStatus.text.toString()

        if (nama.isEmpty() || jabatan.isEmpty() || telp.isEmpty() || password.isEmpty() || cabang.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_complete_all_data), Toast.LENGTH_SHORT).show()
            return
        }

        val id = dataEdit?.idPegawai ?: db.push().key

        val pegawai = ModelPegawai(
            idPegawai = id,
            namaPegawai = nama,
            jabatanPegawai = jabatan,
            teleponPegawai = telp,
            passwordPegawai = password,
            cabangPegawai = cabang,
            statusPegawai = status
        )

        if (id != null) {
            db.child(id).setValue(pegawai).addOnSuccessListener {
                Toast.makeText(this, getString(R.string.pegawai_save_success), Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, getString(R.string.msg_failed, it.message), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
