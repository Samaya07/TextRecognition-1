package com.example.textrecognition4

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.sqrt


class MainActivity : AppCompatActivity() {

    private lateinit var inputVideoBtn:MaterialButton
    private lateinit var recognizeTextBtn:MaterialButton
    private lateinit var videoV: VideoView
    private lateinit var recognizedTextEt : EditText

    private companion object {

        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101
    }

    //uri of the image that we will take from camera/gallery
    //private var imageUri: Uri? = null
    private var videoUri: Uri? = null

    private lateinit var cameraPermissions: Array<String>
    private lateinit var storagePermissions: Array<String>

    private lateinit var  progressDialog: ProgressDialog

    private lateinit var textRecognizer: TextRecognizer
    private var arrayOfProds = arrayListOf<ArrayList<Any>>()
    private var arrayBitmaps = arrayListOf<Bitmap>()
    private var frameIndex = 0
    private var maxScore = 0.0
    private var resBlock = arrayListOf<String>()
    private lateinit var finalResult: String
    private lateinit var finalProduct: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //init UI views
        inputVideoBtn = findViewById(R.id.inputVideoBtn)
        recognizeTextBtn = findViewById(R.id.recognizeTextBtn)
        videoV = findViewById(R.id.videoV)
        recognizedTextEt = findViewById(R.id.recognizedTextEt)


        //init arrays of permissions required for camera,gallery
        cameraPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_VIDEO)
        storagePermissions = arrayOf(Manifest.permission.READ_MEDIA_VIDEO)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        //handle click, show input image dialog
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        //handle click, show input image dialog

        inputVideoBtn.setOnClickListener {
            showInputImageDialog()
        }

        recognizeTextBtn.setOnClickListener {

            /*if(imageUri == null){
                showToast("Pick Image first")
            }*/
            if(videoUri == null){
                showToast("Pick Video first")
            }
            else{
                //recognizeTextFromImage()

                arrayOfProds.clear()
                arrayBitmaps.clear()
                resBlock.clear()
                frameIndex = 0
                maxScore = 0.0
                finalResult = ""
                finalProduct = ""
                progressDialog.setMessage("Recognizing text")
                progressDialog.show()
                arrayBitmaps = getVideoFrame(context = baseContext)

            }
        }
    }
//Product Function
    private fun extractProduct(text: Text): ArrayList<Any> {
        val recognizedText = text.text

        data class ElementSize(val size: Double, val element: String)

        val elementSizes = mutableListOf<ElementSize>()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    var size = 0.0
                    val corners = element.cornerPoints
                    if (corners != null && corners.size == 4) {
                        val dx1 = (corners[0].x - corners[3].x).toDouble()
                        val dy1 = (corners[0].y - corners[3].y).toDouble()
                        val len1 = sqrt(dx1 * dx1 + dy1 * dy1)
                        val dx2 = (corners[2].x - corners[3].x).toDouble()
                        val dy2 = (corners[2].y - corners[3].y).toDouble()
                        val len2 = sqrt(dx2 * dx2 + dy2 * dy2)
                        size = (len1 + len2) * 2
                    }
                    elementSizes.add(ElementSize(size, element.text))
                }
            }
        }
        val top5Elements: List<String> =
            elementSizes.sortedByDescending { it.size }.take(3).map { it.element }

        //Score calculation
        val wordsArray = recognizedText.split("\\s+".toRegex()).toTypedArray()
        val recognizedTextLines = recognizedText.split("\n").toTypedArray()

        var score = 0.0
        var mscore = 0.0
        //var j = 1.0
        val len = wordsArray.size
        val scoreArr = mutableListOf<Double>()
        val mscoreArr = mutableListOf<Double>()
    //4th condition
        val blockOfMrp = extractDateMrpBlock(text).toString()
        val blockArray = blockOfMrp.split("(\\s+|:|;)".toRegex()).toTypedArray()
    //3rd condition MRP function
        var mrpValue = "nonenull"
        for (line in recognizedTextLines) {
            if (line.contains(Regex("""\b(?:Rs|MRP|mrp|₹|MR|MRR|MPP|MPR|M.R.P|)\b""",RegexOption.IGNORE_CASE))) {
                extractMrpValue(line)?.let {
                    mrpValue = it
                }
            }
        }


        val p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE)
        for (i in wordsArray.indices) {
            if (wordsArray[i].length > 3) {
                val m = p.matcher(wordsArray[i])
                while (m.find()) {
                    score = -0.7
                }
                if (wordsArray[i].uppercase() == wordsArray[i]) {
                    score += 0.4
                }
                if (wordsArray[i].capitalize() == wordsArray[i]) {
                    score += 0.3
                }
                if (i < (len / 3)) {
                    score += 0.2
                } else {
                    if (i < (len * 2 / 3)) {
                        score += 0.25
                    }
                }
            }

            if(wordsArray[i].toDoubleOrNull()!=null) {

                val num = wordsArray[i].toDouble()
                //1st condition
                if (2.0 < num && num < 10000.0) {
                    mscore += 0.35
                }
                //2nd condition
                if (num % 5 == 0.0 || num % 10 == 0.0 || (num - 99) % 100 == 0.0 || (num - 9) % 10 == 0.0 || num % 100 == 0.0) {
                    mscore += 0.5
                }
                //3rd condition
                if (mrpValue == wordsArray[i]) {
                    mscore += 1
                }
                if (2020 < num && num < 2030) {
                    mscore -= 0.3
                }
                //4th condition
                if (wordsArray[i] in blockArray) {
                    mscore += 0.7
                }
            }



            mscoreArr.add(mscore)
            scoreArr.add(score)
            mscore=0.0
            score = 0.0
        }
        var adder = 0.6
        for (i in top5Elements.indices) {
            for (j in wordsArray.indices) {
                if (top5Elements[i] == wordsArray[j]) {
                    scoreArr[j] += adder
                    adder -= 0.1
                }
            }
        }

        //Setting up max scorers
        val i1 = scoreArr.indexOf(scoreArr.maxOrNull())
        val max1 = wordsArray[i1]
        val max1Score = scoreArr[i1]
        scoreArr[i1] = 0.0
        val i2 = scoreArr.indexOf(scoreArr.maxOrNull())
        val max2 = wordsArray[i2]
        val max2Score = scoreArr[i2]
        scoreArr[i2] = 0.0
        val i3 = scoreArr.indexOf(scoreArr.maxOrNull())
        val max3 = wordsArray[i3]

        var checker = ""
        var flagMax = 0
        var finalProd = ""
        var finalScore = 0.0
        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    if (max1 == element.text && flagMax == 0) {
                        finalProd = block.text
                        finalScore = max1Score
                    }
                    if (max2 == element.text) {
                        checker = block.text
                    }
                    if (checker == block.text && max3 == element.text) {
                        finalProd = block.text
                        finalScore = max2Score
                        flagMax = 1
                    }
                }
            }
        }
        return arrayListOf(finalProd, finalScore, m1, m1Score, mscoreArr, wordsArray, mrpValue)
    }
