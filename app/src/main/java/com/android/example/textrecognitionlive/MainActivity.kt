package com.android.example.textrecognitionlive


//import androidx.room.jarjarred.org.antlr.v4.gui.Interpreter

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.android.example.textrecognitionlive.databinding.ActivityMainBinding
import com.android.example.textrecognitionlive.main.MainFragment
import com.google.firebase.ml.modeldownloader.CustomModel
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import org.tensorflow.lite.Interpreter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity(), LifecycleOwner {


    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var interpreter: Interpreter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.main_idkwhat)
        if(savedInstanceState == null)
        {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container,MainFragment.newInstance())
                .commitNow()
        }

        //downloadAndUpdateModel(this)

        // Start continuous model update every 3 days
        // startContinuousModelUpdate(this)

    }

    // Function to preprocess input text
    private fun preprocessText(text: String): IntArray {
        val word2idx = mapOf("PAD" to 0, "UNK" to 1, "The" to 2, "product" to 3, "will" to 4, "be" to 5, "available" to 6, "from" to 7, "2023-06-24." to 8)
        val words = text.split(" ")
        return words.map { word2idx[it] ?: word2idx["UNK"]!! }.toIntArray()
    }

    // Function to process model output into tags
    private fun processOutput(output: Array<FloatArray>): List<String> {
        val idx2tag = mapOf(0 to "O", 1 to "PRODUCT", 2 to "MRP", 3 to "EXP", 4 to "MFG")
        return output[0].map { idx2tag[it.roundToInt()] ?: "O" }
    }

    // Function to start continuous model update every 3 days
    private fun startContinuousModelUpdate(context: Context) {
        val executor = Executors.newSingleThreadScheduledExecutor()
        executor.scheduleAtFixedRate({
            downloadAndUpdateModel(context)
        }, 0, 3, TimeUnit.DAYS)
    }

    // Function to download and update model from Firebase ML
    private fun downloadAndUpdateModel(context: Context) {
        val conditions = CustomModelDownloadConditions.Builder().requireWifi().build()
        FirebaseModelDownloader.getInstance()
            .getModel("NER_Model", DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND, conditions)
            .addOnSuccessListener { model: CustomModel? ->
                val file = model?.file
                if (file != null) {
                    interpreter = Interpreter(file)
                    // Optionally, you can notify the user or update UI
                }
            }
            .addOnFailureListener {
                // Handle any errors
            }
    }
}





















