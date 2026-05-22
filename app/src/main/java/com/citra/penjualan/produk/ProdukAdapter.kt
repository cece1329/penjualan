package com.citra.penjualan.produk

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ItemDataProdukBinding
import com.citra.penjualan.model.ModelProduk

class ProdukAdapter(private var list: List<ModelProduk>) : RecyclerView.Adapter<ProdukAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDataProdukBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemDataProdukBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = list[position]

        with(holder.binding) {
            // Isi Data Produk
            tvNamaProduk.text = p.namaProduk
            tvHargaProduk.text = "Rp. ${p.hargaProduk}"
            tvKategoriItem.text = p.namaKategori ?: "Kategori"
            tvStokItem.text = p.stokProduk?.toString() ?: "0"

            // Menampilkan Cabang Produk
            tvCabangItem.text = p.cabangProduk ?: "Belum Ada Cabang"

            // Automatic Gradient Avatar/Image Generation
            try {
                val initials = if (p.namaProduk.isNotEmpty()) p.namaProduk.take(1).uppercase() else "?"
                tvInitials.text = initials
                
                val colors1 = intArrayOf(Color.parseColor("#BA68C8"), Color.parseColor("#8E24AA"))
                val colors2 = intArrayOf(Color.parseColor("#9575CD"), Color.parseColor("#673AB7"))
                val colors3 = intArrayOf(Color.parseColor("#7986CB"), Color.parseColor("#3F51B5"))
                val colors4 = intArrayOf(Color.parseColor("#E040FB"), Color.parseColor("#9C27B0"))
                val gradientsList = listOf(colors1, colors2, colors3, colors4)
                
                val hashIdx = Math.abs(p.namaProduk.hashCode()) % gradientsList.size
                val selectedGradient = gradientsList[hashIdx]
                
                val gd = android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                    selectedGradient
                )
                gd.cornerRadius = 24f
                tvInitials.background = gd
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Logic Status Aktif / Tidak Aktif
            if (p.statusProduk == "Aktif") {
                chipStatus.text = "Aktif"
                chipStatus.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#72C9FFBF"))
                chipStatus.chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                chipStatus.setTextColor(Color.parseColor("#2E7D32"))
                chipStatus.setChipIconResource(R.drawable.labeltick)
            } else {
                chipStatus.text = "Tidak Aktif"
                chipStatus.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#FFCDD2")) // Merah Pastel
                chipStatus.chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#F44336"))
                chipStatus.setTextColor(Color.parseColor("#C62828"))

                // Gunakan ikon X bawaan Android
                chipStatus.setChipIconResource(android.R.drawable.ic_menu_close_clear_cancel)
            }

            // Tambahan agar item produk bisa diklik untuk diupdate
            root.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, TambahProdukActivity::class.java)
                intent.putExtra("DATA_PRODUK", p)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<ModelProduk>) {
        list = newList
        notifyDataSetChanged()
    }
}