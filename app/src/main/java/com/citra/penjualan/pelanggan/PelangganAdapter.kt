package com.citra.penjualan.pelanggan

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.databinding.ItemPelangganBinding
import com.citra.penjualan.model.ModelPelanggan

class PelangganAdapter(private var list: List<ModelPelanggan>) : RecyclerView.Adapter<PelangganAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemPelangganBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemPelangganBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = list[position]
        with(holder.binding) {
            tvNamaPelanggan.text = p.namaPelanggan
            tvTeleponPelanggan.text = "Telepon: ${p.teleponPelanggan ?: "-"}"
            tvAlamatPelanggan.text = "Alamat: ${p.alamatPelanggan ?: "-"}"

            root.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, TambahPelangganActivity::class.java)
                intent.putExtra("DATA_PELANGGAN", p)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<ModelPelanggan>) {
        list = newList
        notifyDataSetChanged()
    }
}
