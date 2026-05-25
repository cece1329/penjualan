package com.citra.penjualan.cabang

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.citra.penjualan.R
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.databinding.ItemCabangBinding
import com.citra.penjualan.model.ModelCabang
import com.citra.penjualan.model.ModelPegawai
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CabangAdapter(private var list: List<ModelCabang>) : RecyclerView.Adapter<CabangAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemCabangBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemCabangBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val c = list[position]
        with(holder.binding) {
            tvNamaCabang.text = c.namaCabang
            tvKotaCabang.text = holder.itemView.context.getString(R.string.item_city_prefix, c.kotaCabang ?: "-")
            tvAlamatCabang.text = holder.itemView.context.getString(R.string.item_address_prefix, c.alamatCabang ?: "-")

            // Logika otomatis menampilkan pegawai di card Cabang
            val dbPegawai = FirebaseDatabase.getInstance().getReference("pegawai")
            dbPegawai.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val listPegawai = mutableListOf<String>()
                    for (data in snapshot.children) {
                        val p = data.getValue(ModelPegawai::class.java)
                        if (p != null && p.cabangPegawai == c.namaCabang) {
                            listPegawai.add(p.namaPegawai ?: "-")
                        }
                    }
                    
                    if (listPegawai.isNotEmpty()) {
                        tvDaftarPegawai.text = listPegawai.joinToString(", ")
                        ivIconPegawai.setImageResource(R.drawable.employee)
                        ivIconPegawai.setColorFilter(Color.parseColor("#BA68C8")) // Ungu Soft
                        lnPegawaiSection?.visibility = View.VISIBLE
                    } else {
                        tvDaftarPegawai.text = "Belum ada petugas"
                        lnPegawaiSection?.visibility = View.VISIBLE
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

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
