package com.farhan.dayblock

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.farhan.dayblock.data.DayblockDatabase
import com.farhan.dayblock.data.DiaryEntry
import com.farhan.dayblock.data.DiaryRepository
import com.farhan.dayblock.domain.DiaryStatsCalculator
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * Dayblock's interface is written in HTML/CSS (assets/dayblock/index.html)
 * and shown in a WebView. Kotlin stays in charge of the data: the page
 * talks to [DayblockBridge], which reads and writes the same Room
 * database the app has always used, so existing entries are untouched.
 */
class MainActivity : ComponentActivity() {

    /** Receives the file the page's <input type="file"> (photo of the day) picks. */
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uris =
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { arrayOf(it) }
                } else {
                    null
                }
            filePathCallback?.onReceiveValue(uris)
            filePathCallback = null
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = DiaryRepository(
            DayblockDatabase.create(application).diaryDao()
        )

        val webView = WebView(this).apply {
            // Cream paper colour, so there is no white flash before the page loads.
            setBackgroundColor(0xFFFFF8EC.toInt())
            settings.javaScriptEnabled = true
            // Lets the page remember the dark/light theme between opens.
            settings.domStorageEnabled = true
            addJavascriptInterface(DayblockBridge(repository, this@MainActivity), "DayblockBridge")
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView,
                    filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams
                ): Boolean {
                    this@MainActivity.filePathCallback?.onReceiveValue(null)
                    this@MainActivity.filePathCallback = filePathCallback
                    return try {
                        fileChooserLauncher.launch(fileChooserParams.createIntent())
                        true
                    } catch (e: Exception) {
                        this@MainActivity.filePathCallback = null
                        false
                    }
                }
            }
            loadUrl("file:///android_asset/dayblock/index.html")
        }
        setContentView(webView)
    }
}

/** The small bridge the HTML page uses to reach the diary database. */
class DayblockBridge(
    private val repository: DiaryRepository,
    private val context: Context
) {

    /** Whole diary state (entries + stats) as one JSON string. */
    @JavascriptInterface
    fun getStateJson(): String = runBlocking {
        val entries = repository.entries.first()
        val stats = DiaryStatsCalculator.calculate(entries, LocalDate.now())

        val entriesJson = JSONArray()
        for (entry in entries) {
            entriesJson.put(
                JSONObject()
                    .put("date", entry.date)
                    .put("title", entry.title)
                    .put("body", entry.body)
                    .put("mood", entry.mood)
                    .put("tags", entry.tags)
                    .put("starred", entry.starred)
            )
        }

        JSONObject()
            .put("entries", entriesJson)
            .put(
                "stats",
                JSONObject()
                    .put("totalEntries", stats.totalEntries)
                    .put("currentStreak", stats.currentStreak)
                    .put("entriesThisWeek", stats.entriesThisWeek)
                    .put("totalWords", stats.totalWords)
                    .put("moodCounts", JSONObject(stats.moodCounts))
            )
            .toString()
    }

    @JavascriptInterface
    fun saveEntry(date: String, title: String, body: String, mood: String, tags: String) {
        runBlocking {
            val now = System.currentTimeMillis()
            val existing = repository.getByDate(date)
            repository.upsert(
                DiaryEntry(
                    id = existing?.id ?: 0,
                    date = date,
                    title = title.trim(),
                    body = body.trim(),
                    mood = mood,
                    tags = tags.trim(),
                    starred = existing?.starred ?: false,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )
            )
        }
    }

    @JavascriptInterface
    fun deleteEntry(date: String) {
        runBlocking {
            repository.getByDate(date)?.let { repository.delete(it) }
        }
    }

    @JavascriptInterface
    fun toggleStar(date: String) {
        runBlocking {
            repository.getByDate(date)?.let {
                repository.setStarred(it, !it.starred)
            }
        }
    }

    /** Sends text (a diary export or an encrypted backup) out through the share sheet. */
    @JavascriptInterface
    fun shareText(text: String, subject: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        val chooser = Intent.createChooser(send, subject).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
