package com.citra.penjualan.notifikasi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.R
import com.citra.penjualan.model.Pengumuman
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PengumumanAdapter(
    private val announcements: MutableList<Pengumuman>,
    private val canDelete: Boolean,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<PengumumanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvJudul: TextView = view.findViewById(R.id.tvJudul)
        val tvPesan: TextView = view.findViewById(R.id.tvPesan)
        val tvPengirim: TextView = view.findViewById(R.id.tvPengirim)
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val btnDelete: View = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pengumuman, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pengumuman = announcements[position]
        holder.tvJudul.text = pengumuman.judul
        holder.tvPesan.text = pengumuman.pesan
        holder.tvPengirim.text = "${holder.itemView.context.getString(R.string.by)}: ${pengumuman.pengirim}"

        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        holder.tvTanggal.text = dateFormat.format(Date(pengumuman.timestamp))

        holder.btnDelete.visibility = if (canDelete) View.VISIBLE else View.GONE

        holder.btnDelete.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle(R.string.delete_announcement_title)
                .setMessage(R.string.delete_announcement_message)
                .setPositiveButton(R.string.cancel) { _, _ ->
                    onDelete(pengumuman.id)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun getItemCount() = announcements.size
}
