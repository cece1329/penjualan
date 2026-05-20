package com.citra.penjualan.akun

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.databinding.ActivityAkunBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AkunActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAkunBinding
    private val db = FirebaseDatabase.getInstance().getReference("profil")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAkunBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadProfile()

        binding.btnSimpanProfil.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadProfile() {
        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val namaPemilik = snapshot.child("namaPemilik").value?.toString() ?: ""
                    val namaToko = snapshot.child("namaToko").value?.toString() ?: ""
                    val emailToko = snapshot.child("emailToko").value?.toString() ?: ""

                    binding.etNamaPemilik.setText(namaPemilik)
                    binding.etNamaToko.setText(namaToko)
                    binding.etEmailToko.setText(emailToko)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AkunActivity, "Gagal memuat profil: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveProfile() {
        val namaPemilik = binding.etNamaPemilik.text.toString().trim()
        val namaToko = binding.etNamaToko.text.toString().trim()
        val emailToko = binding.etEmailToko.text.toString().trim()

        if (namaPemilik.isEmpty() || namaToko.isEmpty() || emailToko.isEmpty()) {
            Toast.makeText(this, "Lengkapi semua data ya!", Toast.LENGTH_SHORT).show()
            return
        }

        val data = hashMapOf(
            "namaPemilik" to namaPemilik,
            "namaToko" to namaToko,
            "emailToko" to emailToko
        )

        db.setValue(data).addOnSuccessListener {
            Toast.makeText(this, "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
