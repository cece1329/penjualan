package com.citra.penjualan.cabang

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ActivityTambahCabangBinding
import com.citra.penjualan.model.ModelCabang
import com.google.firebase.database.FirebaseDatabase

class TambahCabangActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTambahCabangBinding
    private val db = FirebaseDatabase.getInstance().getReference("cabang")
    private var dataEdit: ModelCabang? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahCabangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataEdit = intent.getParcelableExtra("DATA_CABANG")

        if (dataEdit != null) {
            setupViewEdit()
            binding.btnHapusCabang.visibility = android.view.View.VISIBLE
            binding.btnHapusCabang.setOnClickListener {
                showDeleteConfirmation()
            }
        } else {
            binding.btnHapusCabang.visibility = android.view.View.GONE
        }

        binding.btnSimpanCabang.setOnClickListener {
            validateAndSave()
        }
    }

    private fun setupViewEdit() {
        binding.apply {
            etNamaCabang.setText(dataEdit?.namaCabang)
            etKotaCabang.setText(dataEdit?.kotaCabang)
            etAlamatCabang.setText(dataEdit?.alamatCabang)
            btnSimpanCabang.text = getString(R.string.cabang_update_btn)
        }
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.cabang_delete_title))
            .setMessage(getString(R.string.cabang_delete_msg))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                dataEdit?.idCabang?.let { id ->
                    db.child(id).removeValue().addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.cabang_delete_success), Toast.LENGTH_SHORT).show()
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this, getString(R.string.msg_failed, it.message), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun validateAndSave() {
        val nama = binding.etNamaCabang.text.toString().trim()
        val kota = binding.etKotaCabang.text.toString().trim()
        val alamat = binding.etAlamatCabang.text.toString().trim()

        if (nama.isEmpty() || kota.isEmpty() || alamat.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_complete_all_data), Toast.LENGTH_SHORT).show()
            return
        }

        val id = dataEdit?.idCabang ?: db.push().key

        val cabang = ModelCabang(
            idCabang = id,
            namaCabang = nama,
            kotaCabang = kota,
            alamatCabang = alamat
        )

        if (id != null) {
            db.child(id).setValue(cabang).addOnSuccessListener {
                Toast.makeText(this, getString(R.string.cabang_save_success), Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, getString(R.string.msg_failed, it.message), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
