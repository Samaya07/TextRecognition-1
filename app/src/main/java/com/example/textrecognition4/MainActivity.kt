package com.example.textrecognition4

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
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

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
        cameraPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        //handle click, show input image dialog
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        textRecognizer = TextRecognition.getClient((DevanagariTextRecognizerOptions.Builder().build()))



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
             val testTaskResult = textRecognizer.process(inputImage)
                .addOnSuccessListener {text ->
                    //process completed, dismiss dialog
                    progressDialog.dismiss()
                    //get the recognized text
                    val recognizedText = text.text
                    var finalEle: String? = ""       // String with the filtered text
                    var maxSize = 0.0
                    var flag = 0
                    var flag2 = 0
                    val pattern = Regex("\\d+\\.?\\d*")   //Pattern to recognize price format
                    val arrayOfText = recognizedText.split("\n").toTypedArray()  //The recognized text into an array
                    for(x in arrayOfText) {
                        if (x.contains("Rs") or x.contains("MRP")
                            or x.contains("M.R.P") or x.contains("\u20B9")) {
                            flag = 1
                            //Log.i(TAG, x)
                            val match = pattern.find(x)
                            val value = match?.value
                            finalEle = value
                            if(finalEle == null) {flag2 = 1}
                            else {break}
                        }
                        //Log.i(TAG,x)
                        if(pattern.matches(x) && flag2==1)  //Searching for price format in
                        {                                            // the vicinity of MRP or Rs tags
                            val match = pattern.find(x)
                            val value = match?.value
                            finalEle = value
                            //Log.i(TAG,"here")
                            break
                        }
                    }
                    /*for(block in text.textBlocks){
                        if(block.text.contains("Rs") or block.text.contains("MRP")
                            or block.text.contains("M.R.P") or block.text.contains("\u20B9"))
                        {
                            flag = 1
                            Log.i(TAG, block.text)
                            Log.i(TAG,"---")
                            val match = pattern.find(block.text)
                            val value = match?.value
                            finalEle = value
                            if(finalEle == null) {flag2 = 1}
                            else {break}

                        }
                        Log.i(TAG,block.text)
                        if(pattern.matches(block.text) && flag2==1)  //Searching for price format in
                        {                                            // the vicinity of MRP or Rs tags
                            val match = pattern.find(block.text)
                            val value = match?.value
                            finalEle = value
                            Log.i(TAG,"here")
                            break
                        }
                    }*/

                    if (flag == 0) {
                        Log.i(TAG,"title")
                        for (block in text.textBlocks) {
                            for (line in block.lines) {
                                for (element in line.elements) {
                                    var size = 0.0
                                    val corners = element.cornerPoints
                                    if (corners != null && corners.size == 4) {
                                        val dx = (corners[0].x - corners[1].x).toDouble()
                                        val dy = (corners[0].y - corners[1].y).toDouble()
                                        val len = sqrt(dx * dx + dy * dy)
                                        val dx2 = (corners[2].x - corners[3].x).toDouble()
                                        val dy2 = (corners[2].y - corners[3].y).toDouble()
                                        val bred = sqrt(dx2 * dx2 + dy2 * dy2)
                                        size = 2 * (len + bred)
                                    }
                                    if (size > maxSize) {
                                        maxSize = size
                                        //finalEle = element.text biggest element
                                        finalEle =
                                            block.text //block corresponding to the biggest element
                                    }
                                }
                            }
                        }
                    }
                    //Log.i(TAG,recognizedText) //may have to remove
                    recognizedTextEt.setText(finalEle) //Remove later
                    Log.i(TAG, finalEle.toString())
                    //recognizedTextEt.setText(recognizedText)


                }
                .addOnFailureListener { e->
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

        //return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        return true
    }

    private fun checkCameraPermissions() : Boolean{

//        val cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
//        val storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
//
//        return cameraResult && storageResult
        return true
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