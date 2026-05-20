package com.citra.penjualan.transaksi

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.databinding.ActivityTransaksiBinding
import com.citra.penjualan.model.ModelProduk
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransaksiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransaksiBinding
    private val dbProduk = FirebaseDatabase.getInstance().getReference("produk")
    private val dbTransaksi = FirebaseDatabase.getInstance().getReference("transaksi")

    private val produkList = ArrayList<ModelProduk>()
    private var selectedProduk: ModelProduk? = null
    private var totalHarga = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransaksiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadProdukData()

        binding.etJumlah.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateTotal()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnBayar.setOnClickListener {
            saveTransaction()
        }
    }

    private fun loadProdukData() {
        dbProduk.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                produkList.clear()
                val names = ArrayList<String>()
                
                for (data in snapshot.children) {
                    val produk = data.getValue(ModelProduk::class.java)
                    if (produk != null) {
                        produk.idProduk = data.key
                        produkList.add(produk)
                        names.add("${produk.namaProduk} - Rp ${produk.hargaProduk}")
                    }
                }

                if (produkList.isEmpty()) {
                    names.add("Tidak ada produk tersedia")
                }

                val adapter = ArrayAdapter(this@TransaksiActivity, android.R.layout.simple_spinner_item, names)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerProduk.adapter = adapter

                binding.spinnerProduk.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (produkList.isNotEmpty() && position < produkList.size) {
                            selectedProduk = produkList[position]
                            calculateTotal()
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TransaksiActivity, "Gagal mengambil data produk: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun calculateTotal() {
        val qtyStr = binding.etJumlah.text.toString().trim()
        val qty = qtyStr.toIntOrNull() ?: 0
        val price = selectedProduk?.hargaProduk ?: 0
        totalHarga = qty * price
        binding.tvTotalHarga.text = "Rp. $totalHarga"
    }

    private fun saveTransaction() {
        val qtyStr = binding.etJumlah.text.toString().trim()
        val qty = qtyStr.toIntOrNull() ?: 0

        if (selectedProduk == null || qty <= 0) {
            Toast.makeText(this, "Silakan pilih produk dan jumlah yang valid", Toast.LENGTH_SHORT).show()
            return
        }

        val id = dbTransaksi.push().key
        val tanggal = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        val data = hashMapOf(
            "idTransaksi" to id,
            "namaProduk" to selectedProduk?.namaProduk,
            "jumlah" to qty,
            "totalHarga" to totalHarga,
            "tanggal" to tanggal
        )

        if (id != null) {
            // Update stok produk di database
            val newStok = (selectedProduk?.stokProduk ?: 0) - qty
            if (newStok < 0) {
                Toast.makeText(this, "Stok tidak mencukupi! Stok saat ini: ${selectedProduk?.stokProduk}", Toast.LENGTH_SHORT).show()
                return
            }

            dbTransaksi.child(id).setValue(data).addOnSuccessListener {
                // Kurangi stok produk
                selectedProduk?.idProduk?.let { key ->
                    dbProduk.child(key).child("stokProduk").setValue(newStok)
                }
                Toast.makeText(this, "Transaksi berhasil disimpan!", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Gagal simpan transaksi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
