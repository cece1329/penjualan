package com.citra.penjualan.pelanggan

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ActivityTambahPelangganBinding
import com.citra.penjualan.model.ModelPelanggan
import com.google.firebase.database.FirebaseDatabase

class TambahPelangganActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTambahPelangganBinding
    private val db = FirebaseDatabase.getInstance().getReference("pelanggan")
    private var dataEdit: ModelPelanggan? = null
    private val jenisPelangganOptions = listOf("Umum", "Khusus", "Loyal", "VIP")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahPelangganBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val jenisAdapter = ArrayAdapter(this, R.layout.item_spinner_selected, jenisPelangganOptions)
        jenisAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spinnerJenisPelanggan.adapter = jenisAdapter

        dataEdit = intent.getParcelableExtra("DATA_PELANGGAN")

        if (dataEdit != null) {
            setupViewEdit()
            binding.btnHapusPelanggan.visibility = android.view.View.VISIBLE
            binding.btnHapusPelanggan.setOnClickListener {
                showDeleteConfirmation()
            }
        } else {
            binding.btnHapusPelanggan.visibility = android.view.View.GONE
        }

        binding.btnSimpanPelanggan.setOnClickListener {
            validateAndSave()
        }
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.pelanggan_delete_title))
            .setMessage(getString(R.string.pelanggan_delete_msg))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                dataEdit?.idPelanggan?.let { id ->
                    db.child(id).removeValue().addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.pelanggan_delete_success), Toast.LENGTH_SHORT).show()
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this, getString(R.string.msg_failed, it.message), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun setupViewEdit() {
        binding.apply {
            etNamaPelanggan.setText(dataEdit?.namaPelanggan)
            etTeleponPelanggan.setText(dataEdit?.teleponPelanggan)
            etAlamatPelanggan.setText(dataEdit?.alamatPelanggan)
            val jenisIndex = jenisPelangganOptions.indexOf(dataEdit?.jenisPelanggan ?: "Umum")
            spinnerJenisPelanggan.setSelection(if (jenisIndex >= 0) jenisIndex else 0)
            btnSimpanPelanggan.text = getString(R.string.pelanggan_update_btn)
        }
    }

    private fun validateAndSave() {
        val nama = binding.etNamaPelanggan.text.toString().trim()
        val telp = binding.etTeleponPelanggan.text.toString().trim()
        val alamat = binding.etAlamatPelanggan.text.toString().trim()
        val jenis = binding.spinnerJenisPelanggan.selectedItem?.toString() ?: "Umum"

        if (nama.isEmpty() || telp.isEmpty() || alamat.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_complete_all_data), Toast.LENGTH_SHORT).show()
            return
        }

        val id = dataEdit?.idPelanggan ?: db.push().key

        val pelanggan = ModelPelanggan(
            idPelanggan = id,
            namaPelanggan = nama,
            teleponPelanggan = telp,
            alamatPelanggan = alamat,
            jenisPelanggan = jenis
        )

        if (id != null) {
            db.child(id).setValue(pelanggan).addOnSuccessListener {
                Toast.makeText(this, getString(R.string.pelanggan_save_success), Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, getString(R.string.msg_failed, it.message), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
