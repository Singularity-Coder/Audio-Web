package com.singularitycoder.audioweb

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.singularitycoder.audioweb.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var textToSpeech: TextToSpeech? = null

    private val speechToTextResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
        result ?: return@registerForActivityResult
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data: Intent? = result.data
        val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
//        binding.customSearch.getSearchView().setText(text?.firstOrNull())
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

            override fun onError(utteranceId: String) {
                CoroutineScope(Dispatchers.Main).launch { binding.root.showSnackBar("Error reading $utteranceId") }
            }
        })
    }

    private fun startTextToSpeech() {
        val utteranceId = getString(R.string.app_name)
        val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId) }
        val textToSpeak = "get some website description here"
        textToSpeech?.apply {
            speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            playSilentUtterance(1, TextToSpeech.QUEUE_ADD, utteranceId) // Stay silent for 1 ms
        }
    }
}