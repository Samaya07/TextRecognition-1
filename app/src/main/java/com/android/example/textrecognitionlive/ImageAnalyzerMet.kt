package com.android.example.textrecognitionlive

import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.android.example.textrecognitionlive.extraction.ExtractionFuns
import com.android.example.textrecognitionlive.main.MainFragment
import com.android.example.textrecognitionlive.util.ImageUtils
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executor
import kotlin.time.TimeSource

class ImageAnalyzerMet(
    private val context: Context,
    lifecycle: Lifecycle,
    executor: Executor,
    private val imageCropPercentages: MutableLiveData<Pair<Int, Int>>,
    private val recognizedTextView: TextView,
    private val mark2: TimeSource.Monotonic.ValueTimeMark
) : ImageAnalysis.Analyzer {

    private val latinDetector = TextRecognition.getClient(TextRecognizerOptions.Builder().setExecutor(executor).build())
    private val devanagariDetector = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())

    private var rupeeLine = arrayListOf<Double>()
    private var maxScore = 0.0
    private var maxMRPScore = 0.0
    private var toastFlag = true
    private var maxDateScore = 0.4

    private var finalResult = ""
    private var finalProduct = ""
    private var finalMRP = ""
    private var finalMFG = ""
    private var finalEXP = ""

    companion object {
        var finalProduct = ""
        var finalProdDisplay = ""
        var finalMRP = ""
        var finalMFG = ""
        var finalEXP = ""
    }

    init {
        lifecycle.addObserver(latinDetector)
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val imageHeight = mediaImage.height
        val imageWidth = mediaImage.width
        val actualAspectRatio = imageWidth / imageHeight

        val convertImageToBitmap = ImageUtils.convertYuv420888ImageToBitmap(mediaImage)
        val cropRect = Rect(0, 0, imageWidth, imageHeight)
        val currentCropPercentages = imageCropPercentages.value ?: return

        if (actualAspectRatio > 3) {
            val (originalHeightCropPercentage, originalWidthCropPercentage) = currentCropPercentages
            imageCropPercentages.value = Pair(originalHeightCropPercentage / 2, originalWidthCropPercentage)
        }

        val cropPercentages = imageCropPercentages.value ?: return
        val (heightCropPercent, widthCropPercent) = cropPercentages
        val (widthCrop, heightCrop) = when (rotationDegrees) {
            90, 270 -> Pair(heightCropPercent / 100f, widthCropPercent / 100f)
            else -> Pair(widthCropPercent / 100f, heightCropPercent / 100f)
        }

        cropRect.inset((imageWidth * widthCrop / 2).toInt(), (imageHeight * heightCrop / 2).toInt())
        val croppedBitmap = ImageUtils.rotateAndCrop(convertImageToBitmap, rotationDegrees, cropRect)
        recognizeTextOnDevice(InputImage.fromBitmap(croppedBitmap, 0)).addOnCompleteListener {
            imageProxy.close()
        }
    }

    private fun recognizeTextOnDevice(image: InputImage): Task<Text> {
        return latinDetector.process(image)
            .addOnSuccessListener { visionText ->
                processDevanagariText(image)
                processVisionText(visionText)
                updateRecognizedTextView()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Text recognition error", exception)
                getErrorMessage(exception)?.let {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun processDevanagariText(image: InputImage) {
        devanagariDetector.process(image)
            .addOnSuccessListener { hindiText ->
                rupeeLine.clear()
                for (block in hindiText.textBlocks) {
                    for (line in block.lines) {
                        for ((i, element) in line.elements.withIndex()) {
                            var rupee = ""
                            if (element.text.contains("\u20B9")) {
                                rupee += if (element.text.length > 1) {
                                    element.text.replace("\u20B9", "")
                                } else {
                                    line.elements.getOrNull(i + 1)?.text ?: continue
                                }
                            }
                            rupee.split("[\\s:;.]".toRegex())
                                .filter { it.isNotEmpty() }
                                .mapNotNull { it.toDoubleOrNull() }
                                .forEach { rupeeLine.add(it) }
                        }
                    }
                }
            }
    }

    private fun processVisionText(visionText: Text) {
        MainFragment.tokens.addAll(visionText.textBlocks.flatMap { it.lines }.flatMap { it.elements }.map { it.text })

        val productResult = ExtractionFuns.extractProduct(visionText)
        val mrpResult = ExtractionFuns.extractMrp(visionText, rupeeLine)
        val dates = ExtractionFuns.extractDates(visionText)

        val (dateResult, dateResult2) = dates.take(2)
        val (dScore1, dScore2) = dates.drop(2).map { it.toString().toDouble() }
        val dScoreAvg = (dScore1 + dScore2) / 2

        if (dScoreAvg >= maxDateScore) {
            maxDateScore = dScoreAvg
            finalMFG = dateResult.toString()
            finalEXP = if (dScore2 >= 0.4) dateResult2.toString() else "Not found"
        }

        var pScore = productResult[1].toString().toDouble()
        val mScore = mrpResult[1].toString().toDouble()

        if (maxDateScore >= 0.5) {
            pScore -= 0.4
        }

        if (pScore > maxScore) {
            finalProduct = productResult[0].toString()
            maxScore = pScore
        } else if (pScore == maxScore && productResult[0].toString().length >= finalProduct.length) {
            finalProduct = productResult[0].toString()
            maxScore = pScore
        }

        if (mScore > maxMRPScore) {
            finalMRP = if (mScore > 0.0) mrpResult[0].toString() else "Not found"
            maxMRPScore = mScore
        }

        finalResult = buildFinalResult(productResult, mrpResult, dScore2, dates)
    }

    private fun buildFinalResult(productResult: List<Any>, mrpResult: List<Any>, dScore2: Double, dates: List<Any>): String {
        return """
            Max product: ${productResult[0]}
            Score Prod: ${productResult[1]}
            Price: ${mrpResult[0]}
            MRP Score: ${mrpResult[1]}
            ${mrpResult[3]}
            Price Array: ${mrpResult[2]}
            Words Array: ${productResult[2]}
            Score of Date: $maxDateScore
            Score of exp: $dScore2
            Prod max score: $maxScore
            MRP max score: $maxMRPScore
            MFG date is: $finalMFG
            EXP date is: $finalEXP
            Final prod: $finalProduct
            Final MRP: $finalMRP
        """.trimIndent()
    }

    private fun updateRecognizedTextView() {
        recognizedTextView.text = finalResult
    }

    private fun getErrorMessage(exception: Exception): String? {
        val mlKitException = exception as? MlKitException ?: return exception.message
        return if (mlKitException.errorCode == MlKitException.UNAVAILABLE) {
            "Waiting for text recognition model to be downloaded"
        } else exception.message
    }
}













///////THIS IS BEFORE OPTIMIZATION



//package com.android.example.textrecognitionlive
//
//import android.content.ContentValues.TAG
//import android.content.Context
//import android.graphics.Rect
//import android.util.Log
//import android.widget.TextView
//import android.widget.Toast
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.ImageProxy
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.MutableLiveData
//import com.android.example.textrecognitionlive.extraction.ExtractionFuns
//import com.android.example.textrecognitionlive.main.MainFragment
//import com.android.example.textrecognitionlive.util.ImageUtils
//import com.google.android.gms.tasks.Task
//import com.google.mlkit.common.MlKitException
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.text.Text
//import com.google.mlkit.vision.text.TextRecognition
//import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
//import com.google.mlkit.vision.text.latin.TextRecognizerOptions
//import java.util.concurrent.Executor
//import kotlin.time.TimeSource
//
//
//class ImageAnalyzerMet(
//    private val context: Context,
//    lifecycle: Lifecycle,
//    executor: Executor,
//    private val imageCropPercentages: MutableLiveData<Pair<Int, Int>>,
//    private val recognizedTextView: TextView,
//    private val mark2: TimeSource.Monotonic.ValueTimeMark
//) : ImageAnalysis.Analyzer {
//    private val detector =
//        TextRecognition.getClient(TextRecognizerOptions.Builder().setExecutor(executor).build())
//    private val devanagiriDetector = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
//    private var rupeeLine = arrayListOf <Double>()
//
////    private val detect3 = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
//
//
//    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//
//    private var arrayOfProds = arrayListOf<ArrayList<Any>>()
//    private var maxScore = 0.0
//    private var maxMRPScore = 0.0
//    private var toastFlag = true
//
//    //    private var maxMFGScore = 0.5
////    private var maxEXPScore = 0.5
//    private var maxDateScore = 0.4
//    private var finalResult = String()
//    private var finalProduct = String()
//    private var finalMRP = String()
//    private var finalMFG = String()
//    private var finalEXP = String()
//
//    companion object {
//        var finalProduct = String()
//        var finalProdDisplay = String()
//        var finalMRP = String()
//        var finalMFG = String()
//        var finalEXP = String()
//    }
//
//    init {
//        lifecycle.addObserver(detector)
//    }
//
//
//    @androidx.camera.core.ExperimentalGetImage
//    override fun analyze(imageProxy: ImageProxy) {
//        val mediaImage = imageProxy.image ?: return
//
//        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
//
//        // We requested a setTargetAspectRatio, but it's not guaranteed that's what the camera
//        // stack is able to support, so we calculate the actual ratio from the first frame to
//        // know how to appropriately crop the image we want to analyze.
//        val imageHeight = mediaImage.height
//        val imageWidth = mediaImage.width
//
//        val actualAspectRatio = imageWidth / imageHeight
//
//        val convertImageToBitmap = ImageUtils.convertYuv420888ImageToBitmap(mediaImage)
//        val cropRect = Rect(0, 0, imageWidth, imageHeight)
//
//        // If the image has a way wider aspect ratio than expected, crop less of the height so we
//        // don't end up cropping too much of the image. If the image has a way taller aspect ratio
//        // than expected, we don't have to make any changes to our cropping so we don't handle it
//        // here.
//        val currentCropPercentages = imageCropPercentages.value ?: return
//        if (actualAspectRatio > 3) {
//            val originalHeightCropPercentage = currentCropPercentages.first
//            val originalWidthCropPercentage = currentCropPercentages.second
//            imageCropPercentages.value =
//                Pair(originalHeightCropPercentage / 2, originalWidthCropPercentage)
//        }
//
//        // If the image is rotated by 90 (or 270) degrees, swap height and width when calculating
//        // the crop.
//        val cropPercentages = imageCropPercentages.value ?: return
//        val heightCropPercent = cropPercentages.first
//        val widthCropPercent = cropPercentages.second
//        val (widthCrop, heightCrop) = when (rotationDegrees) {
//            90, 270 -> Pair(heightCropPercent / 100f, widthCropPercent / 100f)
//            else -> Pair(widthCropPercent / 100f, heightCropPercent / 100f)
//        }
//
//        cropRect.inset(
//            (imageWidth * widthCrop / 2).toInt(),
//            (imageHeight * heightCrop / 2).toInt()
//        )
//        val croppedBitmap =
//            ImageUtils.rotateAndCrop(convertImageToBitmap, rotationDegrees, cropRect)
//        recognizeTextOnDevice(InputImage.fromBitmap(croppedBitmap, 0)).addOnCompleteListener {
//            imageProxy.close()
//        }
//    }
//
//    private fun recognizeTextOnDevice(
//        image: InputImage
//    ): Task<Text> {
//        // Pass image to an ML Kit Vision API
//
//        //detect2.process()
//        return detector.process(image)
//            .addOnSuccessListener { visionText ->
//                // Task completed successfully
//                //result.value = visionText.text
//                //recognizedTextView.text = visionText.text
//                //val b1 = visionText.textBlocks
//
//
//                devanagiriDetector.process(image)
//                    .addOnSuccessListener { hindiText ->
//                        var rupee = ""
//                        var filteredRupee = listOf<Any>("")
//                        rupeeLine.clear()
//                        for (block in hindiText.textBlocks) {
//                            for (line in block.lines) {
//                                for ((i, element) in line.elements.withIndex()) {
//                                    if (element.text.contains("\u20B9")) {
//                                        if(element.text.length > 1) {
//                                            rupee += element.text.replace("\u20B9", "")
//                                        } else {
//                                            // Check if there is a next element
//                                            if (i + 1 < line.elements.size) {
//                                                rupee += line.elements[i + 1].text
//                                            } else {
//                                                continue
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                        filteredRupee = rupee.split("[\\s:;.]".toRegex()).filter { it.isNotEmpty() }
//                        for (each in filteredRupee)
//                        {
//                            each.toDoubleOrNull()?.let { rupeeLine.add(it) }
//                        }
//                            //rupeeLine = rupee.split("[\\s:;.]".toRegex()).filter { it.isNotEmpty() }
//
//                        //Toast.makeText(context, "$rupeeLine", Toast.LENGTH_SHORT).show()
//
//                    }
//
//
//
//                for (block in visionText.textBlocks) {
//                    for (line in block.lines) {
//                        for (element in line.elements) {
//                            MainFragment.tokens.add(element.text)
//                        }
//                    }
//                }
//
//
//                val productResult = ExtractionFuns.extractProduct(visionText)
//                val mrpResult = ExtractionFuns.extractMrp(visionText, rupeeLine)
//                val dates = ExtractionFuns.extractDates(visionText)
////                    arrayOfProds.add(result)
//
//                val recognizedText = visionText.text
//
//
////Date Function
//                val dateResult = dates[0].toString()
//                val dScore1 = dates[2].toString().toDouble()
//                val dateResult2 = dates[1].toString()
//                val dScore2 = dates[3].toString().toDouble()
//                val dScoreAvg = (dScore1+dScore2)/2
////                if(dScore1>maxMFGScore){
////                    finalMFG = dateResult
////                    maxMFGScore = dScore1
////                }
////                if(dScore2>maxEXPScore){
////                    finalEXP = dateResult2
////                    maxEXPScore = dScore2
////                }
//
////TEST thershold
//                if(dScoreAvg>=maxDateScore){
//                    if(dScore2>=0.4) {
//                        maxDateScore = dScoreAvg
//                        finalMFG = dateResult
//                        finalEXP = dateResult2
//                    }
//                    else{
//                        maxDateScore = dScoreAvg
//                        finalMFG = dateResult
//                        finalEXP = "Not found"
//                    }
//                }
//
//                //Picking max score product
//                var pScore = productResult[1].toString().toDouble()
//                val mScore = mrpResult[1].toString().toDouble()
//                val wordsArrayDisplay = productResult[2]
//                val mScoreArray = mrpResult[2]
//
////Picking Final Product
////TEST
//                //date and prod relation condition
//                if(maxDateScore>=0.5) pScore -= 0.4
//                if(pScore>maxScore){
//                    finalProduct = productResult[0].toString()
//                    maxScore = pScore
//                }
//                else if(pScore==maxScore){
//                    if(productResult[0].toString().length>=finalProduct.length)
//                    {
//                        finalProduct = productResult[0].toString()
//                        maxScore = pScore
//                    }
//                }
//
////Picking Final MRP
//                if(mScore>maxMRPScore) {
//                    if(mScore>0.0){
//                        finalMRP = mrpResult[0].toString()
//                        maxMRPScore = mScore
//                    }
//                    else{
//                        finalMRP = "Not found"
//                    }
//
//                }
//
//
////                    else{
////                        maxDateScore = dScore1
////                        finalMFG = "Not found"
////                        finalEXP = "Not found"
////                    }
////                }
////                if(dScore2<=maxEXPScore) {
////                    maxEXPScore = dScore2
////                    finalEXP = dates[1].toString()
////                }
//                //val manufacturingDate = dates
////                    val expiryDate = dates.second
////                    if(manufacturingDate!=null && finalMFG!="") finalMFG = manufacturingDate
////
////                    if(expiryDate!=null && finalEXP!="") finalEXP = expiryDate
////
////                    if(manufacturingDate!=null && finalMFG.length < manufacturingDate.length && manufacturingDate.contains("[/-.//s]")) finalMFG = manufacturingDate
////
////                    if(expiryDate!=null && finalEXP.length < expiryDate.length && expiryDate.contains("[/-.//s]")) finalEXP = expiryDate
////
////                    if(manufacturingDate!=null && !finalMFG.contains("/") && manufacturingDate.contains("/")) finalMFG = manufacturingDate
////
////                    if(expiryDate!=null && !finalEXP.contains("/") && expiryDate.contains("/")) finalEXP = expiryDate
//
////                    val wordsArray = result[5].joinToString(prefix = "[", postfix = "]", separator = ", ")
////                    Printing final
////                    frameIndex += 1
//                /*if(frameIndex == arrayBitmaps.size)
//                {
//                    finalResult =
//                        "Product: $finalProduct\n\n\nPrice:\n$finalMRP\n\n\nPrice Array$MscoreArray\n\nMRP Score:$maxMRPScore\n\n\nWords Array:$wordsArrayDisplay\n\n\nDate is: ${dates.first}\n${dates.second}\n\n${result[6]}"
//                    //recognizedTextEt.setText(finalResult)
//                    overlayView.updateElements(finalResult)
//                    //progressDialog.dismiss()
//
//                }*/
//
//
//
//                /*if(mark2.hasNotPassedNow())
//                {
//                    finalProdDisplay = finalProduct
//                }
//                else if(mark2.hasPassedNow() && toastFlag)
//                {
//                    Toast.makeText(context, "Scan the mrp and dates", Toast.LENGTH_SHORT).show()
//                    toastFlag = false
//                }*/
//
//                finalResult =
//                    "Max product: ${productResult[0]}\n\n" +
//                            "Score Prod: $pScore\n\n"+
//                            "Price:${mrpResult[0]}\n" +
//                            "MRP Score:$mScore\n" +
//                            "${mrpResult[3]}\n"+
//                            "Price Array$mScoreArray\n\n" +
//                            "Words Array:$wordsArrayDisplay\n\n" +
//                            "Score of Date: ${maxDateScore}\n"+
//                            "Score of exp: ${dScore2}\n"+
//                            "Prod max score:$maxScore\n"+
//                            "MRP max score: $maxMRPScore\n\n"+
//                            "MFG date is: ${finalMFG}\n" +
//                            "EXP date is: ${finalEXP}\n"+
//                            "Final prod $finalProduct\n"+
//                            "Final MRP $finalMRP\n"
//
//
//
//                recognizedTextView.text = finalResult
//
//            }
//            .addOnFailureListener { exception ->
//                // Task failed with an exception
//                Log.e(TAG, "Text recognition error", exception)
//                val message = getErrorMessage(exception)
//                message?.let {
//                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
//                }
//            }
//    }
//
//
//
//    private fun getErrorMessage(exception: Exception): String? {
//        val mlKitException = exception as? MlKitException ?: return exception.message
//        return if (mlKitException.errorCode == MlKitException.UNAVAILABLE) {
//            "Waiting for text recognition model to be downloaded"
//        } else exception.message
//    }
//
//
//}

///////THIS IS BEFORE OPTIMIZATION END
