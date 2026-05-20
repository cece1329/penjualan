package com.citra.penjualan.laporan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.databinding.ItemLaporanBinding
import com.citra.penjualan.model.ModelTransaksi

class LaporanAdapter(private var list: List<ModelTransaksi>) : RecyclerView.Adapter<LaporanAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemLaporanBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemLaporanBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = list[position]
        with(holder.binding) {
            tvNamaProduk.text = t.namaProduk
            tvTotalHarga.text = "Rp. ${t.totalHarga}"
            tvQty.text = "Jumlah: ${t.jumlah} item"
            tvTanggal.text = "Tanggal: ${t.tanggal ?: "-"}"
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<ModelTransaksi>) {
        list = newList
        notifyDataSetChanged()
    }
}
