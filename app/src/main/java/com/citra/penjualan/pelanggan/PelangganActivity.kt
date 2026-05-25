package com.citra.penjualan.pelanggan

import android.content.Intent
import android.os.Bundle
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ActivityPelangganBinding
import com.citra.penjualan.model.ModelPelanggan
import com.google.android.material.chip.Chip
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PelangganActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPelangganBinding
    private lateinit var adapter: PelangganAdapter
    private val db = FirebaseDatabase.getInstance().getReference("pelanggan")
    private val listPelangganFull = ArrayList<ModelPelanggan>()
    private val jenisOptions = listOf("Semua", "Umum", "Khusus", "Loyal", "VIP")
    private var selectedJenis = "Semua"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPelangganBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = PelangganAdapter(arrayListOf())
        binding.recyclerPelanggan.apply {
            layoutManager = LinearLayoutManager(this@PelangganActivity)
            adapter = this@PelangganActivity.adapter
        }

        binding.fabTambahPelanggan.setOnClickListener {
            startActivity(Intent(this, TambahPelangganActivity::class.java))
        }

        setupJenisChips()

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fetchPelanggan()
    }

    private fun fetchPelanggan() {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = ArrayList<ModelPelanggan>()
                for (data in snapshot.children) {
                    val p = data.getValue(ModelPelanggan::class.java)
                    if (p != null) {
                        p.idPelanggan = data.key
                        list.add(p)
                    }
                }
                listPelangganFull.clear()
                listPelangganFull.addAll(list)
                filter(binding.etSearch.text?.toString() ?: "")
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PelangganActivity, getString(R.string.pelanggan_load_failed, error.message), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filter(query: String) {
        val q = query.trim()
        val filteredList = listPelangganFull.filter {
            val jenis = it.jenisPelanggan ?: "Umum"
            val matchesJenis = selectedJenis == "Semua" || jenis.equals(selectedJenis, ignoreCase = true)
            val matchesSearch = q.isEmpty() ||
                it.namaPelanggan?.contains(q, ignoreCase = true) == true ||
                    it.teleponPelanggan?.contains(q, ignoreCase = true) == true ||
                    it.alamatPelanggan?.contains(q, ignoreCase = true) == true ||
                    jenis.contains(q, ignoreCase = true)
            matchesJenis && matchesSearch
        }
        adapter.updateData(filteredList)
    }

    private fun setupJenisChips() {
        binding.chipGroupJenis.removeAllViews()
        jenisOptions.forEach { jenis ->
            val chip = Chip(this)
            chip.text = jenis
            chip.isCheckable = true
            chip.isChecked = jenis == selectedJenis
            chip.chipCornerRadius = dp(16).toFloat()
            chip.chipMinHeight = dp(40).toFloat()
            chip.setTextColor(Color.parseColor("#4A2B66"))
            chip.chipStrokeWidth = 1f
            chip.chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#D7C5E8"))
            chip.chipBackgroundColor = ColorStateList.valueOf(
                Color.parseColor(if (jenis == selectedJenis) "#EAD9F7" else "#FFFFFF")
            )
            chip.setOnClickListener {
                selectedJenis = jenis
                refreshJenisChips()
                filter(binding.etSearch.text?.toString() ?: "")
            }
            binding.chipGroupJenis.addView(chip)
        }
    }

    private fun refreshJenisChips() {
        for (i in 0 until binding.chipGroupJenis.childCount) {
            val chip = binding.chipGroupJenis.getChildAt(i) as? Chip ?: continue
            val checked = chip.text.toString() == selectedJenis
            chip.isChecked = checked
            chip.chipBackgroundColor = ColorStateList.valueOf(
                Color.parseColor(if (checked) "#EAD9F7" else "#FFFFFF")
            )
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
