package com.android.example.textrecognitionlive

import android.content.ContentValues.TAG
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ImageAnalyzerMet(private val overlayView: GraphicOverlay) : ImageAnalysis.Analyzer{

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var arrayOfProds = arrayListOf<ArrayList<Any>>()
    private var maxScore = 0.0
    private var maxMRPScore = 0.0
    private var finalProduct = String()
    private var finalMRP = String()
    private var finalResult = String()
    //recognizer.
    //val cameraSource = CameraSource.Builder(context, recognizer).build()

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        //val cameraSource = CameraSource.Builder(context, recognizer)

        Log.i(TAG,"iam")

        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Handle the recognized text
                    val ep = MainActivity()

                    val result = ep.extractProduct(visionText)
                    arrayOfProds.add(result)

                    val recognizedText = visionText.text
//                    val wordsArray = recognizedText.split("\\s+".toRegex()).toTypedArray()
//                    val wordsString = wordsArray.joinToString(prefix = "[", postfix = "]", separator = ", ")

                    //Picking max score product
                    val scorer = result[1].toString()
                    val mScore = result[3].toString()
                    val wordsArrayDisplay = result[5]
                    val mScoreArray = result[4]
                    val mrpScore = mScore.toDouble()
                    val intScorer = scorer.toDouble()
//                    if(intScorer>maxScore){
//                        maxScore = intScorer
                        finalProduct = result[0].toString()

//                    }
//                    if(mrpScore>maxMRPScore){
//                        maxMRPScore = mrpScore
                    finalMRP = if(mrpScore>0) {
                        result[2].toString()
                    } else{
                        "Not found"
                    }
//                    }
//Date Function
                    val dates = ep.extractDates(recognizedText)

                    //val wordsArray = result[5].joinToString(prefix = "[", postfix = "]", separator = ", ")
                    //Printing final
                    //frameIndex += 1
                    /*if(frameIndex == arrayBitmaps.size)
                    {
                        finalResult =
                            "Product: $finalProduct\n\n\nPrice:\n$finalMRP\n\n\nPrice Array$MscoreArray\n\nMRP Score:$maxMRPScore\n\n\nWords Array:$wordsArrayDisplay\n\n\nDate is: ${dates.first}\n${dates.second}\n\n${result[6]}"
                        //recognizedTextEt.setText(finalResult)
                        overlayView.updateElements(finalResult)
                        //progressDialog.dismiss()

                    }*/
                    finalResult =
                        "Product: $finalProduct\n"+
                                "Price:$finalMRP\n" +
                                "Price Array$mScoreArray\n" +
                                "MRP Score:$mScore\n" +
                                "Words Array:$wordsArrayDisplay\n" +
                                "Date is: ${dates.first}\n${dates.second}\n" +
                                "${result[6]}"
                    //recognizedTextEt.setText(finalResult)
                    Log.i(TAG,"fr:$finalResult")
                    overlayView.updateElements(finalResult)
                    //processTextRecognitionResult(visionText)
                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    Log.e(TAG, "Text recognition error", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }


    /*private fun processTextRecognitionResult(result: Text) {
        val elements = mutableListOf<Text.Element>()
        for (block in result.textBlocks) {
            for (line in block.lines) {
                elements.addAll(line.elements)

            }
        }
        overlayView.updateElements(elements)
    }*/




}