package com.renown.whiskeyvictor

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.renown.whiskeyvictor.databinding.ActivityWebViewBinding
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class WebView : AppCompatActivity() {

    private val tag = "WebViewActivity"

    private lateinit var binding: ActivityWebViewBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val webViewSaveLinkButton = binding.webViewSaveLinkButton
        val webViewCloseButton = binding.webViewCloseButton
        val webViewMain = binding.webViewMain
        val newLink = intent.getStringExtra("newLink")
        val url = intent.getStringExtra("url")
        val userName = intent.getStringExtra("userName")
        val existingSessionJson = intent.getStringExtra("existingSession")
        val existingSession = Gson().fromJson(existingSessionJson, Session::class.java)

        if (newLink!= null && url != null && userName != null) {
            webViewMain.webViewClient = WebViewClient()
            webViewMain.settings.javaScriptEnabled = true
            webViewMain.loadUrl(url)
        } else if (existingSession != null) {
            webViewSaveLinkButton.visibility = View.GONE
            val cookieString = newSetCookies(existingSession)
            Log.d(tag, cookieString)
            Log.d(tag, existingSession.url)
            val cookieManager = CookieManager.getInstance()
            cookieManager.setCookie(existingSession.url, cookieString)
            webViewMain.webViewClient = WebViewClient()
            webViewMain.settings.javaScriptEnabled = true
            webViewMain.loadUrl(existingSession.url)
        }

        webViewCloseButton.setOnClickListener {
//            val cookiesJson = getCookiesAsJson(url ?: "")
//            Log.d(tag, "Cookies Json: $cookiesJson")
//            if(!newLink.isNullOrEmpty() && !url.isNullOrEmpty() && !userName.isNullOrEmpty() && cookiesJson.isNotEmpty()) {
//                val session = Session(userName = userName, url = url, cookiesJson = cookiesJson)
//                writeSessionToFile(this@WebView, session, "sessions_file.txt")
//                Log.d(tag, "Session written to file: $session")
//            } else {
//                Log.d(tag, "Empty URL or cookies or already saved session, not saving session.")
//            }



            clearCookies()
            webViewMain.clearCache(true)
            webViewMain.clearHistory()
            val intent = Intent(this, ExistingSessions::class.java)
            startActivity(intent)
            finish()
        }

        webViewSaveLinkButton.setOnClickListener {
            val cookiesJson = getCookiesAsJson(url ?: "")
            Log.d(tag, "Cookies Json: $cookiesJson")
            if(!newLink.isNullOrEmpty() && !url.isNullOrEmpty() && !userName.isNullOrEmpty() && cookiesJson.isNotEmpty()) {
                val session = Session(userName = userName, url = url, cookiesJson = cookiesJson)
                writeSessionToFile(this@WebView, session, "sessions_file.txt")
                Log.d(tag, "Session written to file: $session")
                Toast.makeText(this, "This session has been saved", Toast.LENGTH_SHORT).show()
                webViewSaveLinkButton.isEnabled = false
                webViewSaveLinkButton.alpha = 0.5f
            } else {
                Log.d(tag, "Empty URL or cookies or already saved session, not saving session.")
                Toast.makeText(this, "This session has already been saved.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCookiesAsJson(url: String): String {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(url)?.split("; ") ?: emptyList()
        val cookiesList = cookies.map { cookie ->
            val parts = cookie.split("=")
            Cookie(name = parts[0], value = parts.getOrElse(1) { "" })
        }
        val gson = Gson()
        return gson.toJson(cookiesList)
    }

    private fun clearCookies() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }

    private fun newSetCookies(session: Session): String {
        val gson = Gson()
        val cookieListType = object : TypeToken<List<Cookie>>() {}.type
        val cookies: List<Cookie> = gson.fromJson(session.cookiesJson, cookieListType)
        return cookies.joinToString(",") { "${it.name}=${it.value}" }
    }

    fun writeSessionToFile(context: Context, session: Session, fileName: String) {
        val gson = Gson()
        val sessionJson = gson.toJson(session)
        try {
            val fileOutputStream = context.openFileOutput(fileName, Context.MODE_APPEND)
            fileOutputStream.write(sessionJson.toByteArray())
            fileOutputStream.write("\n".toByteArray())
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        val webViewMain = binding.webViewMain
        if (webViewMain.canGoBack()) {
            webViewMain.goBack()
        } else {
            super.onBackPressed()
        }
    }
}