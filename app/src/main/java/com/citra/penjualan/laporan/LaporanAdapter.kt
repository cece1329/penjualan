package com.citra.penjualan.laporan

import android.view.LayoutInflater
import android.view.ViewGroup
import com.citra.penjualan.R
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
            tvTotalHarga.text = "Rp ${t.totalHarga}"
            tvQty.text = holder.itemView.context.getString(R.string.item_qty_prefix, t.jumlah)
            tvTanggal.text = holder.itemView.context.getString(R.string.item_date_prefix, t.tanggal ?: "-")

            root.setOnLongClickListener {
                val context = holder.itemView.context
                androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.delete_confirm_title))
                    .setMessage(context.getString(R.string.delete_confirm_msg))
                    .setPositiveButton(context.getString(R.string.btn_delete)) { _, _ ->
                        t.idTransaksi?.let { id ->
                            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("transaksi").child(id)
                                .removeValue()
                                .addOnSuccessListener {
                                    android.widget.Toast.makeText(context, context.getString(R.string.laporan_delete_success), android.widget.Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .setNegativeButton(context.getString(R.string.btn_cancel), null)
                    .show()
                true
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<ModelTransaksi>) {
        list = newList
        notifyDataSetChanged()
    }
}
