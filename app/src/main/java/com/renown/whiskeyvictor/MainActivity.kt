package com.renown.whiskeyvictor

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.renown.whiskeyvictor.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"

    private lateinit var binding: ActivityMainBinding

    private lateinit var sessionList: MutableList<Session>

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val deletedSessionAcknowledgementFrame = binding.sessionFileDeletedAcknowledgement
//        val deletedSessionAcknowledgementButton = binding.deletedSessionFileAcknowledgementButton
        val enterLinkEditText = binding.enterLinkEditText
        val enterUserNameEditText = binding.enterUserNameEditText
        val openWebViewButton = binding.openWebViewButton
//        val openExistingSessionsButton = binding.openExistingSessionsButton
//        val deleteExistingSessionsButton = binding.deleteExistingSessionsButton

        enterLinkEditText.setText("renownapp.com")

        sessionList = readSessionsFromFile(this, "sessions_file.txt")

        openWebViewButton.setOnClickListener {
            val url = enterLinkEditText.text.toString()
            val userName = enterUserNameEditText.text.toString()
            if (url.isNotEmpty() && userName.isNotEmpty()) {
                val usernameExists = sessionList.any { it.userName == userName }
                if (usernameExists) {
                    Toast.makeText(this, "Title already exists.", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this, WebView::class.java).apply {
                        putExtra("newLink", "1")
                        putExtra("url", url)
                        putExtra("userName", userName)
                    }
                    startActivity(intent)
                }
            } else {
                Toast.makeText(this, "Please enter a domain and a title", Toast.LENGTH_SHORT).show()
            }
        }

//        openExistingSessionsButton.setOnClickListener {
//            val intent = Intent(this, ExistingSessions::class.java)
//            startActivity(intent)
//        }

//        deleteExistingSessionsButton.setOnClickListener {
//            deleteSessionsFile(this, "sessions_file.txt")
//            deletedSessionAcknowledgementFrame.visibility = View.VISIBLE
//        }

//        deletedSessionAcknowledgementButton.setOnClickListener {
//            deletedSessionAcknowledgementFrame.visibility = View.GONE
//        }
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

//    private fun deleteSessionsFile(context: Context, fileName: String) {
//        val file = File(context.filesDir, fileName)
//        if (file.exists()) {
//            file.delete()
//            Log.d("MainActivity", "File deleted: $fileName")
//        } else {
//            Log.d("MainActivity", "File not found: $fileName")
//        }
//    }
}