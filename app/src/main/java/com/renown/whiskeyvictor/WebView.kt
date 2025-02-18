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
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.renown.whiskeyvictor.databinding.ActivityWebViewBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class WebView : AppCompatActivity() {

    private val tag = "WebViewActivity"

    private var upload: ValueCallback<Array<Uri>>? = null

    private lateinit var binding: ActivityWebViewBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val webViewSaveLinkButton = binding.webViewSaveLinkButton
        val webViewCloseButton = binding.webViewCloseButton
        val webViewRefreshButton = binding.webViewRefreshButton
        val webViewMain = binding.webViewMain
        val newLink = intent.getStringExtra("newLink")
        val url = intent.getStringExtra("url")
        val userName = intent.getStringExtra("userName")
        val existingSessionJson = intent.getStringExtra("existingSession")
        val existingSession = Gson().fromJson(existingSessionJson, Session::class.java)

        binding.webViewTitleEditText.setText(userName)

        if (newLink!= null && url != null && userName != null) {
            webViewMain.webViewClient = WebViewClient()
            webViewMain.webChromeClient = WebChromeClient()
            webViewMain.settings.javaScriptEnabled = true
            webViewMain.settings.domStorageEnabled = true
            webViewMain.settings.allowFileAccess = true
            webViewMain.settings.userAgentString = "Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"
            webViewMain.loadUrl(url)
        } else if (existingSession != null) {
            webViewSaveLinkButton.isEnabled = false
            webViewSaveLinkButton.visibility = View.GONE
//            webViewSaveLinkButton.text = "Update Link"
            val cookieString = newSetCookies(existingSession)
            Log.d(tag, cookieString)
            Log.d(tag, existingSession.url)
            val cookieManager = CookieManager.getInstance()
            cookieManager.setCookie(existingSession.url, cookieString)
            webViewMain.webViewClient = WebViewClient()
            webViewMain.webChromeClient = WebChromeClient()
            webViewMain.settings.javaScriptEnabled = true
            webViewMain.settings.domStorageEnabled = true
            webViewMain.settings.allowFileAccess = true
            webViewMain.settings.userAgentString = "Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"
            webViewMain.loadUrl(existingSession.url)
        }

        webViewMain.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
                val intent = fileChooserParams.createIntent().apply {
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                upload = filePathCallback
                startActivityForResult(intent, 101)
                return true
            }
        }

        webViewCloseButton.setOnClickListener {
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
            Log.d(tag, "Url: $url")
            Log.d(tag, "Title: $userName")

            if(!url.isNullOrEmpty() && !userName.isNullOrEmpty() && cookiesJson.isNotEmpty()) {
                var newSession = false
                val editedUserName = binding.webViewTitleEditText.text.toString()
                val session = Session(userName = userName, url = url, cookiesJson = cookiesJson)
                val existingSessions = readSessionsFromFile(this, "sessions_file.txt").toMutableList()

                newSession = true
                existingSessions.add(session)
                Log.d(tag, "Session written to file: $session")
                Toast.makeText(this, "This session has been saved", Toast.LENGTH_SHORT).show()

//                val sessionIndex = existingSessions.indexOfFirst { it.userName == userName }
//                if (sessionIndex != -1) {
//                    // Update the existing session
//                    newSession = false
//                    existingSessions[sessionIndex] = session
//                    Log.d(tag, "Session updated in file: $session")
//                } else {
//                    // Add a new session
//                    newSession = true
//                    existingSessions.add(session)
//                    Log.d(tag, "Session written to file: $session")
//                }

                overwriteSessionToFile(this@WebView, existingSessions, "sessions_file.txt")
                Log.d(tag, "Session written to file: $session")

//                if (newSession) {
//                    Toast.makeText(this, "This session has been saved", Toast.LENGTH_SHORT).show()
//                } else {
//                    Toast.makeText(this, "This session has been updated", Toast.LENGTH_SHORT).show()
//                }
//                webViewSaveLinkButton.text = "Update Link"

                webViewSaveLinkButton.isEnabled = false
                webViewSaveLinkButton.alpha = 0.5f
            } else {
                Log.d(tag, "Empty URL or cookies or already saved session, not saving session.")
                Toast.makeText(this, "This session has already been saved.", Toast.LENGTH_SHORT).show()
            }
        }

        webViewRefreshButton.setOnClickListener {
            webViewMain.reload()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101) {
            val results = data?.let {
                if (it.clipData != null) {
                    val count = it.clipData!!.itemCount
                    Array(count) { i -> it.clipData!!.getItemAt(i).uri }
                } else {
                    it.data?.let { uri -> arrayOf(uri) }
                }
            }
            upload?.onReceiveValue(results)
            upload = null
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
        Log.d(tag, cookiesList.toString())
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

    private fun writeSessionToFile(context: Context, session: Session, fileName: String) {
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

    private fun overwriteSessionToFile(context: Context, sessions: List<Session>, fileName: String) {
        val gson = Gson()
        val sessionsJson = sessions.joinToString(separator = "\n") { gson.toJson(it) }
        try {
            val fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            fileOutputStream.write(sessionsJson.toByteArray())
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
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

    override fun onBackPressed() {
        val webViewMain = binding.webViewMain
        if (webViewMain.canGoBack()) {
            webViewMain.goBack()
        } else {
            super.onBackPressed()
        }
    }
}