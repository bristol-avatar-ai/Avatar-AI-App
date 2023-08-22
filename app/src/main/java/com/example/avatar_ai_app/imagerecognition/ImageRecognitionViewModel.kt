package com.example.avatar_ai_app.imagerecognition

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.media.Image
import android.util.Log
import android.view.Display
import android.view.Surface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.avatar_ai_app.ErrorListener
import com.example.avatar_ai_app.ar.ArViewModel
import com.example.avatar_ai_app.chat.ChatViewModel
import com.example.avatar_ai_app.shared.ErrorType
import com.example.avatar_ai_cloud_storage.network.CloudStorageApi
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.arcore.ArFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "ImageRecognitionViewModel"

// Filename for the machine learning model.
private const val FILENAME = "model.tflite"

private const val TIMEOUT = 10000L
private const val DELAY = 500L
private const val MIN_COUNT = 5

/**
 * ViewModel class for Image Recognition which handles the image classification process.
 *
 * @param application The application instance to access the app context and other app-level functionalities.
 */
class ImageRecognitionViewModel(
    application: Application,
    private val arViewModel: ArViewModel,
    private val errorListener: ErrorListener
) : AndroidViewModel(application) {

    enum class Status { INIT, READY, ERROR, PROCESSING}

    private val _status: MutableLiveData<Status?> = MutableLiveData(null)
    val status: LiveData<Status?> get() = _status

    // Application's context, mainly used for accessing files and app-level resources.
    private val context
        get() = getApplication<Application>().applicationContext

    // Model storage location.
    private val modelFile = File(context.filesDir, FILENAME)

    // The image classifier instance responsible for the actual classification.
    private var classifier = ImageClassifier(context, modelFile.canonicalPath)

    private var result: String? = null
    private var resultCounter: Int = 0

    init {
        viewModelScope.launch(Dispatchers.IO) {
            reload()
            context
        }
    }

    private fun updateStatus(status: Status?) {
        Log.i(TAG, "updateStatus: $status")
        _status.postValue(status)
    }

    /**
     * Initialises the image classifier. This involves downloading the model and setting up the classifier.
     */
    suspend fun reload() {
        updateStatus(Status.INIT)
        // If the model cannot be obtained, notify observers and return.
        if (!CloudStorageApi.updateModel(modelFile) && !modelFile.exists()) {
            errorListener.onError(ErrorType.NETWORK)
            Log.e(TAG, "initialiseClassifier: failed to download model file")
        }

        // Initialise the classifier with the model.
        classifier.initialise()
        if (classifier.model != null) {
            Log.i(TAG, "initialiseClassifier: ready")
            updateStatus(Status.READY)
        } else {
            Log.e(TAG, "initialiseClassifier: failed to initialise")
            updateStatus(Status.ERROR)
            errorListener.onError(ErrorType.GENERIC)
        }
    }

    /**
     * Lifecycle method to handle cleanup. Closes the model to free up resources.
     */
    override fun onCleared() {
        super.onCleared()
        updateStatus(null)
        classifier.closeModel()
        result = null
        resultCounter = 0
    }

    /**
     * Function to recognise feature
     * @return Nullable string
     */

    suspend fun recogniseFeature(): String? {
        updateStatus(Status.PROCESSING)
        result = null
        resultCounter = 0

        val count =  hashMapOf<String?, Int>()

        return try {
            withTimeout(TIMEOUT) {
                var maxValue = count.values.maxOrNull() ?: 0
                while (maxValue < MIN_COUNT) {
                    val frame = arViewModel.arSceneView?.currentFrame

                    val image = getCameraImage(frame)

                    if (image != null)
                    {
                        result = classifyImage(image)
                        val value = count.getOrDefault(result, 0)
                        count[result] = value + 1
                        Log.i(TAG, "Count of " + result + " is " + count[result])
                    }

                    maxValue = count.values.maxOrNull() ?: 0

                    delay(DELAY)
                }
            }
            Log.i(TAG, "recogniseFeature: feature: $result")
            updateStatus(Status.READY)
            count.maxByOrNull {it.value}?.key
        } catch (e: TimeoutCancellationException) {
            Log.i(TAG, "recogniseFeature: not recognised before timeout")
            updateStatus(Status.READY)
            null
        }
    }

    /**
     * Captures camera image from ArFrame
     */
    private fun getCameraImage(frame: ArFrame?): Image?{
        if (frame?.camera?.trackingState == TrackingState.TRACKING)
        {
            return try {
                frame.frame.acquireCameraImage()
            }
            catch (e: Exception)
            {
                Log.w(TAG, "getCameraImage: Couldn't capture frame")
                null
            }
        }
        return null
    }

    private fun getBackCameraOrientation(context: Context): Int {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
            }
        }
        return 0
    }

    /**
     * Classify the provided bitmap image.
     *
     * @param bitmap The image that needs to be classified.
     */
    private fun classifyImage(image: Image): String? {
        val originalBitmap = yuv420ToBitmap(image)
        image.close()

        val deviceOrientation = getDeviceRotation()
        Log.i(TAG, "Device rotation is: " + deviceOrientation);
        val cameraOrientation = getBackCameraOrientation(context)
        Log.i(TAG, "Camera rotation is: " + cameraOrientation)
        val rotationRequired = calculateRotation(cameraOrientation, deviceOrientation)
        Log.i(TAG, "Rotation required is: " + rotationRequired)

        //Rotate bitmap
        val rotatedBitmap = rotateBitmap(originalBitmap, rotationRequired.toFloat())

        val result = classifier.getExhibitName(rotatedBitmap)
        Log.i(TAG, "classifyImage: result: $result")
        return result

    }

    /**
     * Function to rotate bitmap images
     */
    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Function to get device rotation
     */
    private fun getDeviceRotation(): Int {
        val displayManager = getApplication<Application>().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)

        return when (display?.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }
    private fun calculateRotation(cameraOrientation: Int, deviceOrientation: Int): Int {
        val rotation = (cameraOrientation - deviceOrientation + 360) % 360
        return rotation
    }

    /**
     * Function to convert Yuv420 to bitmap images.
     */
    private fun yuv420ToBitmap(yuvImage: Image): Bitmap {
        val yBuffer = yuvImage.planes[0].buffer
        val uBuffer = yuvImage.planes[1].buffer
        val vBuffer = yuvImage.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, yuvImage.width, yuvImage.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}

/**
 * Factory class for creating instances of [ChatViewModel].
 *
 * @param application The application context.
 * @param language The initial language for translation and speech.
 * @param errorListener The listener for handling error events.
 */
class ImageRecognitionViewModelFactory(
    private val application: Application,
    private val arViewModel: ArViewModel,
    private val errorListener: ErrorListener
) : ViewModelProvider.Factory {
    /**
     * Creates an instance of the requested ViewModel class.
     *
     * @param modelClass The class of the ViewModel to be created.
     * @return An instance of the requested ViewModel class.
     * @throws IllegalArgumentException if the provided class is not [ChatViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImageRecognitionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ImageRecognitionViewModel(application, arViewModel, errorListener) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
