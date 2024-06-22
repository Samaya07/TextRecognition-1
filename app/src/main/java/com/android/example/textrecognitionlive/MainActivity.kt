package com.android.example.textrecognitionlive

//import androidx.lifecycle.LifecycleOwner
//import com.android.example.textrecognitionlive.databinding.ActivityMainBinding
import android.Manifest
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
import android.text.method.ScrollingMovementMethod
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

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
        //detectionAreaView = findViewById((R.id.detectionAreaView))
        recognizedTextV = findViewById(R.id.recogText)
        recognizedTextV.movementMethod = ScrollingMovementMethod()
        previewView = findViewById(R.id.viewFinder)
        overlayView = findViewById(R.id.overlay)
        //recognizedTextV = findViewById(R.id.recogText)


        // Request camera permissions
        if (allPermissionsGranted()) {

            //startCamera()
            viewBinding.confirmButton.setOnClickListener{ startCamera() }
            //detectionAreaView.setDetectionArea(80f, 400f, 650f, 600f)

        } else {
            requestPermissions()
        }

        // Set up the listeners for take photo and video capture buttons

        //viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }

        //viewBinding.videoCaptureButton.setOnClickListener { captureVideo() }

        viewBinding.redoButton.setOnClickListener { stopFunction() }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }


    private fun confirmData()
    {
    }


    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraProvider: ProcessCameraProvider

    private fun stopFunction()
    {
        cameraProvider.unbindAll()
    }
    private fun startCamera() {

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
       // val cameraSource = CameraSource.Builder(baseContext,)
        //val cameraSource = CameraSource.Builder(this).setRequestedFps(12F).build()


        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()
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



    private fun showToast(message: String)
    {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}











