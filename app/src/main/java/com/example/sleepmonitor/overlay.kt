package com.google.mediapipe.examples.facelandmarker
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.sleepmonitor.R
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: FaceLandmarkerResult? = null
    private var linePaint = Paint()
    private var pointPaint = Paint()
    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var isEyeClosed = false
    private var sleepPercentage = 0.0
    private var eyeClosedStartTime: Long = 0
    private var totalObservationTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isAlarmTriggered = false


    init {
        initPaints()
    }

    fun clear() {
        results = null
        linePaint.reset()
        pointPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        linePaint.color = ContextCompat.getColor(context!!, R.color.mp_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.GREEN // Changed to green color
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (results == null || results!!.faceLandmarks().isEmpty()) {
            clear()
            return
        }

        results?.let { faceLandmarkerResult ->
            for (landmark in faceLandmarkerResult.faceLandmarks()) {
                // Check eye closure
                checkEyeClosureAndTriggerAlarm(landmark)
            }
        }


        results?.let { faceLandmarkerResult ->
            for (landmark in faceLandmarkerResult.faceLandmarks()) {
                // Define eye landmark indices
                val leftEyeIndices = listOf(33, 133, 160, 159, 158, 157, 173)
                val rightEyeIndices = listOf(362, 263, 387, 386, 385, 384, 398)

                // Draw points for left eye
                leftEyeIndices.forEach { index ->
                    val eyeLandmark = landmark[index]
                    val (transformedX, transformedY) = transformCoordinates(
                        eyeLandmark.x(),
                        eyeLandmark.y()
                    )
                    canvas.drawPoint(
                        transformedX * imageWidth * scaleFactor,
                        transformedY * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

                // Draw points for right eye
                rightEyeIndices.forEach { index ->
                    val eyeLandmark = landmark[index]
                    val (transformedX, transformedY) = transformCoordinates(
                        eyeLandmark.x(),
                        eyeLandmark.y()
                    )
                    canvas.drawPoint(
                        transformedX * imageWidth * scaleFactor,
                        transformedY * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

                // Draw connections for eyes
                val eyeConnectors = listOf(
                    Pair(33, 133), Pair(133, 160), Pair(160, 159), Pair(159, 158),
                    Pair(158, 157), Pair(157, 173), Pair(173, 33), // Left eye
                    Pair(362, 263), Pair(263, 387), Pair(387, 386), Pair(386, 385),
                    Pair(385, 384), Pair(384, 398), Pair(398, 362) // Right eye
                )

                eyeConnectors.forEach { connector ->
                    val startLandmark = landmark[connector.first]
                    val endLandmark = landmark[connector.second]
                    val (startX, startY) = transformCoordinates(
                        startLandmark.x(),
                        startLandmark.y()
                    )
                    val (endX, endY) = transformCoordinates(
                        endLandmark.x(),
                        endLandmark.y()
                    )
                    canvas.drawLine(
                        startX * imageWidth * scaleFactor,
                        startY * imageHeight * scaleFactor,
                        endX * imageWidth * scaleFactor,
                        endY * imageHeight * scaleFactor,
                        linePaint
                    )
                }
            }
        }
    }

    private fun transformCoordinates(x: Float, y: Float): Pair<Float, Float> {
        // Adjusted for front camera mirroring effect
        return Pair(1 - y, x)
    }

    private fun calculateEAR(landmark: List<NormalizedLandmark>, indices: List<Int>): Float {
        val vertical1 = distance(landmark[indices[1]], landmark[indices[5]])
        val vertical2 = distance(landmark[indices[2]], landmark[indices[4]])
        val horizontal = distance(landmark[indices[0]], landmark[indices[3]])
        return (vertical1 + vertical2) / (2.0f * horizontal)
    }

    private fun distance(l1:NormalizedLandmark, l2: NormalizedLandmark): Float {
        return sqrt((l1.x() - l2.x()).pow(2) + (l1.y() - l2.y()).pow(2))
    }

    private fun checkEyeClosureAndTriggerAlarm(landmark: List<NormalizedLandmark>) {
        val leftEar = calculateEAR(landmark, listOf(33, 160, 158, 133, 153, 144))
        val rightEar = calculateEAR(landmark, listOf(362, 387, 385, 263, 373, 380))

        val currentTime = System.currentTimeMillis()
        println("left Ear : $leftEar rightEar : $rightEar")
        println(isEyeClosed)
        println(isAlarmTriggered)
        if (leftEar < EAR_THRESHOLD) {
            if (!isEyeClosed) {
                isEyeClosed = true
                eyeClosedStartTime = currentTime
            } else {
                val closedDuration = currentTime - eyeClosedStartTime
                if (closedDuration > 10000 && !isAlarmTriggered) {
                    triggerAlarm()
                    isAlarmTriggered = true
                }
            }
        } else {
            isEyeClosed = false
            isAlarmTriggered = false
            eyeClosedStartTime = 0L
        }
    }

    private fun triggerAlarm() {
        val mediaPlayer = MediaPlayer.create(context, R.raw.alarm_sound)
        mediaPlayer.start()

        Toast.makeText(context, "ALERT: Eyes closed for too long!", Toast.LENGTH_SHORT).show()
    }

    private fun handleEyeClosure(isClosed: Boolean) {
        val currentTime = System.currentTimeMillis()
        if (isClosed) {
            if (!isEyeClosed) {
                isEyeClosed = true
                eyeClosedStartTime = currentTime
            }
        } else {
            if (isEyeClosed) {
                val closedDuration = currentTime - eyeClosedStartTime
                totalObservationTime += closedDuration
                isEyeClosed = false
            }
        }

        // Update sleep percentage
        if (isEyeClosed) {
            val closedDuration = currentTime - eyeClosedStartTime
            sleepPercentage = ((closedDuration + totalObservationTime) / OBSERVATION_WINDOW) * 100.0
        } else {
            sleepPercentage = (totalObservationTime / OBSERVATION_WINDOW) * 100.0
        }
    }


    fun setResults(
        faceLandmarkerResults: FaceLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = faceLandmarkerResults

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F
        private const val TAG = "Face Landmarker Overlay"
        private const val OBSERVATION_WINDOW = 10000L
        private const val EAR_THRESHOLD = 0.2
    }
}