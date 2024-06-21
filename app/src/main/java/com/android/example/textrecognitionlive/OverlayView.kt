package com.android.example.textrecognitionlive


/*import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.text.Text

class OverlayView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        typeface = Typeface.DEFAULT_BOLD
    }
    private var elements: List<Text.Element> = listOf()

    fun updateElements(elements: List<Text.Element>) {
        this.elements = elements
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (element in elements) {
            val rect = element.boundingBox
            if (rect != null) {
                canvas.drawRect(rect, paint)
                canvas.drawText(element.text, rect.left.toFloat(), rect.bottom.toFloat(), textPaint)

            }
        }
    }
}*/

//import androidx.constraintlayout.core.motion.utils.Utils
//import androidx.constraintlayout.core.motion.utils.Utils
//import com.google.mlkit.md.Utils
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.android.example.textrecognitionlive.GraphicOverlay.Graphic


/**
 * A view which renders a series of custom graphics to be overlaid on top of an associated preview
 * (i.e., the camera preview). The creator can add graphics objects, update the objects, and remove
 * them, triggering the appropriate drawing and invalidation within the view.
 *
 *
 * Supports scaling and mirroring of the graphics relative the camera's preview properties. The
 * idea is that detection items are expressed in terms of a preview size, but need to be scaled up
 * to the full view size, and also mirrored in the case of the front-facing camera.
 *
 *
 * Associated [Graphic] items should use [.translateX] and [ ][.translateY] to convert to view coordinate from the preview's coordinate.
 */
class GraphicOverlay(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val lock = Any()

    private var previewWidth: Int = 0
    private var widthScaleFactor = 1.0f
    private var previewHeight: Int = 0
    private var heightScaleFactor = 1.0f
    private val graphics = ArrayList<Graphic>()

    //lateinit var cameraSource: CameraSource


    private val paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        setBackgroundColor(Color.BLACK)
        textSize = 24f
        typeface = Typeface.DEFAULT_BOLD
    }

    /**
     * Base class for a custom graphics object to be rendered within the graphic overlay. Subclass
     * this and implement the [Graphic.draw] method to define the graphics element. Add
     * instances to the overlay using [GraphicOverlay.add].
     */
    abstract class Graphic protected constructor(protected val overlay: GraphicOverlay) {
        protected val context: Context = overlay.context



        /** Draws the graphic on the supplied canvas.  */
        abstract fun draw(canvas: Canvas)



    }

    /** Removes all graphics from the overlay.  */
    fun clear() {
        synchronized(lock) {
            graphics.clear()
        }
        postInvalidate()
    }

    /** Adds a graphic to the overlay.  */
    fun add(graphic: Graphic) {
        synchronized(lock) {
            graphics.add(graphic)
        }
    }

    /**
     * Sets the camera attributes for size and facing direction, which informs how to transform image
     * coordinates later.
     */
    private fun setCameraInfo() {

        //val previewSize = cameraSource.previewSize ?: return
        /*if (Utils.isPortraitMode(context)) {
            // Swap width and height when in portrait, since camera's natural orientation is landscape.
            previewWidth = previewSize.height
            previewHeight = previewSize.width
        } else {
            previewWidth = previewSize.width
            previewHeight = previewSize.height
        }*/
        /*previewWidth = previewSize.height
        previewHeight = previewSize.width*/
        previewWidth = 1620
        previewHeight = 3450
    }

    fun translateX(x: Float): Float = x * widthScaleFactor
    fun translateY(y: Float): Float = y * heightScaleFactor

    /**
     * Adjusts the `rect`'s coordinate from the preview's coordinate system to the view
     * coordinate system.
     */
    fun translateRect(rect: Rect) = RectF(
        translateX(rect.left.toFloat()),
        translateY(rect.top.toFloat()),
        translateX(rect.right.toFloat()),
        translateY(rect.bottom.toFloat())
    )

    //private var elements: List<Text.Element> = listOf()
    private var elements = String()
    fun updateElements(elements: String) {
        this.elements = elements
        //setCameraInfo()
        invalidate()
    }

    /** Draws the overlay with its associated graphic objects.  */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
       // Log.i(TAG, "canvas: $canvas")
        //val cs = CameraSource.Builder(context).setRequestedFps(5)
        //cameraSource = CameraSource.Builder(context,recognizer).build()
        //cameraS = CameraSource.build()





        /*if (previewWidth > 0 && previewHeight > 0) {
            widthScaleFactor = width.toFloat() / previewWidth
            heightScaleFactor = height.toFloat() / previewHeight
        }*/
        //super.onDraw(canvas)

        /*for (element in elements) {
            val rect = element.boundingBox
            if (rect != null) {
                translateRect(rect)

                canvas.drawRect(rect, paint)
                canvas.drawText(element.text, rect.left.toFloat(), rect.bottom.toFloat(), textPaint)

            }
        }*/
        Log.i(TAG, "think: $elements , done")
        val arrayFinal = elements.split("\n").toTypedArray()
        var incr: Float = 0F
        for(x in arrayFinal)
        {
            canvas.drawText(x, 50F, 100F+incr,textPaint)
            incr += 20F
        }
       // canvas.drawText(elements, 50F, 100F,textPaint)
        //draw(canvas)

        synchronized(lock) {
            graphics.forEach { it.draw(canvas) }
        }
    }
}