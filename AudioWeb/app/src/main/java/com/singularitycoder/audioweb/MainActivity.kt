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
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.util.*
import kotlin.collections.ArrayList

// https://jsoup.org/cookbook/extracting-data/selector-syntax
// https://stackoverflow.com/questions/12526979/jsoup-get-all-links-from-a-page
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var textToSpeech: TextToSpeech? = null

    private val speechToTextResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
        result ?: return@registerForActivityResult
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data: Intent? = result.data
        val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        println("Voice result: ${text?.firstOrNull()?.replace(" ", "+")}")
        val url = "https://www.google.com/search?q=${text?.firstOrNull()?.replace(" ", "+")}"
        parseWebPageWithWorker(firstUrl = url)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.setupUI()
        initTextToSpeech()
    }

    override fun onPause() {
        super.onPause()
        if (textToSpeech?.isSpeaking == true) textToSpeech = null
    }

    private fun ActivityMainBinding.setupUI() {
        rvWebPages.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = WebPageAdapter()
        }
        (rvWebPages.adapter as WebPageAdapter).setWebPageClickListener { it: WebPage ->
            startTextToSpeech(it.description)
        }
        fabVoiceSearch.setOnClickListener {
            // Start Speech to Text
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Start Speaking Now!")
            }
            speechToTextResult.launch(intent)
        }
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
                CoroutineScope(Dispatchers.Main).launch { binding.root.showSnackBar("Started reading $utteranceId") }
            }

            override fun onDone(utteranceId: String) {
                CoroutineScope(Dispatchers.Main).launch { binding.root.showSnackBar("Finished reading $utteranceId") }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                CoroutineScope(Dispatchers.Main).launch { binding.root.showSnackBar("Error reading $utteranceId") }
            }
        })
    }

    private fun startTextToSpeech(textToSpeak: String) {
        val utteranceId = getString(R.string.app_name)
        val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId) }
        textToSpeech?.apply {
            speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            playSilentUtterance(1, TextToSpeech.QUEUE_ADD, utteranceId) // Stay silent for 1 ms
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
                    val imageUrlArrayFromWorker = workInfo.outputData.getStringArray(KEY_WEB_PAGE_IMAGE_URL_ARRAY)
                    val titleArrayFromWorker = workInfo.outputData.getStringArray(KEY_WEB_PAGE_TITLE_ARRAY)
                    val pageUrlArrayFromWorker = workInfo.outputData.getStringArray(KEY_WEB_PAGE_PAGE_URL_ARRAY)
                    val descArrayFromWorker = workInfo.outputData.getStringArray(KEY_WEB_PAGE_DESC_ARRAY)

                    val webPageList = ArrayList<WebPage>()
                    CoroutineScope(IO).launch {
                        imageUrlArrayFromWorker?.forEachIndexed { index, s ->
                            webPageList.add(
                                WebPage(
                                    imageUrl = imageUrlArrayFromWorker.get(index),
                                    title = titleArrayFromWorker?.get(index) ?: "",
                                    pageUrl = pageUrlArrayFromWorker?.get(index) ?: "",
                                    description = descArrayFromWorker?.get(index) ?: ""
                                )
                            )
                        }

                        withContext(Main) {
                            (binding.rvWebPages.adapter as WebPageAdapter).apply {
                                this.webPageList = webPageList
                                notifyDataSetChanged()
                            }
                            if (webPageList.size > 100) {
                                WorkManager.getInstance(this@MainActivity).cancelAllWorkByTag(WORKER_TAG_WEB_PAGE_PARSER)
                            }
                            binding.progressCircular.isVisible = false
                        }
                    }
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

private const val WORKER_TAG_WEB_PAGE_PARSER = "WORKER_TAG_WEB_PAGE_PARSER"

const val FIRST_URL = "FIRST_URL"

const val KEY_WEB_PAGE_IMAGE_URL_ARRAY = "KEY_WEB_PAGE_IMAGE_URL_ARRAY"
const val KEY_WEB_PAGE_TITLE_ARRAY = "KEY_WEB_PAGE_TITLE_ARRAY"
const val KEY_WEB_PAGE_PAGE_URL_ARRAY = "KEY_WEB_PAGE_PAGE_URL_ARRAY"
const val KEY_WEB_PAGE_DESC_ARRAY = "KEY_WEB_PAGE_DESC_ARRAY"
