package com.example.todo

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


class NewTask : AppCompatActivity() {

    private lateinit var titleInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var deadlineInput: TextView
    private lateinit var pickDateButton : Button
    private lateinit var colorRadioGroup1: RadioGroup
    private lateinit var colorRadioGroup2: RadioGroup
    private lateinit var selectImageButton: Button
    private lateinit var imagePreview: ImageView
    private lateinit var imageNameText: TextView
    private lateinit var myDB: MyDatabaseHelper

    // Variables for update mode
    private var taskId: String? = null
    private var isUpdateMode = false
    private var selectedColor: String = "#FF6B6B" // Default color (Red)
    private var selectedImagePath: String? = null
    private var currentPhotoUri: Uri? = null

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val STORAGE_PERMISSION_CODE = 101
    }

    // Activity result launchers
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                handleImageUri(uri)
            }
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            currentPhotoUri?.let { uri ->
                handleImageUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_task)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize database helper
        myDB = MyDatabaseHelper(this)

        // Initialize input fields
        titleInput = findViewById(R.id.new_note_title)
        descriptionInput = findViewById(R.id.new_note_description)
        deadlineInput = findViewById(R.id.editTextDate4)
        pickDateButton = findViewById(R.id.buttonPickDate)
        colorRadioGroup1 = findViewById(R.id.color_radio_group)
        colorRadioGroup2 = findViewById(R.id.color_radio_group2)
        selectImageButton = findViewById(R.id.select_image_button)
        imagePreview = findViewById(R.id.selected_image_preview)
        imageNameText = findViewById(R.id.image_name_text)

        // Set default selection (Red)
        findViewById<RadioButton>(R.id.radio_red).isChecked = true

        // Setup color selection listeners
        setupColorSelection()

        // Check if we're in update mode (editing an existing task)
        checkForUpdateMode()

        //Back button
        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        //Save button
        val saveButton: Button = findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            if (isUpdateMode) {
                updateTask()
            } else {
                addTask()
            }
        }

        // Image selection button
        selectImageButton.setOnClickListener {
            showImagePickerDialog()
        }

        // Image preview click - allow changing image
        imagePreview.setOnClickListener {
            showImagePickerDialog()
        }

        //Pick Date Button - Calendar Dialog
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        pickDateButton.setOnClickListener{
            val dpd = DatePickerDialog(this, DatePickerDialog.OnDateSetListener{ _, mYear, mMonth, mDay ->
                // Add 1 to month and format with leading zeros
                deadlineInput.text = String.format("%02d/%02d/%04d", mDay, mMonth + 1, mYear)
            }, year, month, day)
            //show dialog box
            dpd.show()
        }
    }

    /**
     * Show dialog to choose between camera and gallery
     */
    private fun showImagePickerDialog() {
        val options = arrayOf(
            getString(R.string.take_photo),
            getString(R.string.choose_from_gallery),
            getString(R.string.remove_image)
        )

        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.select_image_source))
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> checkCameraPermissionAndTakePhoto()
                1 -> checkStoragePermissionAndPickImage()
                2 -> removeImage()
            }
        }
        builder.show()
    }

    /**
     * Check camera permission and launch camera
     */
    private fun checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    /**
     * Check storage permission and launch gallery
     */
    private fun checkStoragePermissionAndPickImage() {
        // For Android 13+ (API 33+), we don't need READ_EXTERNAL_STORAGE
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            launchGallery()
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                launchGallery()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    /**
     * Launch camera to take a photo
     */
    private fun launchCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Create a file to save the image
        val photoFile = createImageFile()
        photoFile?.let {
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                it
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
            takePictureLauncher.launch(intent)
        }
    }

    /**
     * Launch gallery to pick an image
     */
    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    /**
     * Create a temporary file for camera image
     */
    private fun createImageFile(): File? {
        return try {
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile(
                "TASK_${System.currentTimeMillis()}_",
                ".jpg",
                storageDir
            )
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_creating_file), Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * Handle the selected image URI
     */
    private fun handleImageUri(uri: Uri) {
        try {
            // Save the image to internal storage
            val savedPath = saveImageToInternalStorage(uri)
            if (savedPath != null) {
                selectedImagePath = savedPath

                // Display the image preview
                val bitmap = BitmapFactory.decodeFile(savedPath)
                imagePreview.setImageBitmap(bitmap)
                imagePreview.visibility = android.view.View.VISIBLE

                // Show image name
                val fileName = File(savedPath).name
                imageNameText.text = fileName
                imageNameText.visibility = android.view.View.VISIBLE

                // Update button text
                selectImageButton.text = getString(R.string.change_image)

                Toast.makeText(this, getString(R.string.image_selected), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    /**
     * Save image to internal storage and return the file path
     */
    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Create a file in internal storage
            val directory = File(filesDir, "task_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val fileName = "task_${System.currentTimeMillis()}.jpg"
            val file = File(directory, fileName)

            // Compress and save the bitmap
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.flush()
            outputStream.close()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Remove the selected image
     */
    private fun removeImage() {
        selectedImagePath = null
        imagePreview.setImageDrawable(null)
        imagePreview.visibility = android.view.View.GONE
        imageNameText.visibility = android.view.View.GONE
        selectImageButton.text = getString(R.string.select_image)
        Toast.makeText(this, getString(R.string.image_removed), Toast.LENGTH_SHORT).show()
    }

    /**
     * Handle permission results
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchCamera()
                } else {
                    Toast.makeText(this, getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
                }
            }
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchGallery()
                } else {
                    Toast.makeText(this, getString(R.string.storage_permission_denied), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Setup color selection listeners for both radio groups
     * When one group is selected, clear the other group
     */
    private fun setupColorSelection() {
        colorRadioGroup1.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) {
                // Clear selection in the other group
                colorRadioGroup2.clearCheck()

                // Set color based on selection
                selectedColor = when (checkedId) {
                    R.id.radio_red -> "#FF6B6B"
                    R.id.radio_blue -> "#69A3E1"
                    R.id.radio_green -> "#5CE65C"
                    else -> "#FF6B6B"
                }
            }
        }

        colorRadioGroup2.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) {
                // Clear selection in the other group
                colorRadioGroup1.clearCheck()

                // Set color based on selection
                selectedColor = when (checkedId) {
                    R.id.radio_yellow -> "#FFEF00"
                    R.id.radio_purple -> "#B388FF"
                    R.id.radio_orange -> "#FF9F43"
                    else -> "#FFEF00"
                }
            }
        }
    }

    /**
     * Check if the activity was launched in update mode
     * If task data is passed via intent, populate the fields
     */
    private fun checkForUpdateMode() {
        if (intent.hasExtra("task_id")) {
            isUpdateMode = true
            taskId = intent.getStringExtra("task_id")

            // Get task data from intent
            val title = intent.getStringExtra("task_title")
            val description = intent.getStringExtra("task_description")
            val deadline = intent.getStringExtra("task_deadline")
            val color = intent.getStringExtra("task_color")
            val imagePath = intent.getStringExtra("task_image")

            // Populate fields with existing data
            titleInput.setText(title)
            descriptionInput.setText(description)
            deadlineInput.text = deadline

            // Set the color selection
            color?.let {
                selectedColor = it
                selectColorRadioButton(it)
            }

            // Load image if exists
            imagePath?.let {
                if (it.isNotEmpty() && File(it).exists()) {
                    selectedImagePath = it
                    val bitmap = BitmapFactory.decodeFile(it)
                    imagePreview.setImageBitmap(bitmap)
                    imagePreview.visibility = android.view.View.VISIBLE

                    val fileName = File(it).name
                    imageNameText.text = fileName
                    imageNameText.visibility = android.view.View.VISIBLE

                    selectImageButton.text = getString(R.string.change_image)
                }
            }

            // Change button text to "Update" using string resource
            findViewById<Button>(R.id.save_button).text = getString(R.string.update_button)
        }
    }

    /**
     * Select the appropriate radio button based on color hex value
     */
    private fun selectColorRadioButton(color: String) {
        when (color) {
            "#FF6B6B" -> findViewById<RadioButton>(R.id.radio_red).isChecked = true
            "#69A3E1" -> findViewById<RadioButton>(R.id.radio_blue).isChecked = true
            "#5CE65C" -> findViewById<RadioButton>(R.id.radio_green).isChecked = true
            "#FFEF00" -> findViewById<RadioButton>(R.id.radio_yellow).isChecked = true
            "#B388FF" -> findViewById<RadioButton>(R.id.radio_purple).isChecked = true
            "#FF9F43" -> findViewById<RadioButton>(R.id.radio_orange).isChecked = true
        }
    }

    /**
     * CREATE - Add a new task to the database
     * Validates that title is not empty before saving
     */
    private fun addTask() {
        val title = titleInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        val deadline = deadlineInput.text.toString().trim()

        // REQUIREMENT: Only allow tasks to be saved if the title has text in it
        if (title.isEmpty()) {
            Toast.makeText(this, getString(R.string.title_required), Toast.LENGTH_LONG).show()
            titleInput.requestFocus()
            return
        }

        // Add task to database with selected color and image path
        val result = myDB.addTask(title, description, deadline, selectedColor, selectedImagePath ?: "")

        if (result != -1L) {
            // Clear input fields after successful save
            clearInputFields()

            // Go back to MainActivity
            finish()
        }
    }

    /**
     * UPDATE - Update an existing task in the database
     * Validates that title is not empty before updating
     */
    private fun updateTask() {
        val title = titleInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        val deadline = deadlineInput.text.toString().trim()

        // Validate title is not empty
        if (title.isEmpty()) {
            Toast.makeText(this, getString(R.string.title_required), Toast.LENGTH_LONG).show()
            titleInput.requestFocus()
            return
        }

        // Update task in database with selected color and image path
        taskId?.let {
            myDB.updateTask(it, title, description, deadline, selectedColor, selectedImagePath ?: "")

            // Return to MainActivity with a result to refresh the list
            val resultIntent = Intent()
            resultIntent.putExtra("task_updated", true)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    /**
     * Helper method to clear all input fields
     */
    private fun clearInputFields() {
        titleInput.text.clear()
        descriptionInput.text.clear()
        deadlineInput.text = ""
        removeImage()
        // Reset to default color (Red)
        findViewById<RadioButton>(R.id.radio_red).isChecked = true
        selectedColor = "#FF6B6B"
    }

    /**
     * DELETE - Delete the current task (if in update mode)
     * This method can be called from a delete button if you add one to the layout
     */
    private fun deleteTask() {
        taskId?.let {
            myDB.deleteTask(it)
            finish()
        } ?: run {
            Toast.makeText(this, getString(R.string.no_task_to_delete), Toast.LENGTH_SHORT).show()
        }
    }
}