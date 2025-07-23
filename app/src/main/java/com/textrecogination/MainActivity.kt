package com.textrecogination

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.cardview.widget.CardView
import com.balsikandar.crashreporter.CrashReporter
import com.canhub.cropper.CropImageView
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var textView: TextView
    private lateinit var copyIcon: ImageView
    private var imageBitmap: Bitmap? = null
    private var croppedBitmap: Bitmap? = null
    private lateinit var mainLayout: LinearLayout
    private lateinit var cropLayout: LinearLayout
    private lateinit var cropImageView: CropImageView
    private lateinit var detectButton: Button

    private val REQUEST_STORAGE = 101
    private val REQUEST_CAMERA = 1
    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize crash reporter
        CrashReporter.initialize(this)

        // Initialize views
        initializeViews()

        // Setup click listeners
        setupClickListeners()

        // Initial UI state
        setupInitialState()
    }

    private fun initializeViews() {
        imageView = findViewById(R.id.image)
        textView = findViewById(R.id.text)
        copyIcon = findViewById(R.id.copy_icon) // You'll need to add this ID to the layout
        mainLayout = findViewById(R.id.main)
        cropLayout = findViewById(R.id.im_layout)
        cropImageView = findViewById(R.id.cropImageView)
        detectButton = findViewById(R.id.detectbtn)
    }

    private fun setupClickListeners() {
        // Camera button
        findViewById<Button>(R.id.snapbtn).setOnClickListener {
            try {
                dispatchTakePictureIntent()
            } catch (e: IOException) {
                showToast("Error opening camera: ${e.message}")
            }
        }

        // Gallery button
        findViewById<Button>(R.id.storeimage).setOnClickListener {
            openGallery()
        }

        // Detect text button
        detectButton.setOnClickListener {
            detectText()
        }

        // Crop confirm button
        findViewById<Button>(R.id.croped_image).setOnClickListener {
            confirmCrop()
        }

        // Copy text functionality
        copyIcon?.setOnClickListener {
            copyTextToClipboard()
        }
    }

    private fun setupInitialState() {
        cropLayout.visibility = View.GONE
        detectButton.isEnabled = false
        detectButton.alpha = 0.5f

        // Set placeholder text
        textView.text = "Extracted text will appear here..."
    }

    private fun openGallery() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(Intent.createChooser(intent, "Select Image"), REQUEST_STORAGE)
    }

    @Throws(IOException::class)
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (e: IOException) {
                showToast("Error creating image file")
                null
            }

            photoFile?.let {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "com.textrecogination.fileprovider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_CAMERA)
            }
        } else {
            showToast("No camera app found")
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(imageFileName, ".jpg", storageDir)
        currentPhotoPath = image.absolutePath
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK) return

        when (requestCode) {
            REQUEST_CAMERA -> handleCameraResult()
            REQUEST_STORAGE -> handleGalleryResult(data)
        }
    }

    private fun handleCameraResult() {
        currentPhotoPath?.let { path ->
            imageBitmap = BitmapFactory.decodeFile(path)
            imageBitmap?.let {
                showCropInterface(it)
            } ?: run {
                showToast("Failed to load image from camera")
            }
        } ?: run {
            showToast("No image captured")
        }
    }

    private fun handleGalleryResult(data: Intent?) {
        val imageUri: Uri? = data?.data
        imageUri?.let { uri ->
            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                imageBitmap?.let {
                    showCropInterface(it)
                }
            } catch (e: Exception) {
                showToast("Failed to load image from gallery")
            }
        } ?: run {
            showToast("No image selected")
        }
    }

    private fun showCropInterface(bitmap: Bitmap) {
        cropImageView.setImageBitmap(bitmap)
        mainLayout.visibility = View.GONE
        cropLayout.visibility = View.VISIBLE

        // Clear previous text
        textView.text = "Extracted text will appear here..."
    }

    private fun confirmCrop() {
        croppedBitmap = cropImageView.croppedImage
        croppedBitmap?.let { bitmap ->
            imageView.setImageBitmap(bitmap)
            mainLayout.visibility = View.VISIBLE
            cropLayout.visibility = View.GONE

            // Enable detect button
            detectButton.isEnabled = true
            detectButton.alpha = 1.0f

            // Clear crop image view
            cropImageView.clearImage()

            showToast("Image ready for text extraction")
        } ?: run {
            showToast("Failed to crop image")
        }
    }

    private fun detectText() {
        croppedBitmap?.let { bitmap ->
            showLoadingState(true)

            val image = FirebaseVisionImage.fromBitmap(bitmap)
            val detector = FirebaseVision.getInstance().visionTextDetector

            detector.detectInImage(image)
                .addOnSuccessListener { firebaseVisionText ->
                    showLoadingState(false)
                    processDetectedText(firebaseVisionText)
                }
                .addOnFailureListener { exception ->
                    showLoadingState(false)
                    showToast("Failed to detect text: ${exception.message}")
                }
        } ?: run {
            showToast("No image selected for text detection")
        }
    }

    private fun processDetectedText(text: FirebaseVisionText) {
        val blocks = text.blocks
        if (blocks.isEmpty()) {
            textView.text = "No text found in the image"
            return
        }

        val extractedText = StringBuilder()
        for (block in blocks) {
            extractedText.append(block.text)
            extractedText.append("\n\n")
        }

        val finalText = extractedText.toString().trim()
        textView.text = finalText

        // Show success message
        showToast("Text extracted successfully!")

        // Enable copy functionality if text is found
        copyIcon?.visibility = if (finalText.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun copyTextToClipboard() {
        val textToCopy = textView.text.toString()
        if (textToCopy.isNotEmpty() && textToCopy != "Extracted text will appear here...") {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Extracted Text", textToCopy)
            clipboard.setPrimaryClip(clip)
            showToast("Text copied to clipboard")
        } else {
            showToast("No text to copy")
        }
    }

    private fun showLoadingState(isLoading: Boolean) {
        detectButton.text = if (isLoading) "Extracting..." else "Extract Text"
        detectButton.isEnabled = !isLoading
        detectButton.alpha = if (isLoading) 0.7f else 1.0f

        if (isLoading) {
            textView.text = "Processing image..."
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up bitmaps to prevent memory leaks
        imageBitmap?.recycle()
        croppedBitmap?.recycle()
    }
}