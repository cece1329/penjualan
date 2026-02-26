package com.citra.penjualan.kategori

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.FirebaseDatabase

class TambahKategoriActivity : AppCompatActivity() {

    private val database = FirebaseDatabase.getInstance()
    private val myRef = database.getReference("com/indah/penjualan/kategori")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mod_kategori)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val etNamaKategori = findViewById<EditText>(R.id.etNamaKategori)
        val spinnerStatus = findViewById<Spinner>(R.id.spinnerStatus)
        val btnSimpan = findViewById<MaterialButton>(R.id.btnSimpan)

        val listStatus = arrayOf("Aktif", "Tidak Aktif")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listStatus)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = adapter

        btnBack.setOnClickListener { finish() }

        btnSimpan.setOnClickListener {
            val namaKategori = etNamaKategori.text.toString().trim()
            val statusTerpilih = spinnerStatus.selectedItem.toString()

            if (namaKategori.isEmpty()) {
                etNamaKategori.error = "Nama kategori tidak boleh kosong"
            } else {
                simpanData(namaKategori, statusTerpilih)
            }
        }
    }

    private fun simpanData(nama: String, status: String) {
        val idKategori = myRef.push().key ?: return

        val dataKategori = mapOf(
            "id" to idKategori,
            "nama" to nama,
            "status" to status
        )

        myRef.child(idKategori).setValue(dataKategori)
            .addOnSuccessListener {
                Toast.makeText(this, "Data $nama berhasil disimpan", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menyimpan: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}