//MRP Function
private fun extractMrpValue(line: String): String? {
    val mrpPattern = """""${'"'}(?i)\b(?:Rs|MRP|mrp|MR|MRR|MPP|MPR|M.R.P)\s*[:.]?\s*(\d+(?:\.\d+)?)(?:\s*(₹?))?""${'"'}""".toRegex(RegexOption.IGNORE_CASE)
    val matchResult = mrpPattern.find(line)
    return matchResult?.groupValues?.get(1)?.trim()
}

//Date Function
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
    Log.i(TAG, resBlock.toString())
    return resBlock
}


    private fun recognizeTextFromImage(bitmap: Bitmap) {


        try{
            val inputImage = InputImage.fromBitmap(bitmap, 0)


            //start text recognition process from image
            textRecognizer.process(inputImage)
                .addOnSuccessListener { text ->

                    //Redundant Declaration
                    //val recognizedText = text.toString()
                    //val recognizedTextLines = recognizedText.split("\n").toTypedArray()
                    //val wordsArray = recognizedText.split("\\s+".toRegex()).toTypedArray()
                    //val wordsString = wordsArray.joinToString(prefix = "[", postfix = "]", separator = ", ")

                    //Date detection
                    //val dates = extractDates(wordsString)
                    //MRP detection
                    //val mrpValue = extractMrpValue(text.toString())
                    //Product Detection
                    //Recognised text for debugging
                    /*val recognizedText = text.text
                    val recText = ArrayList<Any>()
                    recText.add(recognizedText)
                    arrayOfProds.add(recText)*/
                   //MRP and Date Detection code
                    val adderBlock = extractDateMrpBlock(text).toString()
                    if(adderBlock!="[]") {
                        resBlock += adderBlock + "\n"
                    }
                    val result = extractProduct(text)
                    arrayOfProds.add(result)
                    //Picking max score product
                    val scorer = result[1].toString()
                    val intScorer = scorer.toDouble()
                    if(intScorer>maxScore){
                        maxScore = intScorer
                        finalProduct = result[0].toString()
                    }
                    //Printing final
                    frameIndex += 1
                    if(frameIndex == arrayBitmaps.size)
                    {
                        finalResult =
                            "ProductArray\n$arrayOfProds\n\n\nPrices Blocks:$resBlock\n\n\nProduct: $finalProduct\n\n\nPrice:\n"+ resBlock[0]
                        recognizedTextEt.setText(finalResult)
                        progressDialog.dismiss()

                    }

                }
                .addOnFailureListener { e ->
                    //failed recognizing text from image, dismiss dialog, show reason in Toast
                    progressDialog.dismiss()
                    showToast("Failed to recognize text due to ${e.message}")
                }



        }
        catch(e:Exception){
            //Exception occurred while preparing InputImage, dismiss dialog, show reason in Toast
            progressDialog.dismiss()
            showToast("Failed to prepare image due to ${e.message}")
        }

    }

    private fun getVideoFrame(context: Context): ArrayList<Bitmap> {

        //var bitmap: Bitmap? = null
        val rev = ArrayList<Bitmap>()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)

            //Create a new Media Player
            val mp: MediaPlayer = MediaPlayer.create(baseContext, videoUri)

            val millis = mp.duration

            Log.i(TAG,"millis $millis")
            val fps = 5

            var i = 1000000/fps
            while (i < millis*1000) {
                val bitmap = retriever.getFrameAtTime(i.toLong(), OPTION_CLOSEST_SYNC)
                rev.add(bitmap!!)
                recognizeTextFromImage(bitmap)

                i += 1000000/fps
            }

        } catch (ex: RuntimeException) {
            ex.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (ex: RuntimeException) {
                ex.printStackTrace()
            }
        }

        return rev

    }


    private fun showInputImageDialog() {

        //init PopupMenu param 1 is context, param 2 is UI View where you want to show PopupMenu
        val popupMenu = PopupMenu(this, inputVideoBtn)

        //Add items Camera, Gallery to PopupMenu, param 2 is menu id, param 3 is position of this menu item in menu items list, param 4 is title of the menu

        popupMenu.menu.add(Menu.NONE, 1, 1, "CAMERA")
        popupMenu.menu.add(Menu.NONE, 2, 2,"Gallery")

        //Show PopupMenu
        popupMenu.show()

        //handle PopupMenu item clicks
        popupMenu.setOnMenuItemClickListener {menuItem->
            //get item id that is clicked from PopupMenu
            val id = menuItem.itemId
            if(id == 1){
                //Camera is clicked, check if camera permissions are granted or not
                if(checkCameraPermissions()){
                    pickImageCamera()
                }
                else{
                    requestCameraPermissions()
                }
            }
            else if(id == 2){
                //Gallery is clicked, check if storage permissions are granted or not
                if(checkStoragePermission()){

                    pickImageGallery()
                }
                else{
                    requestStoragePermission()
                }

            }

            return@setOnMenuItemClickListener true

        }
    }

    private fun pickImageGallery()
    {
        //intent to pick image from gallery. will show all resources from where we can pick an image
        val intent = Intent(Intent.ACTION_PICK)

        //set type of file we want to pick i.e. image
        intent.type = "video/*"
        galleryActivityResultLauncher.launch(intent)

    }

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {result ->
            if(result.resultCode == Activity.RESULT_OK)
            {
                val data = result.data
                //imageUri = data!!.data
                videoUri = data!!.data
                recognizedTextEt.text = null

               // imageIv.setImageURI(imageUri)
                videoV.setVideoURI((videoUri))
                videoV.start()
            }
            else
            {
                showToast("Cancelled!")

            }
        }
    private fun pickImageCamera(){
        val values = ContentValues()
        //values.put(MediaStore.Images.Media.TITLE, "Sample Title")
        //values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description")
        values.put(MediaStore.Video.Media.TITLE, "Sample Title")
        values.put(MediaStore.Video.Media.DESCRIPTION, "Sample Description")

        //imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)
        videoUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)

        //val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        //intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri)

        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
            //here we will receive the image, if taken from camera
            if(result.resultCode == Activity.RESULT_OK){

                recognizedTextEt.text = null
                videoV.setVideoURI(videoUri)
                videoV.start()
            }
            else{
                //cancelled
                showToast("Cancelled!")
            }
        }

    private fun checkStoragePermission(): Boolean{

        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        //return true
    }

    private fun checkCameraPermissions() : Boolean{

        val cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED

        return cameraResult && storageResult
        //return true
    }

    private fun requestStoragePermission(){
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE)
    }

    private fun requestCameraPermissions(){

        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //handle permission(s) results
        when(requestCode){
            CAMERA_REQUEST_CODE ->{

                if(grantResults.isNotEmpty()){
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED

                    if(cameraAccepted && storageAccepted){

                        pickImageCamera()
                    }
                    else{
                        showToast("Camera & Storage permissions are required")
                    }
                }
            }
            STORAGE_REQUEST_CODE->{

                if(grantResults.isNotEmpty()){

                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED

                    if(storageAccepted){
                        pickImageGallery()
                    }
                    else{
                        showToast("Storage permission is required")
                    }
                }
            }
        }
    }


    private fun showToast(message: String)
    {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}