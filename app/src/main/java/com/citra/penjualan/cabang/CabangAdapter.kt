package com.citra.penjualan.cabang

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.databinding.ItemCabangBinding
import com.citra.penjualan.model.ModelCabang

class CabangAdapter(private var list: List<ModelCabang>) : RecyclerView.Adapter<CabangAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemCabangBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemCabangBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val c = list[position]
        with(holder.binding) {
            tvNamaCabang.text = c.namaCabang
            tvKotaCabang.text = "Kota: ${c.kotaCabang ?: "-"}"
            tvAlamatCabang.text = "Alamat: ${c.alamatCabang ?: "-"}"

            root.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, TambahCabangActivity::class.java)
                intent.putExtra("DATA_CABANG", c)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<ModelCabang>) {
        list = newList
        notifyDataSetChanged()
    }
}
