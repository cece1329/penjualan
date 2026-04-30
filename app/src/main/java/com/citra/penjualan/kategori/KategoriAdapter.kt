package com.citra.penjualan.kategori

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ItemDataKategoriBinding
import com.citra.penjualan.model.ModelKategori

class KategoriAdapter(private var list: List<ModelKategori>) : RecyclerView.Adapter<KategoriAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDataKategoriBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemDataKategoriBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val k = list[position]
        with(holder.binding) {
            // Isi Data
            txtNamaKategori.text = k.namaKategori
            tvCabangKategori.text = k.cabangKategori ?: "Belum Ada Cabang"

            // Logic Status Aktif / Tidak Aktif
            if (k.statusKategori == "Aktif") {
                chipStatus.text = "Aktif"
                chipStatus.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#72C9FFBF")) // Hijau Pastel
                chipStatus.chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                chipStatus.setTextColor(Color.parseColor("#2E7D32"))
                chipStatus.setChipIconResource(R.drawable.labeltick)
            } else {
                chipStatus.text = "Tidak Aktif"
                chipStatus.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#FFCDD2")) // Merah Pastel
                chipStatus.chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#F44336"))
                chipStatus.setTextColor(Color.parseColor("#C62828"))

                // Menggunakan ikon X bawaan Android
                chipStatus.setChipIconResource(android.R.drawable.ic_menu_close_clear_cancel)
            }

            // Klik Item untuk Edit
            root.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, TambahKategoriActivity::class.java)
                intent.putExtra("DATA_KATEGORI", k)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<ModelKategori>) {
        list = newList
        notifyDataSetChanged()
    }
}