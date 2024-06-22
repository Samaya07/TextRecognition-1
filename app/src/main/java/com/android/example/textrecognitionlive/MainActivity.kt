package com.android.example.textrecognitionlive

//import androidx.lifecycle.LifecycleOwner
//import com.android.example.textrecognitionlive.databinding.ActivityMainBinding
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.android.example.textrecognitionlive.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.text.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity(), LifecycleOwner {
    private lateinit var viewBinding: ActivityMainBinding

    //private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null


    private lateinit var cameraExecutor: ExecutorService
    //private lateinit var overlayView: GraphicOverlay
    private lateinit var detectionAreaView: DetectionAreaView
    private lateinit var recognizedTextV: TextView
    private lateinit var previewView: PreviewView
    private lateinit var overlayView: SurfaceView







    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        //overlayView = findViewById(R.id.overlayView)
        detectionAreaView = findViewById((R.id.detectionAreaView))
        recognizedTextV = findViewById(R.id.recogText)
        previewView = findViewById(R.id.viewFinder)
        overlayView = findViewById(R.id.overlay)
        //recognizedTextV = findViewById(R.id.recogText)


        // Request camera permissions
        if (allPermissionsGranted()) {

            startCamera()
            //detectionAreaView.setDetectionArea(80f, 400f, 650f, 600f)

        } else {
            requestPermissions()
        }

        // Set up the listeners for take photo and video capture buttons
        //viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        Log.i(TAG,"captureVideoButton")
        //viewBinding.videoCaptureButton.setOnClickListener { captureVideo() }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }


    private fun takePhoto() {}

    // Implements VideoCapture use case, including start and stop capturing.
    /*private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        viewBinding.videoCaptureButton.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // create and start a new recording session
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(this@MainActivity,
                        Manifest.permission.RECORD_AUDIO) ==
                    PermissionChecker.PERMISSION_GRANTED)
                {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        viewBinding.videoCaptureButton.apply {
                            text = getString(R.string.stop_capture)
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " +
                                    "${recordEvent.error}")
                        }
                        viewBinding.videoCaptureButton.apply {
                            text = getString(R.string.start_capture)
                            isEnabled = true
                        }
                    }
                }
            }
    }*/

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private fun startCamera() {

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
       // val cameraSource = CameraSource.Builder(baseContext,)
        //val cameraSource = CameraSource.Builder(this).setRequestedFps(12F).build()


        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            //val cameraSource = CameraSource.Builder().setRequestedFps()

            val metrics = DisplayMetrics().also { previewView.display.getRealMetrics(it) }

            val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)

            val rotation = previewView.display.rotation


            // Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }
            Log.i(TAG,"before call!")




            /*val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)*/
            Log.i(TAG,"recorder: $videoCapture")

            val imageCropPercentages = MutableLiveData<Pair<Int,Int>>()
                .apply{ value = Pair(DESIRED_HEIGHT_CROP_PERCENT, DESIRED_WIDTH_CROP_PERCENT)}


            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor, ImageAnalyzerMet(
                            this, lifecycle,
                            cameraExecutor,
                            imageCropPercentages,
                            recognizedTextV
                        )
                    )
                }

           // val viewLifecycleOwner: LifecycleOwner
            /*val canvas = overlayView.holder.lockCanvas()
            Log.i(TAG,"CAN: $canvas")
            imageCropPercentages.observe(
                lifecycleOwner!!,
                Observer { drawOverlay(canvas,overlayView.holder, it.first, it.second) })*/
            //drawOverlay(overlayView.holder, 8, 74)

            overlayView.apply {
                setZOrderOnTop(true)
                holder.setFormat(PixelFormat.TRANSPARENT)
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {}

                    override fun surfaceCreated(holder: SurfaceHolder) {
                        holder.let {
                            drawOverlay(
                                overlayView.holder.lockCanvas(),
                                it,
                                DESIRED_HEIGHT_CROP_PERCENT,
                                DESIRED_WIDTH_CROP_PERCENT
                            )
                        }
                    }
                })
            }





            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                //cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    //fun View.findViewTreeLifecycleOwner(): LifecycleOwner = ViewTreeLifecycleOwner.get(this)

    private val Context.lifecycleOwner: LifecycleOwner?
        get() {
            var context: Context? = this

            while (context != null && context !is LifecycleOwner) {
                val baseContext = (context as? ContextWrapper?)?.baseContext
                context = if (baseContext == context) null else baseContext
            }

            return if (context is LifecycleOwner) context else null
        }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = ln(max(width, height).toDouble() / min(width, height))
        if (abs(previewRatio - ln(RATIO_4_3_VALUE))
            <= abs(previewRatio - ln(RATIO_16_9_VALUE))
        ) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun drawOverlay(
        canvas: Canvas,
        holder: SurfaceHolder,
        heightCropPercent: Int,
        widthCropPercent: Int
    ) {
        //Log.i(TAG, holder.toString())
        //val canvas = holder.lockCanvas()
        Log.i(TAG, canvas.toString())
        val bgPaint = Paint().apply {
            alpha = 140
        }
        canvas.drawPaint(bgPaint)
        val rectPaint = Paint()
        rectPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        rectPaint.style = Paint.Style.FILL
        rectPaint.color = Color.WHITE
        val outlinePaint = Paint()
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.color = Color.WHITE
        outlinePaint.strokeWidth = 4f
        val surfaceWidth = holder.surfaceFrame.width()
        val surfaceHeight = holder.surfaceFrame.height()

        val cornerRadius = 25f
        // Set rect centered in frame
        val rectTop = surfaceHeight * heightCropPercent / 2 / 100f
        val rectLeft = surfaceWidth * widthCropPercent / 2 / 100f
        val rectRight = surfaceWidth * (1 - widthCropPercent / 2 / 100f)
        val rectBottom = surfaceHeight * (1 - heightCropPercent / 2 / 100f)
        val rect = RectF(rectLeft, rectTop, rectRight, rectBottom)
        canvas.drawRoundRect(
            rect, cornerRadius, cornerRadius, rectPaint
        )
        canvas.drawRoundRect(
            rect, cornerRadius, cornerRadius, outlinePaint
        )

        holder.unlockCanvasAndPost(canvas)
    }

    private fun requestPermissions() {

        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {

        const val DESIRED_WIDTH_CROP_PERCENT = 8
        const val DESIRED_HEIGHT_CROP_PERCENT = 74
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    fun extractProduct(text: Text): ArrayList<Any> {
        val recognizedText = text.text

        data class ElementSize(val size: Double, val element: String)
        data class SizeMRP(val size: Double, val element: String)
        val elementSizes = mutableListOf<ElementSize>()
        val sizesMrp = mutableListOf<SizeMRP>()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    var size = 0.0
                    val corners = element.cornerPoints
                    val elementText = element.text
                    if (corners != null && corners.size == 4) {
                        val dx1 = (corners[0].x - corners[3].x).toDouble()
                        val dy1 = (corners[0].y - corners[3].y).toDouble()
                        val len1 = sqrt(dx1 * dx1 + dy1 * dy1)
//                        val dx2 = (corners[2].x - corners[3].x).toDouble()
//                        val dy2 = (corners[2].y - corners[3].y).toDouble()
//                        val len2 = sqrt(dx2 * dx2 + dy2 * dy2)
//                        size = (len1 + len2) * 2
                        size = len1
                    }
                    if (elementText.matches(Regex("\\d+(\\.\\d+)?"))) {
                        var sizeM = 0.0
                        val cornersM = element.cornerPoints
                        if (cornersM != null && cornersM.size == 4) {
                            val dx1 = (cornersM[0].x - cornersM[3].x).toDouble()
                            val dy1 = (cornersM[0].y - cornersM[3].y).toDouble()
                            val len1M = sqrt(dx1 * dx1 + dy1 * dy1)
                            val dx2 = (cornersM[2].x - cornersM[3].x).toDouble()
                            val dy2 = (cornersM[2].y - cornersM[3].y).toDouble()
                            val len2M = sqrt(dx2 * dx2 + dy2 * dy2)
                            sizeM = (len1M + len2M) * 2
                            //sizeM = len1M
                        }

                        sizesMrp.add(SizeMRP(sizeM, element.text))
                    }
                    elementSizes.add(ElementSize(size, element.text))

                }
            }
        }
        val top5Elements: List<String> =
            elementSizes.sortedByDescending { it.size }.take(3).map { it.element }
        val top3MRP: List<String> =
            sizesMrp.sortedByDescending { it.size }.take(5).map { it.element }

