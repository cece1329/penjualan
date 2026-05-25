package com.citra.penjualan.akun

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.R
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
        binding.btnKeluar.setOnClickListener { performLogout() }
        
        // Memberikan radius pada tombol agar tidak kaku
        applyButtonStyle(binding.btnSimpanProfil, "#BA68C8")
        applyButtonStyle(binding.btnKeluar, "#F44336")
    }

    private fun applyButtonStyle(view: View, colorHex: String) {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = dp(12).toFloat()
        shape.setColor(Color.parseColor(colorHex))
        view.background = shape
    }

    private fun setupProfileHeaderAndSession() {
        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val role = sharedPref.getString("user_role", "pemilik") ?: "pemilik"
        val name = sharedPref.getString("user_name", "Citra") ?: "Citra"
        val phone = sharedPref.getString("user_phone", "-") ?: "-"
        val jabatan = sharedPref.getString("user_jabatan", "Pemilik") ?: "Pemilik"

        // Setup Profil Header: Logo Akun tanpa warna (tint) agar asli
        binding.tvAvatarInitials.visibility = View.GONE
        binding.avatarFrame.removeAllViews()
        val ivAvatar = ImageView(this).apply {
            setImageResource(R.drawable.acc)
        }
        binding.avatarFrame.addView(ivAvatar)

        binding.tvProfileName.text = name
        
        val colors = if (role == "pemilik") {
            intArrayOf(Color.parseColor("#BA68C8"), Color.parseColor("#AB47BC"))
        } else {
            intArrayOf(Color.parseColor("#CE93D8"), Color.parseColor("#BA68C8"))
        }
        val gd = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors)
        gd.cornerRadius = dp(50).toFloat() // Radius 50dp untuk FrameLayout 100dp agar bulat sempurna
        binding.avatarFrame.background = gd

        if (role == "pemilik") {
            binding.tvProfileRoleBadge.text = getString(R.string.akun_role_owner)
            binding.tvProfileRoleBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#BA68C8"))
            binding.layoutEmployeeDetails.visibility = View.GONE
            binding.etNamaPemilik.isEnabled = true
            binding.etNamaToko.isEnabled = true
            binding.etEmailToko.isEnabled = true
            binding.etSandiPemilik.isEnabled = true 
            binding.btnSimpanProfil.visibility = View.VISIBLE
            loadTokoProfile()
            binding.btnSimpanProfil.setOnClickListener { saveTokoProfile() }
        } else {
            binding.tvProfileRoleBadge.text = getString(R.string.akun_role_employee)
            binding.tvProfileRoleBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#AB47BC"))
            binding.layoutEmployeeDetails.visibility = View.VISIBLE
            binding.tvProfileJabatan.text = getString(R.string.akun_position, jabatan)
            binding.tvProfileTelepon.text = getString(R.string.akun_phone, phone)
            binding.etNamaPemilik.isEnabled = false
            binding.etNamaToko.isEnabled = false
            binding.etEmailToko.isEnabled = false
            binding.btnSimpanProfil.visibility = View.GONE
            loadTokoProfile()
        }
    }

    private fun loadTokoProfile() {
        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val namaPemilik = snapshot.child("namaPemilik").value?.toString() ?: ""
                    val namaToko = snapshot.child("namaToko").value?.toString() ?: ""
                    val sandi = snapshot.child("sandiPemilik").value?.toString() ?: "admin123"
                    val email = snapshot.child("emailToko").value?.toString() ?: ""
                    val alamat = snapshot.child("alamatToko").value?.toString() ?: ""
                    
                    binding.etNamaPemilik.setText(namaPemilik)
                    binding.etNamaToko.setText(namaToko)
                    binding.etEmailToko.setText(email)
                    try { binding.javaClass.getMethod("getEtAlamatToko").invoke(binding).let { (it as android.widget.EditText).setText(alamat) } } catch(e: Exception) {}

                    // Muat sandi ke field input
                    binding.etSandiPemilik.setText(sandi)
                    
                    if (namaPemilik.isNotBlank() && getSharedPreferences("user_session", Context.MODE_PRIVATE).getString("user_role", "pemilik") == "pemilik") {
                        binding.tvProfileName.text = namaPemilik
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AkunActivity, getString(R.string.akun_load_failed, error.message), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveTokoProfile() {
        val namaPemilik = binding.etNamaPemilik.text.toString().trim()
        val namaToko = binding.etNamaToko.text.toString().trim()
        val emailToko = binding.etEmailToko.text.toString().trim()
        val sandiBaru = binding.etSandiPemilik.text.toString().trim()
        val alamatToko = try { (binding.javaClass.getMethod("getEtAlamatToko").invoke(binding) as android.widget.EditText).text.toString().trim() } catch(e: Exception) { "" }

        if (namaPemilik.isEmpty() || namaToko.isEmpty() || emailToko.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_complete_all_data), Toast.LENGTH_SHORT).show()
            return
        }

        val data = hashMapOf(
            "namaPemilik" to namaPemilik,
            "namaToko" to namaToko,
            "emailToko" to emailToko,
            "alamatToko" to alamatToko,
            "sandiPemilik" to if (sandiBaru.isNotEmpty()) sandiBaru else "admin123"
        )

        db.setValue(data).addOnSuccessListener {
            getSharedPreferences("user_session", Context.MODE_PRIVATE).edit()
                .putString("user_name", namaPemilik)
                .apply()
            Toast.makeText(this, getString(R.string.akun_save_success), Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, getString(R.string.msg_failed, it.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogout() {
        getSharedPreferences("user_session", Context.MODE_PRIVATE).edit().clear().apply()
        Toast.makeText(this, getString(R.string.akun_logout_success), Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finishAffinity()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
