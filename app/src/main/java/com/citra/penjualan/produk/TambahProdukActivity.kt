package com.citra.penjualan.produk

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.databinding.ActivityTambahProdukBinding
import com.citra.penjualan.model.ModelProduk
import com.google.firebase.database.FirebaseDatabase

class TambahProdukActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTambahProdukBinding
    private val db = FirebaseDatabase.getInstance().getReference("produk")
    private var dataEdit: ModelProduk? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahProdukBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cek apakah ada data yang dikirim (berarti mode EDIT)
        dataEdit = intent.getParcelableExtra("DATA_PRODUK")

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
        binding.btnSimpanProduk.setOnClickListener {
            validateAndSave()
        }
    }

    private fun setupViewEdit() {
        binding.apply {
            inNamaProduk.setText(dataEdit?.namaProduk)
            inHargaBeli.setText(dataEdit?.hargaProduk?.toString() ?: "")
            inStokProduk.setText(dataEdit?.stokProduk?.toString() ?: "")
            inCabangProduk.setText(dataEdit?.cabangProduk ?: "")
            chipStatus.text = dataEdit?.statusProduk
            btnSimpanProduk.text = "Update Data Produk"
        }
    }

    private fun validateAndSave() {
        val nama = binding.inNamaProduk.text.toString().trim()
        val hargaStr = binding.inHargaBeli.text.toString().trim()
        val stokStr = binding.inStokProduk.text.toString().trim()
        val cabang = binding.inCabangProduk.text.toString().trim()
        val status = binding.chipStatus.text.toString()

        // Validasi jika ada field yang kosong
        if (nama.isEmpty() || hargaStr.isEmpty() || stokStr.isEmpty() || cabang.isEmpty()) {
            Toast.makeText(this, "Lengkapi semua data ya!", Toast.LENGTH_SHORT).show()
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
            statusProduk = status
        )

        // Menyimpan atau Update ke Firebase Database
        if (id != null) {
            db.child(id).setValue(produk).addOnSuccessListener {
                Toast.makeText(this, "Berhasil simpan data produk!", Toast.LENGTH_SHORT).show()
                finish() // Kembali ke halaman sebelumnya
            }.addOnFailureListener {
                Toast.makeText(this, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}