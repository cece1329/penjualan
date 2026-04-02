package com.citra.penjualan.produk

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ItemDataProdukBinding // 1. Pastikan nama filenya item_produk.xml
import com.citra.penjualan.model.ModelProduk

class ProdukAdapter(private var listProduk: List<ModelProduk>) :
    RecyclerView.Adapter<ProdukAdapter.ViewHolder>() {

    // 2. Gunakan ItemProdukBinding sesuai nama file XML baru kamu
    class ViewHolder(val binding: ItemDataProdukBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDataProdukBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val produk = listProduk[position]

        // --- 3. PEMASANGAN DATA SESUAI ID XML BARU (format camelCase) ---
        holder.binding.tvNamaProduk.text = produk.namaProduk
        holder.binding.tvHargaProduk.text = "Rp ${produk.hargaProduk}"

        // Chip Status (ID: chip_status)
        holder.binding.chipStatus.text = produk.statusProduk ?: "Aktif"

        // Load Gambar ke img_produk (ID: img_produk)
        Glide.with(holder.itemView.context)
            .load(produk.fotoProduk)
            .placeholder(R.drawable.ic_camera_placeholder)
            .error(R.drawable.ic_camera_placeholder)
            .into(holder.binding.imgProduk)

        // Klik pada kartu produk (ID: card_produk)
        holder.binding.cardProduk.setOnClickListener {
            // Aksi kalau mau pindah ke detail produk atau edit
        }
    }

    override fun getItemCount(): Int = listProduk.size

    fun updateData(newList: List<ModelProduk>) {
        listProduk = newList
        notifyDataSetChanged()
    }
}