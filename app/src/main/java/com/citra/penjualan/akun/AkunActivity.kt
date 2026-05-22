package com.citra.penjualan.akun

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.beranda.cardActivity
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

        setupProfileHeaderAndSession()

        binding.btnKeluar.setOnClickListener {
            performLogout()
        }
    }

    private fun setupProfileHeaderAndSession() {
        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val role = sharedPref.getString("user_role", "pemilik") ?: "pemilik"
        val name = sharedPref.getString("user_name", "Citra") ?: "Citra"
        val phone = sharedPref.getString("user_phone", "-") ?: "-"
        val jabatan = sharedPref.getString("user_jabatan", "Pemilik") ?: "Pemilik"

        // Set initials avatar dynamically with gradient
        binding.tvProfileName.text = name
        binding.tvAvatarInitials.text = name.take(1).uppercase()
        
        val colors = if (role == "pemilik") {
            intArrayOf(Color.parseColor("#BA68C8"), Color.parseColor("#AB47BC"))
        } else {
            intArrayOf(Color.parseColor("#CE93D8"), Color.parseColor("#BA68C8"))
        }
        val gd = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors)
        gd.cornerRadius = 36f
        binding.avatarFrame.background = gd

        // Handle Role Badge & Specific Views
        if (role == "pemilik") {
            binding.tvProfileRoleBadge.text = "PEMILIK / OWNER"
            binding.tvProfileRoleBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#BA68C8"))
            binding.layoutEmployeeDetails.visibility = View.GONE
            
            // Enable Toko Profile Fields for Pemilik
            binding.etNamaPemilik.isEnabled = true
            binding.etNamaToko.isEnabled = true
            binding.etEmailToko.isEnabled = true
            binding.btnSimpanProfil.visibility = View.VISIBLE
            
            // Load Toko Profile
            loadTokoProfile()
            
            binding.btnSimpanProfil.setOnClickListener {
                saveTokoProfile()
            }
        } else {
            binding.tvProfileRoleBadge.text = "KARYAWAN"
            binding.tvProfileRoleBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#AB47BC"))
            
            binding.layoutEmployeeDetails.visibility = View.VISIBLE
            binding.tvProfileJabatan.text = "Jabatan: $jabatan"
            binding.tvProfileTelepon.text = "No. Telepon: $phone"
            
            // Disable Toko Profile Fields and Hide Save Button for Employees
            binding.etNamaPemilik.isEnabled = false
            binding.etNamaToko.isEnabled = false
            binding.etEmailToko.isEnabled = false
            binding.btnSimpanProfil.visibility = View.GONE
            
            // Load Toko Profile as read-only info
            loadTokoProfile()
        }
    }

    private fun loadTokoProfile() {
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
                Toast.makeText(this@AkunActivity, "Gagal memuat profil toko: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveTokoProfile() {
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
            Toast.makeText(this, "Profil toko berhasil diperbarui!", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogout() {
        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        
        Toast.makeText(this, "Berhasil keluar dari akun", Toast.LENGTH_SHORT).show()
        
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finishAffinity()
    }
}
