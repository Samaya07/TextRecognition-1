package com.example.textrecognition4

//import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.sqrt


class MainActivity : AppCompatActivity() {

    private lateinit var inputImageBtn:MaterialButton
    private lateinit var recognizeTextBtn:MaterialButton
    private lateinit var imageIv:ImageView
    private lateinit var recognizedTextEt : EditText




    private companion object {

        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101
    }

    //uri of the image that we will take from camera/gallery
    private var imageUri: Uri? = null

    private lateinit var cameraPermissions: Array<String>
    private lateinit var storagePermissions: Array<String>

    private lateinit var  progressDialog: ProgressDialog

    private lateinit var textRecognizer: TextRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //init UI views
        inputImageBtn = findViewById(R.id.inputImageBtn)
        recognizeTextBtn = findViewById(R.id.recognizeTextBtn)
        imageIv = findViewById(R.id.imageIv)
        recognizedTextEt = findViewById(R.id.recognizedTextEt)

        //init arrays of permissions required for camera,gallery
        cameraPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        storagePermissions = arrayOf(Manifest.permission.READ_MEDIA_IMAGES)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        //handle click, show input image dialog
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        //textRecognizer = TextRecognition.getClient((DevanagariTextRecognizerOptions.Builder().build()))



        //handle click, show input image dialog

        inputImageBtn.setOnClickListener {
            showInputImageDialog()
        }

