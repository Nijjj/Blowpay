package com.example.minimalupi

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val listView = findViewById<ListView>(R.id.historyList)
        val emptyText = findViewById<TextView>(R.id.historyEmpty)

        val items = LocalStore.loadTransactions(this)
        if (items.isEmpty()) {
            emptyText.visibility = android.view.View.VISIBLE
            listView.visibility = android.view.View.GONE
        } else {
            emptyText.visibility = android.view.View.GONE
            listView.visibility = android.view.View.VISIBLE

            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            val rows = items.map { tx ->
                "₹${tx.amount} → ${tx.upiId} • ${formatter.format(Date(tx.timestamp))}"
            }

            listView.adapter = ArrayAdapter(
                this,
                R.layout.item_history,
                R.id.historyRowText,
                rows
            )
        }
    }
}
