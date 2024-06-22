package com.android.example.textrecognitionlive

import android.content.ContentValues
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import java.util.regex.Pattern
import kotlin.math.sqrt

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

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var arrayOfProds = arrayListOf<ArrayList<Any>>()
    private var maxScore = 0.0
    private var maxMRPScore = 0.0
    private var finalProduct = String()
    private var finalMRP = String()
    private var finalResult = String()
    private var finalMFG = String()
    private var finalEXP = String()

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
                //recognizedTextView.text = visionText.text

                val productResult = extractProduct(visionText)
                val mrpResult = extractMrp(visionText)
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
                if(pScore>=maxScore){
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
                val dates = extractDates(recognizedText)
                val manufacturingDate = dates.first
                val expiryDate = dates.second
                if(manufacturingDate!=null && finalMFG!="") finalMFG = manufacturingDate

                if(expiryDate!=null && finalEXP!="") finalEXP = expiryDate

                if(manufacturingDate!=null && finalMFG.length < manufacturingDate.length) finalMFG = manufacturingDate

                if(expiryDate!=null && finalEXP.length < expiryDate.length) finalEXP = expiryDate

                if(manufacturingDate!=null && !finalMFG.contains("/") && manufacturingDate.contains("/")) finalMFG = manufacturingDate

                if(expiryDate!=null && !finalEXP.contains("/") && expiryDate.contains("/")) finalEXP = expiryDate

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
                            "MFG date is: ${finalMFG}\nEXP date is: ${finalEXP}\n\n" +
                            "Final prod $finalProduct\n"+
                            "Final MRP $finalMRP\n" +
                            "Prod max score:$maxScore\n"+
                            "MRP max score: $maxMRPScore"

                recognizedTextView.text = finalResult

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

    fun extractMrp(text: Text):  ArrayList<Any>    {

        val recognizedText = text.text
        data class SizeMRP(val size: Double, val element: String)
        val sizesMrp = mutableListOf<SizeMRP>()

        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val elementText = element.text
                    if (elementText.matches(Regex("\\d+(\\.\\d+)?"))) {
                        var sizeM = 0.0
                        val cornersM = element.cornerPoints
                        if (cornersM != null && cornersM.size == 4) {
                            val dx1 = (cornersM[0].x - cornersM[3].x).toDouble()
                            val dy1 = (cornersM[0].y - cornersM[3].y).toDouble()
                            val len1M = sqrt(dx1 * dx1 + dy1 * dy1)
                            sizeM = len1M
                        }
                        sizesMrp.add(SizeMRP(sizeM, element.text))
                    }
                }
            }
        }

        val top3MRP: List<String> =
            sizesMrp.sortedByDescending { it.size }.take(5).map { it.element }

        val wordsArray = recognizedText.split("[\\s:;.]".toRegex()).filter { it.isNotEmpty() }.toTypedArray()
        if(wordsArray.isEmpty()){
//            return arrayListOf("Not found",0.0,"Not found",0.0,listOf(0.0),"Not found",listOf("Not found"),"Not found")
            return arrayListOf("Not found", 0.0, listOf(0.0), listOf("Not found"))

        }

        val recognizedTextLines = recognizedText.split("\n").toTypedArray()
        var mscore = 0.0
        val mscoreArr = mutableListOf<Double>()

        val blockOfMrp = extractDateMrpBlock(text).toString()
        val blockArray = blockOfMrp.split("[\\s:;.]".toRegex()).filter { it.isNotEmpty() }.toTypedArray()
        //3rd condition MRP function
        var mrpLine = "noneNull"
        for (line in recognizedTextLines) {
            if (line.contains(Regex("""\b(?:Rs|MRP|mrp|₹|MR|MRR|MPP|MPR|M.R.P|Rs.|)\b""",RegexOption.IGNORE_CASE))) {
                mrpLine = line
                break
            }
        }
        val mrpLineArray = mrpLine.split("[\\s:;.]".toRegex()).filter { it.isNotEmpty() }.toTypedArray()

        for (i in wordsArray.indices) {
            if(wordsArray[i].toDoubleOrNull()!=null) {
                val num = wordsArray[i].toDouble()
                mscore += 0.05
                if (num in 2.0..5000.0) {
                    mscore += 0.5
                }
                if(num==2.0){
                    mscore+=0.3
                }
                //2nd condition
                if ((num!= 9.0) && (num % 5 == 0.0 || num % 10 == 0.0 || (num - 99) % 100 == 0.0 || num % 100 == 0.0  || (num - 9) % 10 == 0.0 )) {
                    mscore += 0.3
                }
                if((num==400.00)&&wordsArray[i+1].toDoubleOrNull()!=null){
                    mscore -= 0.5
                }
                //3rd condition
                if( i+1<wordsArray.size-1 && wordsArray[i+1].contains(Regex("""\b(g|Kg|ml|mg|l|per|pe|n|9)\b""",RegexOption.IGNORE_CASE))){
                    mscore -= 0.3
                }
                if (mrpLineArray.contains(wordsArray[i])) {
                    //showToast("Line")
                    mscore += 0.4
                }
                if (2020 < num && num < 2030) {
                    mscore -= 0.1
                }
                //4th condition
                if (wordsArray[i] in blockArray) {
                    //showToast("block")
                    mscore += 0.3
                }
//                if (i<(len*(2/3)) && i > (len*(1/3))) {
//                    mscore += 0.1
//                }

            }
            //Might need changes
            if(wordsArray[i].contains("/-")){
                if(i>1 && wordsArray[i-1].toDoubleOrNull()!=null){
                    mscoreArr[i-1] += 0.6
                }
                else {
                    mscore += 1.3
                }
            }
            mscoreArr.add(mscore)
            mscore=0.0
        }

        var mrpadder = 0.4
        for (i in top3MRP.indices) {
            for (j in wordsArray.indices) {
                if(wordsArray[j].toDoubleOrNull()!=null) {
                    if (top3MRP[i].toDouble() == wordsArray[j].toDouble()) {
                        mscoreArr[j] += mrpadder
                        mrpadder -= 0.1
                    }
                }
            }
        }

        val j1 = mscoreArr.indexOf(mscoreArr.maxOrNull())
        val m1 = wordsArray[j1]
        val m1Score = mscoreArr[j1]

        return arrayListOf(m1, m1Score, mscoreArr, top3MRP)
    }
    fun extractProduct(text: Text): ArrayList<Any> {
        val recognizedText = text.text
        data class ElementSize(val size: Double, val element: String)

        val elementSizes = mutableListOf<ElementSize>()

        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    var size = 0.0
                    val corners = element.cornerPoints
                    //val elementText = element.text
                    if (corners != null && corners.size == 4) {
                        val dx1 = (corners[0].x - corners[3].x).toDouble()
                        val dy1 = (corners[0].y - corners[3].y).toDouble()
                        val len1 = sqrt(dx1 * dx1 + dy1 * dy1)
                        size = len1
                    }
                    elementSizes.add(ElementSize(size, element.text))
                }
            }
        }
        val top5Elements: List<String> =
            elementSizes.sortedByDescending { it.size }.take(3).map { it.element }

        //Score calculation
        //val wordsArray = recognizedText.split("(\\s+|:|;|.)".toRegex()).toTypedArray()

        val wordsArray = recognizedText.split("[\\s:;.]".toRegex()).filter { it.isNotEmpty() }.toTypedArray()
        if(wordsArray.isEmpty()){
            return arrayListOf("Not found",0.0,"Not found")
        }

        //val recognizedTextLines = recognizedText.split("\n").toTypedArray()

        var score = 0.0

        //var j = 1.0
        val len = wordsArray.size.toDouble()
        val scoreArr = mutableListOf<Double>()

        //4th condition
        val specialCharPattern = Pattern.compile("[^a-zA-Z0-9]")
        val moreThanThreeDigitsPattern = Pattern.compile("\\d{4,}")
        val alphabetOnlyPattern = Regex("^[a-zA-Z]+$")

        for (i in wordsArray.indices) {
            if (wordsArray[i].length  >= 3) {
                //Higher uppercase socre for local products
                if (wordsArray[i].uppercase()==wordsArray[i] && wordsArray[i].toDoubleOrNull()==null) {
                    score += 0.2
                }
                else {
                    //Higher capitalizaton score for famous and sophisticated products
                    if (wordsArray[i].capitalize(Locale.ROOT) == wordsArray[i] && wordsArray[i].toDoubleOrNull() == null) {
                        score += 0.16
                    }
                    else{
                        score+=0.1
                    }
                }
                //Lower for causal products(generally rare)
                if(len>20){
                    score -= 0.3
                }
                if (alphabetOnlyPattern.matches(wordsArray[i])) {
                    score += 0.23
                }
                if (specialCharPattern.matcher(wordsArray[i]).find()) {
                    score -= 0.4
                }
                if (moreThanThreeDigitsPattern.matcher(wordsArray[i]).find()) {
                    score -= 0.4
                }
                if (i>0 && wordsArray[i-1].contains(Regex("""\b(item|product|tem|roduct|ite|produc|roduc)\b""",RegexOption.IGNORE_CASE))) {
                    score += 1
                }
            }

            scoreArr.add(score)
            score = 0.0
        }
        var adder = 0.5
        for (i in top5Elements.indices) {
            for (j in wordsArray.indices) {
                if (top5Elements[i] == wordsArray[j])
                {
                    scoreArr[j] += adder
                    adder -= 0.1
                }
            }
        }


        //Setting up max scorers
        val i1 = scoreArr.indexOf(scoreArr.maxOrNull())
        val max1 = wordsArray[i1]
        var max1Score = scoreArr[i1]
        //scoreArr[i1] = 0.0
        //val i2 = scoreArr.indexOf(scoreArr.maxOrNull())
        //val max2 = wordsArray[i2]

        var finalProd = max1
