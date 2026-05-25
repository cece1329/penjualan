package com.citra.penjualan.kategori

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ItemKategoriCardBinding
import com.citra.penjualan.model.ModelKategori

class KategoriAdapter(
    private var list: List<ModelKategori>,
    private var productCounts: Map<String, Int> = emptyMap()
) : RecyclerView.Adapter<KategoriAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemKategoriCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemKategoriCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val k = list[position]
        with(holder.binding) {
            val categoryName = k.namaKategori ?: "Unknown"
            val count = productCounts[categoryName] ?: 0
            
            tvNamaKategori.text = categoryName
            tvStokItem.text = "$count Produk"
            tvCabangItem.text = k.cabangKategori ?: "Semua Cabang"
            
            ivKatIcon.setImageResource(R.drawable.labelkategori)
            ivKatIcon.setColorFilter(Color.parseColor("#BA68C8")) // Ungu Soft

            val status = k.statusKategori ?: "Aktif"
            chipStatus.text = status
            
            if (status == "Aktif") {
                chipStatus.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#72C9FFBF"))
                chipStatus.chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                chipStatus.setTextColor(Color.parseColor("#2E7D32"))
                chipStatus.setChipIconResource(R.drawable.labeltick)
                chipStatus.isChipIconVisible = true
            } else {
                chipStatus.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#FFCDD2"))
                chipStatus.chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#F44336"))
                chipStatus.setTextColor(Color.parseColor("#C62828"))
                chipStatus.setChipIconResource(android.R.drawable.ic_menu_close_clear_cancel)
                chipStatus.isChipIconVisible = true
            }

            root.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, TambahKategoriActivity::class.java)
                intent.putExtra("DATA_KATEGORI", k)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<ModelKategori>, newProductCounts: Map<String, Int>) {
        list = newList
        productCounts = newProductCounts
        notifyDataSetChanged()
    }

    fun filterList(query: String, originalList: List<ModelKategori>) {
        list = if (query.isEmpty()) {
            originalList
        } else {
            originalList.filter {
                it.namaKategori?.contains(query, ignoreCase = true) == true ||
                it.cabangKategori?.contains(query, ignoreCase = true) == true
            }
        }
        notifyDataSetChanged()
    }
}