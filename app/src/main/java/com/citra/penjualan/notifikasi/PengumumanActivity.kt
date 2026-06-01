package com.citra.penjualan.notifikasi

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.BaseActivity
import com.citra.penjualan.R
import com.citra.penjualan.model.Pengumuman
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PengumumanActivity : BaseActivity() {

    private val dbPengumuman = FirebaseDatabase.getInstance().getReference("pengumuman")
    private val announcementList = mutableListOf<Pengumuman>()
    private lateinit var adapter: PengumumanAdapter
    private val sharedPref by lazy { getSharedPreferences("user_session", MODE_PRIVATE) }
    private val currentUserRole by lazy { sharedPref.getString("user_role", "karyawan") ?: "karyawan" }
    private val userCabang by lazy { sharedPref.getString("user_cabang", "") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengumuman)

        findViewById<android.widget.TextView>(R.id.tvTitle).text = getString(R.string.announcements_title)

        adapter = PengumumanAdapter(
            announcementList,
            currentUserRole == "pemilik" || currentUserRole == "admin"
        ) { pengumumanId ->
            deletePengumuman(pengumumanId)
        }
        findViewById<RecyclerView>(R.id.rvPengumuman).apply {
            layoutManager = LinearLayoutManager(this@PengumumanActivity)
            this.adapter = this@PengumumanActivity.adapter
        }

        // Show add button only for pemilik and admin
        if (currentUserRole == "pemilik" || currentUserRole == "admin") {
            findViewById<android.widget.Button>(R.id.btnAddPengumuman).visibility = android.view.View.VISIBLE
            findViewById<android.widget.Button>(R.id.btnAddPengumuman).setOnClickListener {
                showAddPengumumanDialog()
            }
        } else {
            findViewById<android.widget.Button>(R.id.btnAddPengumuman).visibility = android.view.View.GONE
        }

        loadPengumuman()
    }

    private fun loadPengumuman() {
        dbPengumuman.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                announcementList.clear()
                for (data in snapshot.children) {
                    val pengumuman = data.getValue(Pengumuman::class.java)
                    pengumuman?.let {
                        // Show announcements for all users or specific to user's branch
                        if (it.targetCabang == "all" || it.targetCabang == userCabang) {
                            announcementList.add(it)
                        }
                    }
                }
                announcementList.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showAddPengumumanDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_pengumuman, null)
        val etJudul = dialogView.findViewById<EditText>(R.id.etJudul)
        val etPesan = dialogView.findViewById<EditText>(R.id.etPesan)

        AlertDialog.Builder(this)
            .setTitle(R.string.create_announcement)
            .setView(dialogView)
            .setPositiveButton(R.string.send) { _, _ ->
                val judul = etJudul.text.toString().trim()
                val pesan = etPesan.text.toString().trim()

                if (judul.isNotEmpty() && pesan.isNotEmpty()) {
                    val pengumuman = Pengumuman(
                        id = dbPengumuman.push().key ?: "",
                        judul = judul,
                        pesan = pesan,
                        timestamp = System.currentTimeMillis(),
                        pengirim = sharedPref.getString("user_name", "Admin") ?: "Admin",
                        targetCabang = "all" // Send to all branches
                    )
                    dbPengumuman.child(pengumuman.id).setValue(pengumuman)
                        .addOnSuccessListener {
                            Toast.makeText(this, R.string.announcement_sent, Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, R.string.announcement_failed, Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, R.string.complete_all_fields, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deletePengumuman(pengumumanId: String) {
        dbPengumuman.child(pengumumanId).removeValue()
            .addOnSuccessListener {
                announcementList.removeAll { it.id == pengumumanId }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                // Handle error
            }
    }
}
