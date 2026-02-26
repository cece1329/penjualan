package com.citra.penjualan.kategori

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.R
import com.google.android.material.chip.Chip

class KategoriAdapter(
    private var list: List<Kategori>,
    private val listener: (Kategori) -> Unit
) : RecyclerView.Adapter<KategoriAdapter.KategoriViewHolder>() {

    inner class KategoriViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgKategori: ImageView = itemView.findViewById(R.id.imgKategori)
        val txtNama: TextView = itemView.findViewById(R.id.txtNamaKategori)
        val chipStatus: Chip = itemView.findViewById(R.id.chipStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KategoriViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_item_kategori, parent, false)
        return KategoriViewHolder(view)
    }

    override fun onBindViewHolder(holder: KategoriViewHolder, position: Int) {
        val kategori = list[position]
        holder.txtNama.text = kategori.nama
        holder.chipStatus.text = kategori.status
        holder.itemView.setOnClickListener { listener(kategori) }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<Kategori>) {
        list = newList
        notifyDataSetChanged()
    }
}
