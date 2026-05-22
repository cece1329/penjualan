package com.citra.penjualan.akun

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.R
import com.citra.penjualan.beranda.cardActivity
import com.citra.penjualan.databinding.ActivityLoginBinding
import com.citra.penjualan.model.ModelPegawai
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var selectedRole = "pemilik" // Default role
    private val dbPegawai = FirebaseDatabase.getInstance().getReference("pegawai")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user is already logged in
        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
        if (isLoggedIn) {
            startActivity(Intent(this, cardActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRoleSelection()

        binding.btnLogin.setOnClickListener {
            validateAndLogin()
        }
    }

    private fun setupRoleSelection() {
        binding.btnTabPemilik.setOnClickListener {
            selectedRole = "pemilik"
            
            // Highlight Pemilik Tab
            binding.btnTabPemilik.setBackgroundResource(R.drawable.btn_simpan)
            binding.btnTabPemilik.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#BA68C8"))
            binding.btnTabPemilik.setTextColor(Color.WHITE)
            
            // Unhighlight Karyawan Tab
            binding.btnTabKaryawan.setBackgroundColor(Color.TRANSPARENT)
            binding.btnTabKaryawan.setTextColor(Color.parseColor("#BA68C8"))
            
            // Update labels and hints
            binding.tvRoleTitle.text = "Masuk sebagai Pemilik"
            binding.tvUsernameLabel.text = "Username Pemilik"
            binding.etUsername.hint = "Contoh: pemilik / admin"
            binding.etUsername.setText("")
            binding.etPassword.setText("")
        }

        binding.btnTabKaryawan.setOnClickListener {
            selectedRole = "karyawan"
            
            // Highlight Karyawan Tab
            binding.btnTabKaryawan.setBackgroundResource(R.drawable.btn_simpan)
            binding.btnTabKaryawan.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#BA68C8"))
            binding.btnTabKaryawan.setTextColor(Color.WHITE)
            
            // Unhighlight Pemilik Tab
            binding.btnTabPemilik.setBackgroundColor(Color.TRANSPARENT)
            binding.btnTabPemilik.setTextColor(Color.parseColor("#BA68C8"))
            
            // Update labels and hints
            binding.tvRoleTitle.text = "Masuk sebagai Karyawan"
            binding.tvUsernameLabel.text = "Nama / Telepon Karyawan"
            binding.etUsername.hint = "Masukkan nama atau no telepon"
            binding.etUsername.setText("")
            binding.etPassword.setText("")
        }
    }

    private fun validateAndLogin() {
        val usernameInput = binding.etUsername.text.toString().trim()
        val passwordInput = binding.etPassword.text.toString().trim()

        if (usernameInput.isEmpty() || passwordInput.isEmpty()) {
            Toast.makeText(this, "Harap lengkapi semua form input ya!", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedRole == "pemilik") {
            // Pemilik hardcoded / simple credentials for default safety
            if ((usernameInput.lowercase() == "pemilik" || usernameInput.lowercase() == "admin") && passwordInput == "admin123") {
                saveSession(
                    role = "pemilik",
                    name = "Citra (Pemilik)",
                    phone = "-",
                    jabatan = "Owner/Pemilik"
                )
                Toast.makeText(this, "Selamat datang Pemilik!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, cardActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Username atau Password Pemilik salah!", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Fetch Karyawan credentials from Firebase Realtime Database
            dbPegawai.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var loginSuccess = false
                    var matchedPegawai: ModelPegawai? = null
                    
                    for (data in snapshot.children) {
                        val pegawai = data.getValue(ModelPegawai::class.java)
                        if (pegawai != null) {
                            val nameMatch = pegawai.namaPegawai?.equals(usernameInput, ignoreCase = true) == true
                            val phoneMatch = pegawai.teleponPegawai == usernameInput
                            val passwordMatch = pegawai.passwordPegawai == passwordInput
                            
                            if ((nameMatch || phoneMatch) && passwordMatch) {
                                loginSuccess = true
                                matchedPegawai = pegawai
                                break
                            }
                        }
                    }
                    
                    if (loginSuccess && matchedPegawai != null) {
                        saveSession(
                            role = "karyawan",
                            name = matchedPegawai.namaPegawai ?: "Karyawan",
                            phone = matchedPegawai.teleponPegawai ?: "-",
                            jabatan = matchedPegawai.jabatanPegawai ?: "Karyawan"
                        )
                        Toast.makeText(this@LoginActivity, "Selamat datang, ${matchedPegawai.namaPegawai}!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, cardActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Nama/Telepon atau Password Karyawan salah!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@LoginActivity, "Error database: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun saveSession(role: String, name: String, phone: String, jabatan: String) {
        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_role", role)
            putString("user_name", name)
            putString("user_phone", phone)
            putString("user_jabatan", jabatan)
            apply()
        }
    }
}
