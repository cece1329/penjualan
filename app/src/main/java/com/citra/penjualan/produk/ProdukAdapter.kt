package com.citra.penjualan.produk

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ItemDataProdukBinding
import com.citra.penjualan.model.ModelProduk
import com.citra.penjualan.produk.TambahProdukActivity

class ProdukAdapter(
    private var list: List<ModelProduk>,
    private val onItemClick: ((ModelProduk) -> Unit)? = null,
    private var selectedCategory: String = "Semua"
) : RecyclerView.Adapter<ProdukAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDataProdukBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemDataProdukBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = list[position]

        with(holder.binding) {
            // Isi Data Produk
            tvNamaProduk.text = p.namaProduk
            tvHargaProduk.text = "Rp ${formatNumber(p.hargaProduk)}"
            tvKategoriItem.text = p.namaKategori ?: "Kategori"
            tvStokItem.text = formatNumber(p.stokProduk ?: 0)

            // Menampilkan Cabang Produk
            tvCabangItem.text = p.cabangProduk ?: "Belum Ada Cabang"

            imgProduk.setImageResource(R.drawable.product)
            imgProduk.visibility = View.VISIBLE
            tvInitials.visibility = View.GONE

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
                if (onItemClick != null) {
                    onItemClick.invoke(p)
                } else {
                    val context = holder.itemView.context
                    val intent = Intent(context, TambahProdukActivity::class.java)
                    intent.putExtra("DATA_PRODUK", p)
                    context.startActivity(intent)
                }
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<ModelProduk>) {
        list = newList
        notifyDataSetChanged()
    }

    private fun formatNumber(amount: Int): String {
        val s = amount.toString()
        val sb = StringBuilder()
        var count = 0
        for (i in s.length - 1 downTo 0) {
            if (count > 0 && count % 3 == 0) sb.insert(0, ".")
            sb.insert(0, s[i])
            count++
        }
        return sb.toString()
    }

    fun filterList(query: String, category: String, originalList: List<ModelProduk>) {
        selectedCategory = category
        val filtered = originalList.filter { produk ->
            val matchesCategory = category == "Semua" || 
                                 produk.namaKategori.equals(category, ignoreCase = true)
            val matchesSearch = query.isEmpty() || 
                               produk.namaProduk.contains(query, ignoreCase = true) ||
                               (produk.namaKategori?.contains(query, ignoreCase = true) == true) ||
                               (produk.cabangProduk?.contains(query, ignoreCase = true) == true)
            
            matchesCategory && matchesSearch
        }
        list = filtered
        notifyDataSetChanged()
    }
}
