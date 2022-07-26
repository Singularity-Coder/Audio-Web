package com.singularitycoder.audioweb

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.singularitycoder.audioweb.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

// https://jsoup.org/cookbook/extracting-data/selector-syntax
// https://stackoverflow.com/questions/12526979/jsoup-get-all-links-from-a-page
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var dao: WebPageDao

    private lateinit var binding: ActivityMainBinding

    private var textToSpeech: TextToSpeech? = null
    private val webPageAdapter = WebPageAdapter()

    private val speechToTextResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
        result ?: return@registerForActivityResult
        binding.progressCircular.isVisible = true
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data: Intent? = result.data
        val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        binding.tvSearchResults.text = "Search results for \"${text?.firstOrNull()}\""
        println("Voice result: ${text?.firstOrNull()?.replace(" ", "+")}")
        val url = "https://www.google.com/search?q=${text?.firstOrNull()?.replace(" ", "+")}"
        parseWebPageWithWorker(firstUrl = url)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.setupUI()
        binding.setupUserActionListeners()
        observeForData()
    }

    override fun onResume() {
        super.onResume()
        initTextToSpeech()
    }

    override fun onPause() {
        super.onPause()
        if (textToSpeech?.isSpeaking == true) textToSpeech = null
    }

    private fun ActivityMainBinding.setupUI() {
        tvSearchResults.text = "Listen to the world!"
        rvWebPages.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = webPageAdapter
        }
    }

    private fun ActivityMainBinding.setupUserActionListeners() {
        root.setOnClickListener {
            textToSpeech?.stop()
            webPageAdapter.notifyDataSetChanged()
        }
        webPageAdapter.setWebPageClickListener { it: WebPage, isPlaying: Boolean ->
            textToSpeech?.stop()
            startTextToSpeech(it)
        }
        fabVoiceSearch.setOnClickListener {
            CoroutineScope(IO).launch {
                dao.deleteAll()
                withContext(Main) {
                    // Start Speech to Text
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Start Speaking Now!")
                    }
                    speechToTextResult.launch(intent)
//                    testQuery()
                }
            }
        }
    }

    private fun observeForData() {
        dao.getAllWebPageListLiveData().observe(this@MainActivity) { it: List<WebPage>? ->
            it ?: return@observe
            webPageAdapter.webPageList = it
            webPageAdapter.notifyDataSetChanged()
        }
    }

    private fun testQuery() {
        binding.progressCircular.isVisible = true
        val url = "https://www.google.com/search?q=news"
        binding.tvSearchResults.text = "Search results for \"News\""
        parseWebPageWithWorker(firstUrl = url)
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status: Int ->
            if (status == TextToSpeech.SUCCESS) {
                val result: Int? = textToSpeech?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    println("Language not supported for Text-to-Speech!")
                }
            }
        }
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                CoroutineScope(Main).launch {
                    println("Started Reading $utteranceId")
                }
            }

            override fun onDone(utteranceId: String) {
                CoroutineScope(Main).launch {
                    println("Finished reading $utteranceId")
                    webPageAdapter.notifyDataSetChanged()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                CoroutineScope(Main).launch {
                    println("Error reading $utteranceId")
                }
            }
        })
    }

    private fun startTextToSpeech(textToSpeak: WebPage) {
        val utteranceId = textToSpeak.title
        val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId) }
        textToSpeech?.apply {
            speak(textToSpeak.description, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            playSilentUtterance(1000, TextToSpeech.QUEUE_ADD, utteranceId) // Stay silent for 1 ms
        }
    }

    @SuppressLint("RestrictedApi")
    private fun parseWebPageWithWorker(firstUrl: String) {
        println("Worker Input: firstUrl: $firstUrl")
        val workConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val data = Data.Builder().apply {
            putString(FIRST_URL, firstUrl)
        }.build()
        val webPageParsingWorkRequest = OneTimeWorkRequestBuilder<WebPageParsingWorker>()
            .setInputData(data)
            .setConstraints(workConstraints)
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork(WORKER_TAG_WEB_PAGE_PARSER, ExistingWorkPolicy.KEEP, webPageParsingWorkRequest)
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(webPageParsingWorkRequest.id).observe(this) { workInfo: WorkInfo? ->
            when (workInfo?.state) {
                WorkInfo.State.RUNNING -> {
                    println("RUNNING: show Progress")
                    binding.progressCircular.isVisible = true
                }
                WorkInfo.State.ENQUEUED -> println("ENQUEUED: show Progress")
                WorkInfo.State.SUCCEEDED -> {
                    println("SUCCEEDED: showing Progress")
                    val isWorkComplete = workInfo.outputData.getBoolean(KEY_IS_WORK_COMPLETE, false)
                    if (webPageAdapter.webPageList.size > 100) {
                        WorkManager.getInstance(this@MainActivity).cancelAllWorkByTag(WORKER_TAG_WEB_PAGE_PARSER)
                    }
                    binding.progressCircular.isVisible = false
                }
                WorkInfo.State.FAILED -> {
                    println("FAILED: stop showing Progress")
                    binding.root.showSnackBar("Something went wrong!")
                    binding.progressCircular.isVisible = false
                }
                WorkInfo.State.BLOCKED -> println("BLOCKED: show Progress")
                WorkInfo.State.CANCELLED -> {
                    println("CANCELLED: stop showing Progress")
                    binding.progressCircular.isVisible = false
                }
                else -> Unit
            }
        }
    }
}
