package com.android.example.textrecognitionlive.main

//import kotlinx.coroutines.delay
import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.example.textrecognitionlive.FallBActivity
import com.android.example.textrecognitionlive.ImageAnalyzerMet
import com.android.example.textrecognitionlive.R
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource


class MainFragment : Fragment(){

    companion object{

        fun newInstance() = MainFragment()
        const val DESIRED_WIDTH_CROP_PERCENT = 8
        const val DESIRED_HEIGHT_CROP_PERCENT = 74


        // This is an arbitrary number we are using to keep tab of the permission
        // request. Where an app has multiple context for requesting permission,
        // this can help differentiate the different contexts
        private const val REQUEST_CODE_PERMISSIONS = 10

        var tokens = arrayListOf<Any>()
        var labels = arrayListOf <Pair<Any, Any>>()


        // This is an array of all the permission specified in the manifest
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private const val TAG = "MainFragment"
    }


    private var displayId: Int = -1
    private var camera: Camera? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var container: ConstraintLayout
    private lateinit var viewFinder: PreviewView
    private lateinit var overlay: SurfaceView
    private lateinit var recognizedTextV: TextView
    private var db = Firebase.firestore


    private var flag = 1

    private val imageCropPercentages = MutableLiveData<Pair<Int, Int>>()
        .apply { value = Pair(DESIRED_HEIGHT_CROP_PERCENT, DESIRED_WIDTH_CROP_PERCENT) }

    /** Blocking camera and inference operations are performed using this executor. */
    private lateinit var cameraExecutor: ExecutorService


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_main, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Shut down the scoped executor. The camera executor will automatically shut down its
        // background threads after 60s of idling.
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        container = view as ConstraintLayout
        viewFinder = container.findViewById(R.id.viewFinder)
        overlay = container.findViewById(R.id.overlay)
        recognizedTextV = container.findViewById(R.id.recogText)
        recognizedTextV.movementMethod = ScrollingMovementMethod()



        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()


        // Request camera permissions
        if (allPermissionsGranted()) {
            // Wait for the views to be properly laid out
            viewFinder.post {
                // Keep track of the display in which this view is attached
                displayId = viewFinder.display.displayId

                val startBt = container.findViewById<Button>(R.id.start_button)
                val fallBt = container.findViewById<Button>(R.id.fallback_button)
                val confirmBt = container.findViewById<Button>(R.id.confirm_button1)

                //val stopBt = container.findViewById<Button>(R.id.stop_button)
                //stopBt.isEnabled = false

                setUpCamera()

                startBt.setOnClickListener {
                    if(flag == 1) {
                        flag = 0
                        startBt.apply{
                            text = getString(R.string.stop)
                        }
                        //setUpCamera()

                        val timeSource = TimeSource.Monotonic
                        val mark1 = timeSource.markNow()
                        val threeSec: Duration = 6.seconds
                        val mark2 = mark1 + threeSec

                        imageAnalyzer?.setAnalyzer(cameraExecutor, ImageAnalyzerMet(
                            requireContext(),
                            lifecycle,
                            cameraExecutor,
                            imageCropPercentages,
                            recognizedTextV, mark2
                        ))

                    }
                    else if(flag == 0)
                    {
                        flag = 1
                        startBt.apply{
                            text = getString(R.string.start)
                        }

                        Log.i(TAG, "thisonehere: $labels")
                        stopCamera()
                    }
                }

                fallBt.setOnClickListener {
                    val intent = Intent(activity, FallBActivity::class.java)
                    activity?.startActivity(intent)
                }

                confirmBt.setOnClickListener {

                    stopCamera()

                    startBt.apply{
                        text = getString(R.string.start)
                    }
                    flag = 1

                    val prodInArray = ImageAnalyzerMet.finalProduct.split(" ").toTypedArray()
                    for(prod in prodInArray)
                    {
                        labels.add(Pair(prod, "Product"))
                    }

                    //labels.add(Pair(ImageAnalyzerMet.finalProduct, "Product"))
                    labels.add(Pair(ImageAnalyzerMet.finalMRP, "MRP"))
                    labels.add(Pair(ImageAnalyzerMet.finalMFG, "MfgDate"))
                    labels.add(Pair(ImageAnalyzerMet.finalEXP, "ExpDate"))

                    tokens = tokens.distinct() as ArrayList<Any>



                    val data = hashMapOf(
                        "labels" to labels,
                        "tokens" to tokens
                    )

                    db.collection("data").add(data)
                        .addOnSuccessListener {
                            //Toast.makeText(this, "Successfully added!", Toast.LENGTH_SHORT).show()
                            labels.clear()
                            tokens.clear()

                        }
                        .addOnFailureListener{
                            //Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show()
                        }

                    Log.i(ContentValues.TAG, "labels: $labels")
                    Log.i(ContentValues.TAG, "tokens: $tokens")
                }

            }
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }


        overlay.apply {
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

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                }

                override fun surfaceCreated(holder: SurfaceHolder) {
                    drawOverlay(
                        holder,
                        DESIRED_HEIGHT_CROP_PERCENT,
                        DESIRED_WIDTH_CROP_PERCENT
                    )
                }
            })
        }
    }

    private fun stopCamera()
    {

        imageAnalyzer?.clearAnalyzer()
        //cameraProvider.unbindAll()

    }

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            cameraProvider = try {
                cameraProviderFuture.get()
            } catch (e: ExecutionException) {
                throw IllegalStateException("Camera initialization failed.", e.cause!!)
            }
            // Build and bind the camera use cases
            bindCameraUseCases(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = viewFinder.display.rotation

        val preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()



        imageAnalyzer = ImageAnalysis.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            /*.also {
                it.setAnalyzer(
                    cameraExecutor, ImageAnalyzerMet(
                        requireContext(),
                        lifecycle,
                        cameraExecutor,
                        imageCropPercentages,
                        recognizedTextV
                    )
                )
            }*/


        // Build the image analysis use case and instantiate our analyzer

        imageCropPercentages.observe(viewLifecycleOwner,
            Observer { drawOverlay(overlay.holder, it.first, it.second) })

        // Select back camera since text detection does not work with front camera
        cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
            preview.setSurfaceProvider(viewFinder.surfaceProvider)
        } catch (exc: IllegalStateException) {
            Log.e(TAG, "Use case binding failed. This must be running on main thread.", exc)
        }


    }

    private fun drawOverlay(
        holder: SurfaceHolder,
        heightCropPercent: Int,
        widthCropPercent: Int
    ) {
        val canvas = holder.lockCanvas()
        /*val bgPaint = Paint().apply {
            alpha = 140
        }
        canvas.drawPaint(bgPaint)*/
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
        val textPaint = Paint()
        textPaint.color = Color.WHITE
        textPaint.textSize = 50F

        /*val overlayText = getText(androidx.camera.core.R.string.overlay_help)
        val textBounds = Rect()
        textPaint.getTextBounds(overlayText, 0, overlayText.length, textBounds)
        val textX = (surfaceWidth - textBounds.width()) / 2f
        val textY = rectBottom + textBounds.height() + 15f // put text below rect and 15f padding
        canvas.drawText(getString(androidx.camera.core.R.string.overlay_help), textX, textY, textPaint)*/
        holder.unlockCanvasAndPost(canvas)
    }

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by comparing absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = ln(max(width, height).toDouble() / min(width, height))
        if (abs(previewRatio - ln(RATIO_4_3_VALUE))
            <= abs(previewRatio - ln(RATIO_16_9_VALUE))
        ) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { setUpCamera() }
            } else {
                Toast.makeText(
                    context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }
}