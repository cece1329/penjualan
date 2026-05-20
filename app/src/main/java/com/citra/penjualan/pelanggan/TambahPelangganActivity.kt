package com.citra.penjualan.pelanggan

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.databinding.ActivityTambahPelangganBinding
import com.citra.penjualan.model.ModelPelanggan
import com.google.firebase.database.FirebaseDatabase

class TambahPelangganActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTambahPelangganBinding
    private val db = FirebaseDatabase.getInstance().getReference("pelanggan")
    private var dataEdit: ModelPelanggan? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahPelangganBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataEdit = intent.getParcelableExtra("DATA_PELANGGAN")

        if (dataEdit != null) {
            setupViewEdit()
        }

        binding.btnSimpanPelanggan.setOnClickListener {
            validateAndSave()
        }
    }

    private fun setupViewEdit() {
        binding.apply {
            etNamaPelanggan.setText(dataEdit?.namaPelanggan)
            etTeleponPelanggan.setText(dataEdit?.teleponPelanggan)
            etAlamatPelanggan.setText(dataEdit?.alamatPelanggan)
            btnSimpanPelanggan.text = "Update Data Pelanggan"
        }
    }

    private fun validateAndSave() {
        val nama = binding.etNamaPelanggan.text.toString().trim()
        val telp = binding.etTeleponPelanggan.text.toString().trim()
        val alamat = binding.etAlamatPelanggan.text.toString().trim()

        if (nama.isEmpty() || telp.isEmpty() || alamat.isEmpty()) {
            Toast.makeText(this, "Lengkapi semua data ya!", Toast.LENGTH_SHORT).show()
            return
        }

        val id = dataEdit?.idPelanggan ?: db.push().key

        val pelanggan = ModelPelanggan(
            idPelanggan = id,
            namaPelanggan = nama,
            teleponPelanggan = telp,
            alamatPelanggan = alamat
        )

        if (id != null) {
            db.child(id).setValue(pelanggan).addOnSuccessListener {
                Toast.makeText(this, "Berhasil simpan data pelanggan!", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
