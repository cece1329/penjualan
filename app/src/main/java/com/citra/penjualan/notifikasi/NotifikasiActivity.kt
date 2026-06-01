package com.citra.penjualan.notifikasi

import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.BaseActivity
import com.citra.penjualan.R
import com.citra.penjualan.model.Notifikasi
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class NotifikasiActivity : BaseActivity() {

    private val dbNotifikasi = FirebaseDatabase.getInstance().getReference("notifikasi")
    private val dbProduk = FirebaseDatabase.getInstance().getReference("produk")
    private val notificationList = mutableListOf<Notifikasi>()
    private lateinit var adapter: NotifikasiAdapter
    private val sharedPref by lazy { getSharedPreferences("user_session", MODE_PRIVATE) }
    private val userCabang by lazy { sharedPref.getString("user_cabang", "") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifikasi)

        findViewById<TextView>(R.id.tvTitle).text = getString(R.string.notifications_title)

        adapter = NotifikasiAdapter(notificationList) { notifId ->
            deleteNotification(notifId)
        }
        findViewById<RecyclerView>(R.id.rvNotifications).apply {
            layoutManager = LinearLayoutManager(this@NotifikasiActivity)
            this.adapter = this@NotifikasiActivity.adapter
        }

        loadNotifications()
        loadLowStockNotifications()
    }

    private fun loadNotifications() {
        dbNotifikasi.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notificationList.clear()
                for (data in snapshot.children) {
                    val notif = data.getValue(Notifikasi::class.java)
                    notif?.let {
                        // Show notifications for all users or specific to user's branch
                        if (it.targetCabang == "all" || it.targetCabang == userCabang) {
                            notificationList.add(it)
                        }
                    }
                }
                notificationList.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadLowStockNotifications() {
        dbProduk.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (data in snapshot.children) {
                    val stok = data.child("stok").getValue(Int::class.java) ?: 0
                    val namaProduk = data.child("namaProduk").value?.toString() ?: ""
                    val cabang = data.child("cabang").value?.toString() ?: ""

                    // Check if stock is low (less than 5) and belongs to user's branch
                    if (stok < 5 && (cabang == userCabang || userCabang.isBlank())) {
                        val lowStockNotif = Notifikasi(
                            id = "stock_${data.key}",
                            jenis = "stok_habis",
                            judul = "Stok Hampir Habis",
                            pesan = "$namaProduk tersisa $stok item",
                            timestamp = System.currentTimeMillis(),
                            targetCabang = cabang,
                            dibaca = false
                        )
                        // Avoid duplicates
                        if (!notificationList.any { it.id == lowStockNotif.id }) {
                            notificationList.add(lowStockNotif)
                        }
                    }
                }
                notificationList.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun deleteNotification(notifId: String) {
        dbNotifikasi.child(notifId).removeValue()
            .addOnSuccessListener {
                notificationList.removeAll { it.id == notifId }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                // Handle error
            }
    }
}
