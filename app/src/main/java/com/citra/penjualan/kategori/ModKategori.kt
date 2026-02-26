package com.citra.penjualan.kategori

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.citra.penjualan.R
import com.google.firebase.database.FirebaseDatabase

class ModKategori : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mod_kategori)

        // Inisialisasi View sesuai ID XML kamu
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val etNamaKategori = findViewById<EditText>(R.id.etNamaKategori)
        val spinnerStatus = findViewById<Spinner>(R.id.spinnerStatus)
        val btnSimpan = findViewById<AppCompatButton>(R.id.btnSimpan)

        // --- BAGIAN ISI SPINNER ---
        val listStatus = arrayOf("Aktif", "Tidak Aktif")
        // Gunakan layout standar biar gak pusing bikin file baru lagi
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, listStatus)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = adapterSpinner

        // Tombol Kembali
        btnBack.setOnClickListener { finish() }

        // Tombol Simpan
        btnSimpan.setOnClickListener {
            val nama = etNamaKategori.text.toString().trim()
            val status = spinnerStatus.selectedItem.toString()

            if (nama.isNotEmpty()) {
                simpanKeFirebase(nama, status)
            } else {
                etNamaKategori.error = "Nama kategori wajib diisi!"
            }
        }
    }

    private fun simpanKeFirebase(nama: String, status: String) {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("kategori")

        val key = myRef.push().key
        if (key != null) {
            val dataBaru = Kategori(key, nama, status)
            myRef.child(key).setValue(dataBaru)
                .addOnSuccessListener {
                    Toast.makeText(this, "Berhasil simpan $nama", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}