//        var flag = 0
        for (block in text.textBlocks) {
//            if(flag==1){
//                if(block.text.toString().contains(max2)){
//                    finalProd = max1 + block.text
//                }
//                break
//        }
            for (line in block.lines) {
                for (element in line.elements) {
                    if (max1 == element.text) {
                        finalProd = block.text
//                        flag = 1
                    }
                }
            }
        }
        val wordsArrayReturn = wordsArray.joinToString(prefix = "[", postfix = "]", separator = ", ")


//Final Prod Return conditions
        val finaProdArray = finalProd.split("\\s".toRegex()).filter { it.isNotEmpty() }.toTypedArray()
        if (specialCharPattern.matcher(finalProd).find() && finaProdArray.size>=3) {
            finalProd = "Not found"
            max1Score = 0.0
        }

        return arrayListOf(finalProd, max1Score, wordsArrayReturn)

    }

    private fun extractDateMrpBlock(text: Text): ArrayList<Any>  {
        val resBlock = ArrayList<Any>()
        val dateRegex = """\b\d{2}\s*[/.\s]\s*\d{2}\s*[/.\s]\s*(?:\d{2}|\d{4})\b|\b\d{2}\s*[A-Z]{3}\s*\d{2}\b""".toRegex()

        for (block in text.textBlocks) {
            outer@ for (line in block.lines) {
                for (element in line.elements) {
                    val s = element.text

                    if (dateRegex.containsMatchIn(s) ||
                        s.contains("Rs.", ignoreCase = true) ||
                        s.contains("MRP", ignoreCase = true) ||
                        s.contains("₹") ||
                        //  s.contains("/") ||
                        s.contains("M.R.P", ignoreCase = true) ||
                        s.contains("Rs", ignoreCase = true)) {

                        resBlock.add(block.text)
                        break@outer
                    }
                }
            }
        }
        Log.i(ContentValues.TAG, resBlock.toString())
        return resBlock
    }

    fun extractDates(text: String): Pair<String?, String?> {
        val potentialDates = mutableListOf<String>()

        val dateRegex = Regex("""(?ix)
\s*
(?:
  (\d{1,2})[/.](\d{1,2})[/.](\d{4}) |  # Format: DD/MM/YYYY, DD.MM.YYYY
  (\d{1,2})[/.](\d{1,2})[/.](\d{2}) |  # Format: DD/MM/YY, DD.MM.YY
  (\d{4})[/.](\d{1,2})[/.](\d{1,2}) |  # Format: YYYY/MM/DD, YYYY.MM.DD
  
  (\d{1,2})/(\d{4}) |  # Format: MM/YYYY
  (\d{1,2})/(\d{2}) |  # Format: MM/YY

  
  (\d{1,2})-(\d{1,2})-(\d{4}) |  # Format: DD-MM-YYYY
  (\d{1,2})-(\d{1,2})-(\d{2}) |  # Format: DD-MM-YY
  (\d{4})-(\d{1,2})-(\d{1,2}) |
  
  \d{2}\s*[/.\s]\s*\d{2}\s*[/.\s]\s*(?:\d{2}|\d{4})|\d{2}\s*[A-Z]{3}\s*\d{2} |
  
  # Formats with Month Names    
  (\d{1,2})\s*([Jj]an|[Ff]eb|[Mm]ar|[Aa]pr|[Mm]ay|[Jj]un|[Jj]ul|[Aa]ug|[Ss]ep|[Oo]ct|[Nn]ov|[Dd]ec)(\d{4}) |  # DD MMM YYYY
  ([Jj]an|[Ff]eb|[Mm]ar|[Aa]pr|[Mm]ay|[Jj]un|[Jj]ul|[Aa]ug|[Ss]ep|[Oo]ct|[Nn]ov|[Dd]ec)(\d{4}) | #MMM YYYY
  ([Jj]an|[Ff]eb|[Mm]ar|[Aa]pr|[Mm]ay|[Jj]un|[Jj]ul|[Aa]ug|[Ss]ep|[Oo]ct|[Nn]ov|[Dd]ec)(\d{2}) | #MMM YY
)
\s*
""".trimMargin())

        // Find all potential dates using the regex
        dateRegex.findAll(text).forEach { match ->
            potentialDates.add(match.value)
            Log.i(ContentValues.TAG, match.value)
        }

        // If no dates are found, return null for both
        if (potentialDates.isEmpty()) {
            return null to null
        }

        // Helper function to convert date strings to Date objects for comparison
        fun parseDate(dateStr: String): Date? {
            val formats = listOf(
                SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
                SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH),
                SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH),
                SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
                SimpleDateFormat("MM-dd-yyyy", Locale.ENGLISH),
                SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
                SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH),
                SimpleDateFormat("MM.dd.yyyy", Locale.ENGLISH),
                SimpleDateFormat("yyyy.MM.dd", Locale.ENGLISH),
                SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
                SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH),
                SimpleDateFormat("yyyy MMM dd", Locale.ENGLISH),
                SimpleDateFormat("dd/MM/yy", Locale.ENGLISH),
                SimpleDateFormat("MM/dd/yy", Locale.ENGLISH),
                SimpleDateFormat("yy/MM/dd", Locale.ENGLISH),
                SimpleDateFormat("dd-MM-yy", Locale.ENGLISH),
                SimpleDateFormat("MM-dd-yy", Locale.ENGLISH),
                SimpleDateFormat("yy-MM-dd", Locale.ENGLISH),
                SimpleDateFormat("dd.MM.yy", Locale.ENGLISH),
                SimpleDateFormat("MM.dd.yy", Locale.ENGLISH),
                SimpleDateFormat("MM/yyyy", Locale.ENGLISH),
                SimpleDateFormat("MMM yyyy", Locale.ENGLISH),
                SimpleDateFormat("MMM yy", Locale.ENGLISH),
                SimpleDateFormat("MM/yy", Locale.ENGLISH),
