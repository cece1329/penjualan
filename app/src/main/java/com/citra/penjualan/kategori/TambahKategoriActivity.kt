package com.citra.penjualan.kategori

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.databinding.ActivityTambahKategoriBinding
import com.citra.penjualan.model.ModelKategori
import com.google.firebase.database.FirebaseDatabase

class TambahKategoriActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTambahKategoriBinding
    private val db = FirebaseDatabase.getInstance().getReference("kategori")
    private var dataEdit: ModelKategori? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahKategoriBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cek apakah ada data yang dikirim (berarti mode EDIT)
        dataEdit = intent.getParcelableExtra("DATA_KATEGORI")

        if (dataEdit != null) {
            setupViewEdit()
        }

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

    private fun setupViewEdit() {
        binding.apply {
            etNamaKategori.setText(dataEdit?.namaKategori)
            etCabangKategori.setText(dataEdit?.cabangKategori)
            chipStatus.text = dataEdit?.statusKategori
            btnSimpan.text = "Update Data Kategori"
        }
    }

    private fun validateAndSave() {
        val nama = binding.etNamaKategori.text.toString().trim()
        val cabang = binding.etCabangKategori.text.toString().trim()
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