package com.citra.penjualan.produk

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.databinding.ActivityTambahProdukBinding // Pakai Binding biar elit
import com.citra.penjualan.model.ModelProduk
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class TambahProdukActivity : AppCompatActivity() {

    private val binding by lazy { ActivityTambahProdukBinding.inflate(layoutInflater) }
    private var imageUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            // Bonus: nampilin gambar yang dipilih biar user yakin
            // binding.imgPreview.setImageURI(uri)
            Toast.makeText(this, "Foto terpilih!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.boxFoto.setOnClickListener {
            getImage.launch("image/*")
        }

        binding.btnSimpanProduk.setOnClickListener {
            uploadData()
        }
    }

    private fun uploadData() {
        val nama = binding.inNamaProduk.text.toString()
        val harga = binding.inHargaBeli.text.toString().toIntOrNull() ?: 0

        if (nama.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Nama dan Foto wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = UUID.randomUUID().toString()
        val ref = storage.reference.child("produk/$fileName")

        ref.putFile(imageUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    simpanKeFirestore(nama, harga, uri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal upload foto", Toast.LENGTH_SHORT).show()
            }
    }

    private fun simpanKeFirestore(nama: String, harga: Int, fotoUrl: String) {
        val id = db.collection("produk").document().id
        val produk = ModelProduk(
            idProduk = id,
            namaProduk = nama,
            hargaProduk = harga,
            fotoProduk = fotoUrl,
            statusProduk = "Aktif"
        )

        db.collection("produk").document(id)
            .set(produk)
            .addOnSuccessListener {
                Toast.makeText(this, "Produk berhasil disimpan!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal simpan data", Toast.LENGTH_SHORT).show()
            }
    }
}