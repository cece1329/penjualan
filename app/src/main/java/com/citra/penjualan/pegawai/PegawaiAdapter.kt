package com.citra.penjualan.pegawai

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.citra.penjualan.R
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
        val context = holder.itemView.context
        val session = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val roleLogin = session.getString("user_role", "karyawan") ?: "karyawan"

        with(holder.binding) {
            tvNamaPegawai.text = p.namaPegawai
            tvJabatanPegawai.text = context.getString(R.string.item_position_prefix, p.jabatanPegawai ?: "-")
            tvTeleponPegawai.text = context.getString(R.string.item_phone_prefix, p.teleponPegawai ?: "-")
            tvCabangPegawai.text = "Cabang: ${p.cabangPegawai ?: "-"}"
            
            // Keamanan: Hanya pemilik yang bisa melihat PIN/Password pegawai lain
            // Dan karyawan tidak boleh melihat PIN/Password siapapun kecuali dirinya sendiri (opsional)
            // Di sini kita batasi: Jika login sebagai karyawan, PIN disembunyikan.
            if (roleLogin == "pemilik") {
                tvPinPegawai.text = "PIN: ${p.passwordPegawai ?: "-"}"
                tvPinPegawai.visibility = View.VISIBLE
            } else {
                tvPinPegawai.visibility = View.GONE
            }

            root.setOnClickListener {
                if (roleLogin == "pemilik") {
                    val intent = Intent(context, TambahPegawaiActivity::class.java)
                    intent.putExtra("DATA_PEGAWAI", p)
                    context.startActivity(intent)
                } else {
                    android.widget.Toast.makeText(context, "Hanya pemilik yang dapat mengubah data pegawai", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<ModelPegawai>) {
        list = newList
        notifyDataSetChanged()
    }
}
