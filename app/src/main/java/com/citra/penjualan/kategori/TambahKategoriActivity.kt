package com.citra.penjualan.kategori

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        adapterCabang = ArrayAdapter(this, android.R.layout.simple_spinner_item, listCabang)
        adapterCabang.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCabangKategori.adapter = adapterCabang

        // Cek apakah ada data yang dikirim (berarti mode EDIT)
        dataEdit = intent.getParcelableExtra("DATA_KATEGORI")

        // Load branches first
        loadBranches()

        // Logic ganti status via Chip
        binding.chipStatus.setOnClickListener {
            if (binding.chipStatus.text == "Aktif") {
                binding.chipStatus.text = "Tidak Aktif"
            } else {
                binding.chipStatus.text = "Aktif"
            }
        }

        // Tombol Simpan atau Update Data
        binding.btnSimpan.setOnClickListener {
            validateAndSave()
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
                    listCabang.add("Pusat")
                }
                
                adapterCabang.notifyDataSetChanged()

                if (dataEdit != null) {
                    setupViewEdit()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TambahKategoriActivity, "Gagal memuat cabang: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupViewEdit() {
        binding.apply {
            etNamaKategori.setText(dataEdit?.namaKategori)
            chipStatus.text = dataEdit?.statusKategori
            btnSimpan.text = "Update Data Kategori"

            // Set selected branch in spinner
            val branchIndex = listCabang.indexOf(dataEdit?.cabangKategori)
            if (branchIndex != -1) {
                spinnerCabangKategori.setSelection(branchIndex)
            }
        }
    }

    private fun validateAndSave() {
        val nama = binding.etNamaKategori.text.toString().trim()
        val cabang = binding.spinnerCabangKategori.selectedItem?.toString()?.trim() ?: ""
        val status = binding.chipStatus.text.toString()

        if (nama.isEmpty() || cabang.isEmpty()) {
            Toast.makeText(this, "Lengkapi semua data ya!", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Berhasil simpan data!", Toast.LENGTH_SHORT).show()
                finish() // Kembali ke halaman list
            }.addOnFailureListener {
                Toast.makeText(this, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}