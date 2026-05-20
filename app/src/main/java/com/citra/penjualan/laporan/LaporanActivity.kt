package com.citra.penjualan.laporan

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.citra.penjualan.databinding.ActivityLaporanBinding
import com.citra.penjualan.model.ModelTransaksi
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LaporanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLaporanBinding
    private lateinit var adapter: LaporanAdapter
    private val db = FirebaseDatabase.getInstance().getReference("transaksi")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLaporanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = LaporanAdapter(arrayListOf())
        binding.recyclerTransaksi.apply {
            layoutManager = LinearLayoutManager(this@LaporanActivity)
            adapter = this@LaporanActivity.adapter
        }

        fetchLaporan()
    }

    private fun fetchLaporan() {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = ArrayList<ModelTransaksi>()
                var grandTotal = 0
                for (data in snapshot.children) {
                    val t = data.getValue(ModelTransaksi::class.java)
                    if (t != null) {
                        t.idTransaksi = data.key
                        list.add(t)
                        grandTotal += t.totalHarga
                    }
                }
                adapter.updateData(list)
                binding.tvTotalPenjualan.text = "Rp. $grandTotal"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LaporanActivity, "Gagal memuat laporan: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