//Size for MRP

        //Score calculation
        //val wordsArray = recognizedText.split("(\\s+|:|;|.)".toRegex()).toTypedArray()

        val wordsArray = recognizedText.split("[\\s:;.]".toRegex()).filter { it.isNotEmpty() }.toTypedArray()
        if(wordsArray.isEmpty()){
            return arrayListOf("0",0.0,"0",0.0,listOf(0.0),"0",listOf("0"))
        }

        val recognizedTextLines = recognizedText.split("\n").toTypedArray()

        var score = 0.0
        var mscore = 0.0
        //var j = 1.0
        val len = wordsArray.size
        val scoreArr = mutableListOf<Double>()
        val mscoreArr = mutableListOf<Double>()
        //4th condition
        val blockOfMrp = extractDateMrpBlock(text).toString()
        val blockArray = blockOfMrp.split("[\\s:;.]".toRegex()).filter { it.isNotEmpty() }.toTypedArray()
        //3rd condition MRP function
        var mrpValue = "noneNull"
        for (line in recognizedTextLines) {
            if (line.contains(Regex("""\b(?:Rs|MRP|mrp|₹|MR|MRR|MPP|MPR|M.R.P|Rs.|)\b""",RegexOption.IGNORE_CASE))) {
                extractMrpValue(line)?.let {
                    mrpValue = it
                }
            }
        }

        for (i in wordsArray.indices) {
            if (wordsArray[i].length  > 3) {
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

                val num: Double = wordsArray[i].toDouble()
                mscore += 0.222
                if (num in 2.0..5000.0) {
                    mscore += 0.35
                }
                //2nd condition
                if (num!= 9.0 && (num % 5 == 0.0 || num % 10 == 0.0 || (num - 99) % 100 == 0.0 || num % 100 == 0.0  || (num - 9) % 10 == 0.0 )) {
                    mscore += 0.25
                }
                //3rd condition
                if (mrpValue == wordsArray[i]) {
                    showToast("Line")
                    mscore += 1
                }

                if (2020 < num && num < 2030) {
                    mscore -= 0.2
                }
                //4th condition
                if (wordsArray[i] in blockArray) {
                    //showToast("block")
                    mscore += 0.9
                }

                if (i < (len / 3)) {
                    score += 0.15
                } else {
                    if (i < (len * 2 / 3)) {
                        score += 0.1
                    }
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
                    //mscoreArr[j] += adder-0.2
                    adder -= 0.1
                }
            }
        }
        var mrpAdder = 0.4
        for (i in top3MRP.indices) {
            for (j in wordsArray.indices) {
                if (top3MRP[i] == wordsArray[j]) {
                    mscoreArr[j] += mrpAdder
                    mrpAdder -= 0.1
                }
            }
        }

        //Setting up max scorers
        val i1 = scoreArr.indexOf(scoreArr.maxOrNull())
        var max1 = wordsArray[i1]
        val max1Score = scoreArr[i1]
        scoreArr[i1] = 0.0
        val i2 = scoreArr.indexOf(scoreArr.maxOrNull())
        max1 += " "+ wordsArray[i2]
        //val max2Score = scoreArr[i2]
        scoreArr[i2] = 0.0
        val i3 = scoreArr.indexOf(scoreArr.maxOrNull())
        max1 += " "+wordsArray[i3]

        val j1 = mscoreArr.indexOf(mscoreArr.maxOrNull())
        val m1 = wordsArray[j1]
        val m1Score = mscoreArr[j1]

        val wordsArrayReturn = wordsArray.joinToString(prefix = "[", postfix = "]", separator = ", ")

        return arrayListOf(max1, max1Score, m1, m1Score, mscoreArr, wordsArrayReturn,top3MRP)
        //, wordsArray, mrpValue)
        //return wordsArray[i1]
    }

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
        Log.i(ContentValues.TAG, resBlock.toString())
        return resBlock
    }

    fun extractDates(text: String): Pair<String?, String?> {
        val potentialDates = mutableListOf<String>()

        // Regex for DD/MM/YYYY, DD/MM/YY, DDMMyy, DD.MM.YYYY, and DD.MM.YY formats
        //val dateRegex = """\b\d{2}\s*[-/.\s]\s*\d{2}\s*[-/.\s]\s*(?:\d{2}|\d{4})\b|\b\d{2}\s*[A-Z]{3,}\s*\d{2,4}\b""".toRegex()
        val dateRegex = """\b\d{2}\s*[/.\s]\s*\d{2}\s*[/.\s]\s*(?:\d{2}|\d{4})\b|\b\d{2}\s*[A-Z]{3}\s*\d{2}\b""".toRegex()


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
                SimpleDateFormat("dd/MM/yy", Locale.ENGLISH),
                SimpleDateFormat("ddMMMyy", Locale.ENGLISH),
                SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH),
                SimpleDateFormat("dd.MM.yy", Locale.ENGLISH),
                SimpleDateFormat("dd MM yyyy", Locale.ENGLISH),  // Added spaces version
                SimpleDateFormat("dd MM yy", Locale.ENGLISH),     // Added spaces version
                SimpleDateFormat("dd MMM yy", Locale.ENGLISH),
                SimpleDateFormat("MM/yy", Locale.ENGLISH),
                SimpleDateFormat("MMM/yy", Locale.ENGLISH)

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

    private fun showToast(message: String)
    {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}











