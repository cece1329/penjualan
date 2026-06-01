package com.citra.penjualan.notifikasi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.R
import com.citra.penjualan.model.Notifikasi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotifikasiAdapter(
    private val notifications: MutableList<Notifikasi>,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<NotifikasiAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvJudul: TextView = view.findViewById(R.id.tvJudul)
        val tvPesan: TextView = view.findViewById(R.id.tvPesan)
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val viewUnread: View = view.findViewById(R.id.viewUnread)
        val btnDelete: View = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notifikasi, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notif = notifications[position]
        holder.tvJudul.text = notif.judul
        holder.tvPesan.text = notif.pesan

        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        holder.tvTanggal.text = dateFormat.format(Date(notif.timestamp))

        holder.viewUnread.visibility = if (notif.dibaca) View.GONE else View.VISIBLE

        holder.btnDelete.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle(R.string.delete_notification_title)
                .setMessage(R.string.delete_notification_message)
                .setPositiveButton(R.string.cancel) { _, _ ->
                    onDelete(notif.id)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun getItemCount() = notifications.size
}