        recognizeTextBtn.setOnClickListener {

            if(imageUri == null){
                showToast("Pick Image first")
            }
            else{
                recognizeTextFromImage()
            }
        }

    }

    private fun recognizeTextFromImage() {

        //set message and show progress dialog
        progressDialog.setMessage("Preparing image")
        progressDialog.show()

        try{
            //Prepare InputImage from image uri
            val inputImage = InputImage.fromFilePath(this, imageUri!!)
            //image prepared, we are about to start text recognition process, change progress message
            progressDialog.setMessage("Recognizing text")

            //start text recognition process from image
            val textTaskResult = textRecognizer.process(inputImage)
                .addOnSuccessListener {text ->
                    //process completed, dismiss dialog
                    progressDialog.dismiss()
                    //get the recognized text
                    val recognizedText = text.text
                    var finalEle = " "
                    var maxSize : Double = 0.0

                    data class ElementSize(val size: Double, val element: String)

                    val ElementSizes = mutableListOf<ElementSize>()

                    for (block in text.textBlocks) {
                        for (line in block.lines) {
                            for (element in line.elements) {
                                var size: Double = 0.0
                                val corners = element.cornerPoints
                                if (corners != null && corners.size == 4) {
                                    val dx1 = (corners[0].x-corners[3].x).toDouble()
                                    val dy1 = (corners[0].y-corners[3].y).toDouble()
                                    val len1 = sqrt(dx1 * dx1 + dy1 * dy1)
                                    val dx2 = (corners[2].x-corners[3].x).toDouble()
                                    val dy2 = (corners[2].y-corners[3].y).toDouble()
                                    val len2 = sqrt(dx2 * dx2 + dy2 * dy2)
                                    size = (len1+len2)*2
                                }
                                ElementSizes.add(ElementSize(size, element.text))
                            }
                        }
                    }
                    val top5Elements:List<String> = ElementSizes.sortedByDescending { it.size }.take(3).map { it.element }

                    //Score calculation
                    val wordsArray = recognizedText.split("\\s+".toRegex()).toTypedArray()
                    var score = 0.0
                    var j =1.0
                    val len = wordsArray.size
                    val scoreArr = mutableListOf<Double>()
                    val p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE)
                    for(i in wordsArray.indices){
                        if(wordsArray[i].length > 3) {
                            val m = p.matcher(wordsArray[i])
                            while(m.find()){
                                score = -0.7
                            }
                            if(wordsArray[i].uppercase() == wordsArray[i]){
                                score +=0.4
                            }
                            if (wordsArray[i].capitalize() == wordsArray[i]) {
                                score += 0.3
                            }
                            if (i<(len/3)) {
                                score += 0.2
                            }
                            else{
                                if(i<(len*2/3)){
                                    score+=0.25
                                }
                            }
                        }
                        scoreArr.add(score)
                        score = 0.0
                    }
                    var adder = 0.6
                    for(i in top5Elements.indices){
                        for(j in wordsArray.indices){
                            if(top5Elements[i] == wordsArray[j]){
                                scoreArr[j] += adder
                                adder -= 0.1
                            }
                        }
                    }

                    //Setting up max scorers
                    val i1 = scoreArr.indexOf(scoreArr.maxOrNull())
                    val max1 = wordsArray[i1]
                    scoreArr[i1] = 0.0
                    val i2 = scoreArr.indexOf(scoreArr.maxOrNull())
                    val max2 = wordsArray[i2]
                    scoreArr[i2] = 0.0
                    val i3 = scoreArr.indexOf(scoreArr.maxOrNull())
                    val max3 = wordsArray[i3]

                    //Taking the block for product
                    var checker = ""
                    var flagMax = 0
                    var finalProd = ""
                    for (block in text.textBlocks) {
                        for (line in block.lines) {
                            for (element in line.elements) {
                                if (max1 == element.text && flagMax==0) {
                                    finalProd = block.text
                                }
                                if (max2 == element.text) {
                                    checker = block.text
                                }
                                if(checker==block.text && max3==element.text){
                                    finalProd = block.text
                                    flagMax = 1
                                }
                            }
                        }
                    }

                    //Setting up all the arrays
                    val scoredString = scoreArr.joinToString(prefix = "[", postfix = "]", separator = ", ")
                    val wordsString = wordsArray.joinToString(prefix = "[", postfix = "]", separator = ", ")
                    val top5elementsInString = top5Elements.joinToString(prefix = "[", postfix = "]", separator = ", ")
                    val recognizedTextLines = recognizedText.split("\n").toTypedArray()

                    //MRP recognition
                    var mrpValue = "not found"
                    val strl = recognizedText.split("\n").toTypedArray()
//                    for(x in strl) {
//                        if (x.contains("Rs") || x.contains("MRP") || x.contains("mrp") || x.contains("₹")) {
//                            mrpValue = x;
//                        }
//                        }

                    //MRP recognition
                    var completeCheck = 1
                    for (line in recognizedTextLines) {
                        if (line.contains(Regex("""\b(?:Rs|MRP|mrp|₹|MR|MRR|MPP|MPR|M.R.P|)\b""",RegexOption.IGNORE_CASE))) {
                            extractMrpValue(line)?.let {
                                mrpValue = it+" met1"
                                completeCheck = 0
                            }
                        }
                    }
                    if(completeCheck == 1) {
                        val regexDigit = "-?[0-9]+(\\.[0-9]+)?".toRegex()
                        for (x in wordsArray.indices) {
                            if (wordsArray[x].matches(regexDigit)) {
                                if (x.toDouble() in 5.0..10000.0) {
                                    showToast("hi")
                                    //mrpValue = mrpValue + " " + wordsArray[x].toString()
                                    mrpValue = wordsArray[x].toString()
                                }
                            }
                        }
                    }

                    //Date detection
                    val dates = extractDates(wordsString)

                    //Final Printing
                    finalEle = "recognisedText is"+ "\n" +wordsString +"\n\n"+
                            "Scored Array is"+"\n"+scoredString+"\n\n"+
                            "Max elements are"+"\n"+max1+ "  " +max2 + "  "+ max3 + "\n\n"+
                            "Product is:"+finalProd+"\n\n"+
                            "Top 5 elements in size"+top5elementsInString + "\n\n"+
                            "MRP: ₹" + mrpValue + "\n\n"+
                            "Manufacturing date: " + dates.first + "\n\n"+
                            "Expiry date: " + dates.second

                    recognizedTextEt.setText(finalEle)

                }
                .addOnFailureListener { e->
                    //failed recognizing text from image, dismiss dialog, show reason in Toast
                    progressDialog.dismiss()
                    showToast("Failed to recognize text due to ${e.message}")
                }

        }
        catch(e:Exception){
            //Exception occured while preparing InputImage, dismiss dialog, show reason in Toast
            progressDialog.dismiss()
            showToast("Failed to prepare image due to ${e.message}")
        }
    }

    fun extractMrpValue(line: String): String? {
        val mrpPattern = """\b(?:Rs|MRP|mrp|MR|MRR|MPP|MPR|M.R.P)\s*[:.]\s*₹?\s*(\d+(?:\.\d+)?)""".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = mrpPattern.find(line)
        return matchResult?.groupValues?.get(1)?.trim()
    }


    private fun extractDates(text: String): Pair<String?, String?> {
        val potentialDates = mutableListOf<String>()

        // Regex for DD/MM/YYYY, DD/MM/YY, DDMMMyy, DD.MM.YYYY, and DD.MM.YY formats
        //val dateRegex = """\b\d{2}\s*[-/.\s]\s*\d{2}\s*[-/.\s]\s*(?:\d{2}|\d{4})\b|\b\d{2}\s*[A-Z]{3,}\s*\d{2,4}\b""".toRegex()
        val dateRegex = """\b\d{2}\s*[/.\s]\s*\d{2}\s*[/.\s]\s*(?:\d{2}|\d{4})\b|\b\d{2}\s*[A-Z]{3}\s*\d{2}\b""".toRegex()


        // Find all potential dates using the regex
        dateRegex.findAll(text).forEach { match ->
            potentialDates.add(match.value)
            Log.i(TAG, match.value)
        }

        // If no dates are found, return null for both
        if (potentialDates.isEmpty()) {
            return null to null
        }

        // Helper function to convert date strings to Date objects for comparison
        fun parseDate(dateStr: String): Date? {
            val formats = listOf(
                SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
                SimpleDateFormat("dd/MM/yy", Locale.ENGLISH),
                SimpleDateFormat("ddMMMyy", Locale.ENGLISH),
                SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH),
                SimpleDateFormat("dd.MM.yy", Locale.ENGLISH),
                SimpleDateFormat("dd MM yyyy", Locale.ENGLISH),  // Added spaces version
                SimpleDateFormat("dd MM yy", Locale.ENGLISH),     // Added spaces version
                SimpleDateFormat("dd MMM yy", Locale.ENGLISH)
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
        val expiryDate = sortedDates.getOrNull(1)

        return manufacturingDate to expiryDate
    }

    private fun showInputImageDialog() {

        //init PopupMenu param 1 is context, param 2 is UI View where you want to show PopupMenu
        val popupMenu = PopupMenu(this, inputImageBtn)

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
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)

    }

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {result ->
            if(result.resultCode == Activity.RESULT_OK)
            {
                val data = result.data
                imageUri = data!!.data

                imageIv.setImageURI(imageUri)
            }
            else
            {
                showToast("Cancelled!")

            }
        }
    private fun pickImageCamera(){
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Sample Title")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
            //here we will receive the image, if taken from camera
            if(result.resultCode == Activity.RESULT_OK){

                //image is taken from camera
                //we already have the image in imageUri using function pickImageCamera
                imageIv.setImageURI(imageUri)
            }
            else{
                //cancelled
                showToast("Cancelled!")
            }
        }

    private fun checkStoragePermission(): Boolean{

        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        //return true
    }

    private fun checkCameraPermissions() : Boolean{

        val cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED

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