//                SimpleDateFormat("MM-yy", Locale.ENGLISH),
//                SimpleDateFormat("MM-yyyy", Locale.ENGLISH),
            )

            for (format in formats) {
                try {
                    return format.parse(dateStr)
                } catch (e: Exception) {
                    // Continue to the next format
                }
            }
            return null
        }



        // Sort the potential dates by their parsed Date objects
        val sortedDates = potentialDates.mapNotNull { dateStr -> parseDate(dateStr)?.let { dateStr to it } }
            .sortedBy { it.second }
            .map { it.first }

        // Assuming the first sorted date is manufacturing and the second is expiry (adjust logic if needed)
        val manufacturingDate = sortedDates.firstOrNull()
        var expiryDate = sortedDates.getOrNull(1)

        val expiry = Regex("""(?ix)(Best Before| Use Before)""")

        val lines = text.split("\n").toTypedArray()

        for (line in lines){
            expiryDate = if(line.contains(expiry))
                line
            else if(manufacturingDate != expiryDate && manufacturingDate!=null && expiryDate!=null)
                sortedDates.getOrNull(1)
            else
                null
        }

        return manufacturingDate to expiryDate
        //return finalMFG to finalEXP
    }

    private fun getErrorMessage(exception: Exception): String? {
        val mlKitException = exception as? MlKitException ?: return exception.message
        return if (mlKitException.errorCode == MlKitException.UNAVAILABLE) {
            "Waiting for text recognition model to be downloaded"
        } else exception.message
    }
}