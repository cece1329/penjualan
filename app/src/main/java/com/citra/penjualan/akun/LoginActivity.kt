package com.citra.penjualan.akun

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import com.citra.penjualan.BaseActivity
import com.citra.penjualan.R
import com.citra.penjualan.beranda.cardActivity
import com.citra.penjualan.databinding.ActivityLoginBinding
import com.citra.penjualan.model.ModelPegawai
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var selectedRole = "pemilik"
    private val dbPegawai = FirebaseDatabase.getInstance().getReference("pegawai")
    private val dbProfil = FirebaseDatabase.getInstance().getReference("profil")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        if (sharedPref.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, cardActivity::class.java))
            finish()
            return
        }
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRoleSelection()
        binding.btnLogin.setOnClickListener { validateAndLogin() }
    }

    private fun setupRoleSelection() {
        binding.btnTabPemilik.setOnClickListener {
            selectedRole = "pemilik"
            binding.btnTabPemilik.setBackgroundResource(R.drawable.btn_simpan)
            binding.btnTabPemilik.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#BA68C8"))
            binding.btnTabPemilik.setTextColor(Color.WHITE)
            binding.btnTabKaryawan.setBackgroundColor(Color.TRANSPARENT)
            binding.btnTabKaryawan.setTextColor(Color.parseColor("#BA68C8"))
            binding.tvRoleTitle.text = getString(R.string.login_as_owner)
            binding.tvUsernameLabel.text = getString(R.string.login_username_owner)
            binding.etUsername.hint = getString(R.string.login_hint_owner)
            binding.etUsername.setText("")
            binding.etPassword.setText("")
        }
        binding.btnTabKaryawan.setOnClickListener {
            selectedRole = "karyawan"
            binding.btnTabKaryawan.setBackgroundResource(R.drawable.btn_simpan)
            binding.btnTabKaryawan.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#BA68C8"))
            binding.btnTabKaryawan.setTextColor(Color.WHITE)
            binding.btnTabPemilik.setBackgroundColor(Color.TRANSPARENT)
            binding.btnTabPemilik.setTextColor(Color.parseColor("#BA68C8"))
            binding.tvRoleTitle.text = getString(R.string.login_as_employee)
            binding.tvUsernameLabel.text = getString(R.string.login_username_employee)
            binding.etUsername.hint = getString(R.string.login_hint_employee)
            binding.etUsername.setText("")
            binding.etPassword.setText("")
        }
    }

    private fun validateAndLogin() {
        val usernameInput = binding.etUsername.text.toString().trim()
        val passwordInput = binding.etPassword.text.toString().trim()
        if (usernameInput.isEmpty() || passwordInput.isEmpty()) {
            Toast.makeText(this, getString(R.string.login_fill_all), Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedRole == "pemilik") {
            // Cek sandi pemilik dari database profil
            dbProfil.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sandiDb = snapshot.child("sandiPemilik").value?.toString() ?: "admin123"
                    val namaPemilik = snapshot.child("namaPemilik").value?.toString() ?: "Citra"

                    // Login menggunakan Nama Pemilik dari DB atau fallback standar
                    val isUsernameValid = usernameInput.equals(namaPemilik, ignoreCase = true) ||
                                         usernameInput.lowercase() == "admin" ||
                                         usernameInput.lowercase() == "pemilik"
                    
                    if (isUsernameValid && passwordInput == sandiDb) {
                        saveSession("pemilik", namaPemilik, "-", "Owner/Pemilik", "", "")
                        Toast.makeText(this@LoginActivity, getString(R.string.login_welcome_owner), Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, cardActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, getString(R.string.login_wrong_owner), Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } else {
            dbPegawai.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var matchedPegawai: ModelPegawai? = null
                    for (data in snapshot.children) {
                        val pegawai = data.getValue(ModelPegawai::class.java) ?: continue
                        pegawai.idPegawai = data.key
                        pegawai.cabangPegawai = firstNotBlank(
                            pegawai.cabangPegawai,
                            data.child("cabangPegawai").value?.toString(),
                            data.child("cabang").value?.toString(),
                            data.child("namaCabang").value?.toString()
                        )
                        val nameMatch = pegawai.namaPegawai?.equals(usernameInput, ignoreCase = true) == true
                        val phoneMatch = pegawai.teleponPegawai == usernameInput
                        if ((nameMatch || phoneMatch) && pegawai.passwordPegawai == passwordInput) {
                            matchedPegawai = pegawai; break
                        }
                    }
                    if (matchedPegawai != null) {
                        saveSession(
                            "karyawan",
                            matchedPegawai.namaPegawai ?: getString(R.string.login_tab_employee),
                            matchedPegawai.teleponPegawai ?: "-",
                            matchedPegawai.jabatanPegawai ?: getString(R.string.login_tab_employee),
                            matchedPegawai.idPegawai ?: "",
                            matchedPegawai.cabangPegawai ?: ""
                        )
                        Toast.makeText(this@LoginActivity, getString(R.string.login_welcome_employee, matchedPegawai.namaPegawai), Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, cardActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, getString(R.string.login_wrong_employee), Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@LoginActivity, getString(R.string.login_db_error, error.message), Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun saveSession(role: String, name: String, phone: String, jabatan: String, idPegawai: String, cabang: String) {
        getSharedPreferences("user_session", Context.MODE_PRIVATE).edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_role", role)
            putString("user_name", name)
            putString("user_phone", phone)
            putString("user_jabatan", jabatan)
            putString("user_id_pegawai", idPegawai)
            putString("user_cabang", cabang)
            apply()
        }
    }

    private fun firstNotBlank(vararg values: String?): String {
        return values.firstOrNull { !it.isNullOrBlank() }.orEmpty()
    }
}
