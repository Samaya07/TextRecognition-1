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
    private var finalMFG = String()
    private var finalEXP = String()
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

                    val productResult = ep.extractProduct(visionText)
                    val mrpResult = ep.extractMrp(visionText)
//                    arrayOfProds.add(result)

                    val recognizedText = visionText.text
//                    val wordsArray = recognizedText.split("\\s+".toRegex()).toTypedArray()
//                    val wordsString = wordsArray.joinToString(prefix = "[", postfix = "]", separator = ", ")

                    //Picking max score product
                    val pScore = productResult[1].toString().toDouble()
                    val mScore = mrpResult[1].toString().toDouble()
                    val wordsArrayDisplay = productResult[2]
                    val mScoreArray = mrpResult[2]

//Picking Final Product
                    if(pScore>maxScore){
                            finalProduct = productResult[0].toString()
                            maxScore = pScore
                    }
                    else if(pScore==maxScore){
                        if(productResult[0].toString().length>=finalProduct.length)
                        {
                            finalProduct = productResult[0].toString()
                            maxScore = pScore
                        }
                    }

//Picking Final MRP
                    if(mScore>maxMRPScore) {
                        if(mScore>=0.0){
                            finalMRP = mrpResult[0].toString()
                            maxMRPScore = mScore
                        }
                        else{
                            finalMRP = "Not found"
                        }

                    }

//Date Function
//                    val dates = ep.extractDates(recognizedText)
//                    val manufacturingDate = dates.first
//                    val expiryDate = dates.second
//                    if(manufacturingDate!=null && finalMFG!="") finalMFG = manufacturingDate
//
//                    if(expiryDate!=null && finalEXP!="") finalEXP = expiryDate
//
//                    if(manufacturingDate!=null && finalMFG.length < manufacturingDate.length) finalMFG = manufacturingDate
//
//                    if(expiryDate!=null && finalEXP.length < expiryDate.length) finalEXP = expiryDate
//
//                    if(manufacturingDate!=null && !finalMFG.contains("/") && manufacturingDate.contains("/")) finalMFG = manufacturingDate
//
//                    if(expiryDate!=null && !finalEXP.contains("/") && expiryDate.contains("/")) finalEXP = expiryDate

//                    val wordsArray = result[5].joinToString(prefix = "[", postfix = "]", separator = ", ")
//                    Printing final
//                    frameIndex += 1
                    /*if(frameIndex == arrayBitmaps.size)
                    {
                        finalResult =
                            "Product: $finalProduct\n\n\nPrice:\n$finalMRP\n\n\nPrice Array$MscoreArray\n\nMRP Score:$maxMRPScore\n\n\nWords Array:$wordsArrayDisplay\n\n\nDate is: ${dates.first}\n${dates.second}\n\n${result[6]}"
                        //recognizedTextEt.setText(finalResult)
                        overlayView.updateElements(finalResult)
                        //progressDialog.dismiss()

                    }*/
                    finalResult =
                                "Max product: ${productResult[0]}\n\n" +
                                "Score Prod: $pScore\n\n"        +
                                "Price:$finalMRP\n" +
                                "MRP Score:$mScore\n" +
                                "${mrpResult[3]}\n"+
                                "Price Array$mScoreArray\n\n" +
                                "Words Array:$wordsArrayDisplay\n\n" +
//                                "MFG date is: ${finalMFG}\nEXP date is: ${finalEXP}\n\n" +
                                "Final prod $finalProduct\n"+
                                "Final MRP $finalMRP\n" +
                                "Prod max score:$maxScore\n"+
                                "MRP max score: $maxMRPScore"
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