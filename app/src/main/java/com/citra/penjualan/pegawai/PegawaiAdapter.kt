package com.citra.penjualan.pegawai

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.databinding.ItemPegawaiBinding
import com.citra.penjualan.model.ModelPegawai

class PegawaiAdapter(private var list: List<ModelPegawai>) : RecyclerView.Adapter<PegawaiAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemPegawaiBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemPegawaiBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = list[position]
        with(holder.binding) {
            tvNamaPegawai.text = p.namaPegawai
            tvJabatanPegawai.text = "Jabatan: ${p.jabatanPegawai ?: "-"}"
            tvTeleponPegawai.text = "Telepon: ${p.teleponPegawai ?: "-"}"

            root.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, TambahPegawaiActivity::class.java)
                intent.putExtra("DATA_PEGAWAI", p)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<ModelPegawai>) {
        list = newList
        notifyDataSetChanged()
    }
}
