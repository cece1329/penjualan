package com.citra.penjualan.notifikasi

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.BaseActivity
import com.citra.penjualan.R
import com.citra.penjualan.model.CatatanHarian
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatatanHarianActivity : BaseActivity() {

    private val dbCatatan = FirebaseDatabase.getInstance().getReference("catatan_harian")
    private val catatanList = mutableListOf<CatatanHarian>()
    private lateinit var adapter: CatatanHarianAdapter
    private val sharedPref by lazy { getSharedPreferences("user_session", MODE_PRIVATE) }
    private val userName by lazy { sharedPref.getString("user_name", "User") ?: "User" }
    private val userCabang by lazy { sharedPref.getString("user_cabang", "") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catatan_harian)

        findViewById<android.widget.TextView>(R.id.tvTitle).text = getString(R.string.daily_notes_title)

        adapter = CatatanHarianAdapter(catatanList) { catatanId ->
            deleteCatatan(catatanId)
        }
        findViewById<RecyclerView>(R.id.rvCatatan).apply {
            layoutManager = LinearLayoutManager(this@CatatanHarianActivity)
            this.adapter = this@CatatanHarianActivity.adapter
        }

        findViewById<android.widget.Button>(R.id.btnAddCatatan).setOnClickListener {
            showAddCatatanDialog()
        }

        loadCatatan()
    }

    private fun loadCatatan() {
        dbCatatan.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                catatanList.clear()
                for (data in snapshot.children) {
                    val catatan = data.getValue(CatatanHarian::class.java)
                    catatan?.let {
                        // Show notes for all users or specific to user's branch
                        if (it.cabang == "all" || it.cabang == userCabang) {
                            catatanList.add(it)
                        }
                    }
                }
                catatanList.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showAddCatatanDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_catatan, null)
        val etCatatan = dialogView.findViewById<EditText>(R.id.etCatatan)

        AlertDialog.Builder(this)
            .setTitle(R.string.add_note)
            .setView(dialogView)
            .setPositiveButton(R.string.send) { _, _ ->
                val catatanText = etCatatan.text.toString().trim()

                if (catatanText.isNotEmpty()) {
                    val catatan = CatatanHarian(
                        id = dbCatatan.push().key ?: "",
                        catatan = catatanText,
                        timestamp = System.currentTimeMillis(),
                        penulis = userName,
                        cabang = userCabang.ifBlank { "all" }
                    )
                    dbCatatan.child(catatan.id).setValue(catatan)
                        .addOnSuccessListener {
                            Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, R.string.note_failed, Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, R.string.note_empty, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteCatatan(catatanId: String) {
        dbCatatan.child(catatanId).removeValue()
            .addOnSuccessListener {
                catatanList.removeAll { it.id == catatanId }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                // Handle error
            }
    }
}
