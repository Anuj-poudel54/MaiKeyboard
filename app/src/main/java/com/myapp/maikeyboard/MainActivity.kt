package com.myapp.maikeyboard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONArray
import org.json.JSONException

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    private fun copyToClipboard(text: String) {
        // Get the ClipboardManager
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Create a ClipData with the text
        val clip = ClipData.newPlainText("Copied Text", text)

        // Set the ClipData to the ClipboardManager
        clipboardManager.setPrimaryClip(clip)

        // Show a confirmation message
        Toast.makeText(this, "Text copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView: TextView = findViewById(R.id.ocrText)

        // Set an OnClickListener on the TextView
        textView.setOnClickListener {
            copyToClipboard(textView.text.toString())
        }

        previewView = findViewById(R.id.previewView)
        val captureButton: Button = findViewById(R.id.captureButton)

        // Request Camera Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Set up capture button click listener
        captureButton.setOnClickListener {
            capturePhoto()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }


    private val executorService = Executors.newSingleThreadExecutor()

    // Method to process the API response
    private fun onApiResponse(response: String) {

        try {
            // Parse the response string as a JSONArray

            // Parse the response string as a JSONArray
            val jsonResponse = JSONArray(response)

            // The first element in the response is an array, so get it

            // The first element in the response is an array, so get it
            val translationData = jsonResponse.getJSONArray(0)

            // The first element inside translationData is another array that contains the translation

            // The first element inside translationData is another array that contains the translation
            val translatedText = translationData.getJSONArray(0)

            // Extract the translated and original text

            // Extract the translated and original text
            val toText = translatedText.getString(0) // "नमस्कार"

            val fromText = translatedText.getString(1) // "hello"


            var ocrTextPreview = findViewById<TextView>(R.id.ocrText)
            ocrTextPreview.text = toText

        } catch (e : JSONException) {
            e.printStackTrace();
        }

    }


    // Constructor
    // Method to fetch translation from the API in background
    private fun translate(urlString: String) {
        executorService.submit {
            val response = fetchApiResponse(urlString)
            if (response != null) {
                // On response, process the result on the main thread
                val handler = Handler(Looper.getMainLooper())
                handler.post { onApiResponse(response) }
            }
        }
    }

    // Method to fetch API response
    private fun fetchApiResponse(urlString: String): String? {
        var response = ""
        try {
            // Create the URL and open connection
            val url = URL(urlString)
            Log.d("dbg", url.toString())
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 20000 // 20 seconds timeout
            connection.readTimeout = 20000

            // Read the response
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val result = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                result.append(line)
            }
            reader.close()
            response = result.toString()
        } catch (e: java.lang.Exception) {
            Log.e("API Error", "Error fetching translation mainactivity", e)
        }
        return response
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhoto() {
        val photoFile = File(filesDir, "captured_image.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    processImage(Uri.fromFile(photoFile))
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@MainActivity, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun processImage(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)


            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    Log.d("TextRecognition", "Text: ${visionText.text}")
                    var toTranslate = visionText.text.replace("\n", " ").trim()
                    toTranslate = URLEncoder.encode(toTranslate, StandardCharsets.UTF_8.toString())
                    translate(Constants.host+"/tl/${toTranslate}/en/mai")


                }
                .addOnFailureListener { e ->
                    Log.e("TextRecognition", "Error recognizing text", e)
                }
        } catch (e: Exception) {
            Log.e("TextRecognition", "Error processing image", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
