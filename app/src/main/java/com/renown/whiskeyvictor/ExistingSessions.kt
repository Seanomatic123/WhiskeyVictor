package com.renown.whiskeyvictor

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.renown.whiskeyvictor.databinding.ActivityExistingSessionsBinding
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import kotlin.properties.Delegates

class ExistingSessions : AppCompatActivity() {

    private val tag = "ExistingSessions"

    private lateinit var binding: ActivityExistingSessionsBinding

    private lateinit var sessionList: MutableList<Session>

    private lateinit var filteredSessionList: MutableList<Session>

    private lateinit var sessionToDelete: Session
    private var adapterPositionToDelete : Int? = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExistingSessionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentBinding = binding

        val existingSessionsDefaultFrame = binding.existingSessionsDefaultFrame
        val existingSessionDeleteConfirmation = binding.existingSessionDeleteConfirmation

        val deleteSessionAcceptButton = binding.deleteSessionAcceptButton
        val deleteSessionCancelButton = binding.deleteSessionCancelButton

        val existingSessionCreateNewSessionButton = binding.existingSessionsCreateNewSessionButton

        sessionList = readSessionsFromFile(this, "sessions_file.txt")

        val existingSessionEmptyPlaceholder = binding.existingSessionsEmptyPlaceholder
        if (sessionList.isEmpty()) {
            existingSessionEmptyPlaceholder.visibility = TextView.VISIBLE
        } else {
            existingSessionEmptyPlaceholder.visibility = TextView.GONE
        }

        existingSessionCreateNewSessionButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val existingSessionsRecyclerView = binding.existingSessionsRecyclerView
        val existingSessionsSearchView = binding.existingSessionsSearchView

        existingSessionsSearchView.setOnClickListener {
            existingSessionsSearchView.isIconified = false
        }

        filteredSessionList = sessionList.toMutableList()

        val sessionAdapter = SessionAdapter(this, filteredSessionList)
        existingSessionsRecyclerView.adapter = sessionAdapter.apply {
            sessionAdapterClickCallback = object: SessionAdapterClickCallback {
                override fun onSessionClicked(session: Session, adapterPosition: Int?) {
                    val intent = Intent(applicationContext, WebView::class.java).apply {
                        putExtra("existingSession", Gson().toJson(session))
                    }
                    startActivity(intent)
                }
                override fun onSessionDeleteClicked(session: Session, adapterPosition: Int?) {
                    existingSessionDeleteConfirmation.visibility = View.VISIBLE
                    sessionToDelete = session
                    adapterPositionToDelete = adapterPosition

                }
            }
        }
        existingSessionsRecyclerView.layoutManager = LinearLayoutManager(this)

        deleteSessionCancelButton.setOnClickListener {
            existingSessionDeleteConfirmation.visibility = View.GONE
        }

        deleteSessionAcceptButton.setOnClickListener {
            if (adapterPositionToDelete != null) {
                adapterPositionToDelete?.let {
                    // Remove from original session list
                    val indexInOriginalList = sessionList.indexOfFirst { it.userName == sessionToDelete.userName }
                    if (indexInOriginalList != -1) {
                        sessionList.removeAt(indexInOriginalList)
                    }
                    // Remove from filtered session list
                    filteredSessionList.removeAt(it)
                    // Notify adapter of item removed
                    sessionAdapter.notifyItemRemoved(it)
                    sessionAdapter.notifyItemRangeChanged(it, filteredSessionList.size)
                    // Delete session from file (or other storage)
                    deleteSessionFromFile(sessionToDelete)
                }
            }
            existingSessionDeleteConfirmation.visibility = View.GONE
            Toast.makeText(this, "Session has been deleted.", Toast.LENGTH_SHORT).show()
        }

        // search view filter query listener
        existingSessionsSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filter(sessionAdapter, query)
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                filter(sessionAdapter, newText)
                return false
            }
        })

    }

    private fun filter(adapter: SessionAdapter, text: String?) {
        filteredSessionList.clear()
        if (text.isNullOrEmpty()) {
            filteredSessionList.addAll(sessionList)
        } else {
            val lowerCaseText = text.toLowerCase()
            sessionList.forEach {
                if (it.userName.toLowerCase().contains(lowerCaseText)) {
                    filteredSessionList.add(it)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun readSessionsFromFile(context: Context, fileName: String): MutableList<Session> {
        val sessions = mutableListOf<Session>()
        try {
            val fileInputStream = context.openFileInput(fileName)
            val inputStreamReader = InputStreamReader(fileInputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            var line: String? = bufferedReader.readLine()
            val gson = Gson()
            while (line != null) {
                val session = gson.fromJson(line, Session::class.java)
                sessions.add(session)
                line = bufferedReader.readLine()
            }
            bufferedReader.close()
            inputStreamReader.close()
            fileInputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return sessions
    }

    private fun deleteSessionFromFile(sessionToDelete: Session) {
        try {
            val file = File(filesDir, "sessions_file.txt")
            if (file.exists()) {
                val sessions = readSessionsFromFile(this, "sessions_file.txt").toMutableList()
                sessions.removeAll { it.url == sessionToDelete.url && it.userName == sessionToDelete.userName }
                reWriteSessionsToFile(sessions)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun reWriteSessionsToFile(sessions: List<Session>) {
        try {
            val fileOutputStream = openFileOutput("sessions_file.txt", Context.MODE_PRIVATE)
            val gson = Gson()
            for (session in sessions) {
                val sessionJson = gson.toJson(session)
                fileOutputStream.write(sessionJson.toByteArray())
                fileOutputStream.write("\n".toByteArray())
            }
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        val sessionAdapter = SessionAdapter(this, filteredSessionList)
        sessionAdapter.notifyDataSetChanged()
    }

}