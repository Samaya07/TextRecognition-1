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
import com.android.example.textrecognitionlive.util.ImageUtils
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executor

/*class ImageAnalyzerMet(private val detectionView: DetectionAreaView, private val recognizedTextView: TextView, private val previewView: PreviewView) : ImageAnalysis.Analyzer{

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

                    val detectionArea = detectionView.getDetectionArea()
                    val transformedDetectionArea = transformDetectionArea(detectionArea)
                    val imhei = mediaImage.height
                    val imwid = mediaImage.width
                    Log.i(TAG, "imageHei: $imhei")
                    Log.i(TAG,"imageWid: $imwid")

                    val filteredBlocks = visionText.textBlocks.filter { block ->
                        block.boundingBox?.let { boundingBox ->
                            transformedDetectionArea.contains(
                                boundingBox.left.toFloat(),
                                boundingBox.top.toFloat()
                            ) && transformedDetectionArea.contains(
                                boundingBox.right.toFloat(),
                                boundingBox.bottom.toFloat()
                            )
                        } ?: false
                    }
                    handleRecognizedText(filteredBlocks)

                    // Handle the recognized text
                    /*val ep = MainActivity()

                    val result = ep.extractProduct(visionText)
                    arrayOfProds.add(result)

                    val recognizedText = visionText.text
                    val wordsArray = recognizedText.split("\\s+".toRegex()).toTypedArray()
                    val wordsString = wordsArray.joinToString(prefix = "[", postfix = "]", separator = ", ")

                    //Picking max score product
                    val scorer = result[1].toString()
                    val mScore = result[3].toString()
                    val wordsArrayDisplay = result[5]
                    val mScoreArray = result[4]
                    val mrpScore = mScore.toDouble()
                    val intScorer = scorer.toDouble()
                    if(intScorer>maxScore){
                        maxScore = intScorer
                        finalProduct = result[0].toString()
                    }
                    if(mrpScore>maxMRPScore){
                        maxMRPScore = mrpScore
                        finalMRP = result[2].toString()
                    }
//Date Function
                    val dates = ep.extractDates(wordsString)

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
                                "MRP Score:$maxMRPScore\n" +
                                "Words Array:$wordsArrayDisplay\n" +
                                "Date is: ${dates.first}\n${dates.second}\n" +
                                "${result[6]}"
                    //recognizedTextEt.setText(finalResult)
                    Log.i(TAG,"fr:$finalResult")
                    overlayView.updateElements(finalResult)*/
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

    private fun transformDetectionArea(detectionArea: RectF): RectF {
        val previewWidth = previewView.width
        val previewHeight = previewView.height
        Log.i(TAG,"preWid: $previewWidth, preHei: $previewHeight")
        //Log.i(TAG, "dlef: ${detectionArea.left}, dtop: ${detectionArea.top}, dri: ${detectionArea.right}, dbot: ${detectionArea.bottom}")
        Log.i(TAG, "detWid: ${detectionView.width}, detHei: ${detectionView.height}")

        val left = detectionArea.left * previewWidth / detectionView.width
        val top = detectionArea.top * previewHeight / detectionView.height
        val right = detectionArea.right * previewWidth / detectionView.width
        val bottom = detectionArea.bottom * previewHeight / detectionView.height

        Log.i(TAG, "left:$left, top:$top, right: $right, bottom: $bottom")

        return RectF(left, top, right, bottom)
    }

    private fun handleRecognizedText(filteredBlocks: List<Text.TextBlock>)
    {
        //val ma = MainActivity()
        var finalEle = ""
        for( block in filteredBlocks)
        {
            //finalEle += block

            finalEle = "$finalEle ${block.text}"

        }
        recognizedTextView.text = finalEle

    }

}*/

class ImageAnalyzerMet(
    private val context: Context,
    lifecycle: Lifecycle,
    executor: Executor,
    private val imageCropPercentages: MutableLiveData<Pair<Int, Int>>,
    private val recognizedTextView: TextView
) : ImageAnalysis.Analyzer {
    private val detector =
        TextRecognition.getClient(TextRecognizerOptions.Builder().setExecutor(executor).build())

    init {
        lifecycle.addObserver(detector)
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        // We requested a setTargetAspectRatio, but it's not guaranteed that's what the camera
        // stack is able to support, so we calculate the actual ratio from the first frame to
        // know how to appropriately crop the image we want to analyze.
        val imageHeight = mediaImage.height
        val imageWidth = mediaImage.width

        val actualAspectRatio = imageWidth / imageHeight

        val convertImageToBitmap = ImageUtils.convertYuv420888ImageToBitmap(mediaImage)
        val cropRect = Rect(0, 0, imageWidth, imageHeight)

        // If the image has a way wider aspect ratio than expected, crop less of the height so we
        // don't end up cropping too much of the image. If the image has a way taller aspect ratio
        // than expected, we don't have to make any changes to our cropping so we don't handle it
        // here.
        val currentCropPercentages = imageCropPercentages.value ?: return
        if (actualAspectRatio > 3) {
            val originalHeightCropPercentage = currentCropPercentages.first
            val originalWidthCropPercentage = currentCropPercentages.second
            imageCropPercentages.value =
                Pair(originalHeightCropPercentage / 2, originalWidthCropPercentage)
        }

        // If the image is rotated by 90 (or 270) degrees, swap height and width when calculating
        // the crop.
        val cropPercentages = imageCropPercentages.value ?: return
        val heightCropPercent = cropPercentages.first
        val widthCropPercent = cropPercentages.second
        val (widthCrop, heightCrop) = when (rotationDegrees) {
            90, 270 -> Pair(heightCropPercent / 100f, widthCropPercent / 100f)
            else -> Pair(widthCropPercent / 100f, heightCropPercent / 100f)
        }

        cropRect.inset(
            (imageWidth * widthCrop / 2).toInt(),
            (imageHeight * heightCrop / 2).toInt()
        )
        val croppedBitmap =
            ImageUtils.rotateAndCrop(convertImageToBitmap, rotationDegrees, cropRect)
        recognizeTextOnDevice(InputImage.fromBitmap(croppedBitmap, 0)).addOnCompleteListener {
            imageProxy.close()
        }
    }

    private fun recognizeTextOnDevice(
        image: InputImage
    ): Task<Text> {
        // Pass image to an ML Kit Vision API
        return detector.process(image)
            .addOnSuccessListener { visionText ->
                // Task completed successfully
                //result.value = visionText.text
                recognizedTextView.text = visionText.text

            }
            .addOnFailureListener { exception ->
                // Task failed with an exception
                Log.e(TAG, "Text recognition error", exception)
                val message = getErrorMessage(exception)
                message?.let {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun getErrorMessage(exception: Exception): String? {
        val mlKitException = exception as? MlKitException ?: return exception.message
        return if (mlKitException.errorCode == MlKitException.UNAVAILABLE) {
            "Waiting for text recognition model to be downloaded"
        } else exception.message
    }
}