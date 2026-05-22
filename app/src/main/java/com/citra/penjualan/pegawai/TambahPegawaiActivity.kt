package com.citra.penjualan.pegawai

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.databinding.ActivityTambahPegawaiBinding
import com.citra.penjualan.model.ModelPegawai
import com.google.firebase.database.FirebaseDatabase

class TambahPegawaiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTambahPegawaiBinding
    private val db = FirebaseDatabase.getInstance().getReference("pegawai")
    private var dataEdit: ModelPegawai? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahPegawaiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataEdit = intent.getParcelableExtra("DATA_PEGAWAI")

        if (dataEdit != null) {
            setupViewEdit()
        }

        binding.btnSimpanPegawai.setOnClickListener {
            validateAndSave()
        }
    }

    private fun setupViewEdit() {
        binding.apply {
            etNamaPegawai.setText(dataEdit?.namaPegawai)
            etJabatanPegawai.setText(dataEdit?.jabatanPegawai)
            etTeleponPegawai.setText(dataEdit?.teleponPegawai)
            etPasswordPegawai.setText(dataEdit?.passwordPegawai)
            btnSimpanPegawai.text = "Update Data Pegawai"
        }
    }

    private fun validateAndSave() {
        val nama = binding.etNamaPegawai.text.toString().trim()
        val jabatan = binding.etJabatanPegawai.text.toString().trim()
        val telp = binding.etTeleponPegawai.text.toString().trim()
        val password = binding.etPasswordPegawai.text.toString().trim()

        if (nama.isEmpty() || jabatan.isEmpty() || telp.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Lengkapi semua data ya!", Toast.LENGTH_SHORT).show()
            return
        }

        val id = dataEdit?.idPegawai ?: db.push().key

        val pegawai = ModelPegawai(
            idPegawai = id,
            namaPegawai = nama,
            jabatanPegawai = jabatan,
            teleponPegawai = telp,
            passwordPegawai = password
        )

        if (id != null) {
            db.child(id).setValue(pegawai).addOnSuccessListener {
                Toast.makeText(this, "Berhasil simpan data pegawai!", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
