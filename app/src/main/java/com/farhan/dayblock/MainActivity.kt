package com.farhan.dayblock

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.farhan.dayblock.data.DayblockDatabase
import com.farhan.dayblock.data.DiaryEntry
import com.farhan.dayblock.data.DiaryRepository
import com.farhan.dayblock.domain.DiaryStatsCalculator
import java.time.LocalDate
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * Dayblock's interface is written in HTML/CSS (assets/dayblock/index.html)
 * and shown in a WebView. Kotlin stays in charge of the data: the page
 * talks to [DayblockBridge], which reads and writes the same Room
 * database the app has always used, so existing entries are untouched.
 *
 * The native side also covers what a WebView cannot do well: the daily
 * reminder alarm, fingerprint unlock, and speech-to-text dictation.
 */
class MainActivity : FragmentActivity() {

    private lateinit var webView: WebView
    private var speechRecognizer: SpeechRecognizer? = null
    private var pendingVoiceStart = false

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

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best effort */ }

    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted && pendingVoiceStart) {
                startListening()
            } else if (!granted) {
                evalJs("window.onDayblockVoiceError && window.onDayblockVoiceError('Microphone permission was not granted, so voice typing cannot run.')")
            }
            pendingVoiceStart = false
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = DiaryRepository(
            DayblockDatabase.create(application).diaryDao()
        )

        webView = WebView(this).apply {
            // Cream paper colour, so there is no white flash before the page loads.
            setBackgroundColor(0xFFFFF8EC.toInt())
            settings.javaScriptEnabled = true
            // Lets the page remember the dark/light theme between opens.
            settings.domStorageEnabled = true
            addJavascriptInterface(
                DayblockBridge(repository, this@MainActivity) { script -> evalJs(script) },
                "DayblockBridge"
            )
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

        // Ask once for notification permission so the writing reminder can show.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }

    /** Runs a snippet in the page, always on the UI thread. */
    fun evalJs(script: String) {
        runOnUiThread { if (::webView.isInitialized) webView.evaluateJavascript(script, null) }
    }

    /** True when this phone has a fingerprint (or face) sensor enrolled. */
    fun fingerprintAvailable(): Boolean =
        BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS

    /** Shows the system biometric prompt and reports back into the page. */
    fun requestFingerprint() {
        runOnUiThread {
            if (!fingerprintAvailable()) {
                evalJs("window.onDayblockFingerprint && window.onDayblockFingerprint(false)")
                return@runOnUiThread
            }
            val prompt = BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        evalJs("window.onDayblockFingerprint && window.onDayblockFingerprint(true)")
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        evalJs("window.onDayblockFingerprint && window.onDayblockFingerprint(false)")
                    }
                }
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Dayblock")
                .setSubtitle("Your diary, your fingerprint")
                .setNegativeButtonText("Use PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()
            prompt.authenticate(info)
        }
    }

    /** Starts dictation, asking for the microphone first when needed. */
    fun startVoice() {
        runOnUiThread {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                pendingVoiceStart = true
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return@runOnUiThread
            }
            startListening()
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            evalJs("window.onDayblockVoiceError && window.onDayblockVoiceError('No speech service is available on this phone.')")
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                    if (!text.isNullOrEmpty()) {
                        evalJs(
                            "window.onDayblockVoice && window.onDayblockVoice(" +
                                JSONObject.quote(text) + ")"
                        )
                    } else {
                        evalJs("window.onDayblockVoiceError && window.onDayblockVoiceError('I could not hear that. Try again, a little closer to the phone.')")
                    }
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH ->
                            "I could not make out words that time. Try again."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            "No speech heard. Tap Voice and try again."
                        else -> "Voice typing stopped (error $error). Typing still works."
                    }
                    evalJs(
                        "window.onDayblockVoiceError && window.onDayblockVoiceError(" +
                            JSONObject.quote(message) + ")"
                    )
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your diary entry")
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            evalJs("window.onDayblockVoiceError && window.onDayblockVoiceError('Voice typing could not start on this phone.')")
        }
    }
}

/** The small bridge the HTML page uses to reach the diary database and native features. */
class DayblockBridge(
    private val repository: DiaryRepository,
    private val activity: MainActivity,
    private val evalJs: (String) -> Unit
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
        activity.startActivity(chooser)
    }

    /** Schedules the daily writing reminder at the given local time. */
    @JavascriptInterface
    fun setReminder(hour: Int, minute: Int) {
        val alarmManager =
            activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = reminderPendingIntent()
        val first = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            first.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pending
        )
    }

    @JavascriptInterface
    fun cancelReminder() {
        val alarmManager =
            activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(reminderPendingIntent())
    }

    private fun reminderPendingIntent(): PendingIntent {
        val intent = Intent(activity, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            activity, 42, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @JavascriptInterface
    fun fingerprintAvailable(): Boolean = activity.fingerprintAvailable()

    @JavascriptInterface
    fun requestFingerprint() {
        activity.requestFingerprint()
    }

    @JavascriptInterface
    fun startVoice() {
        activity.startVoice()
    }
}
