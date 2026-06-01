package com.citra.penjualan.notifikasi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.R
import com.citra.penjualan.model.CatatanHarian
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatatanHarianAdapter(
    private val catatanList: MutableList<CatatanHarian>,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<CatatanHarianAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCatatan: TextView = view.findViewById(R.id.tvCatatan)
        val tvPenulis: TextView = view.findViewById(R.id.tvPenulis)
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val btnDelete: View = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_catatan_harian, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val catatan = catatanList[position]
        holder.tvCatatan.text = catatan.catatan
        holder.tvPenulis.text = "${holder.itemView.context.getString(R.string.by)}: ${catatan.penulis}"

        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        holder.tvTanggal.text = dateFormat.format(Date(catatan.timestamp))

        holder.btnDelete.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle(R.string.delete_note_title)
                .setMessage(R.string.delete_note_message)
                .setPositiveButton(R.string.cancel) { _, _ ->
                    onDelete(catatan.id)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun getItemCount() = catatanList.size
}
