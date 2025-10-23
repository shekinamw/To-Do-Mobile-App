

package com.example.todo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class NewTask : AppCompatActivity() {

    private lateinit var titleInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var deadlineInput: EditText
    private lateinit var colorRadioGroup1: RadioGroup
    private lateinit var colorRadioGroup2: RadioGroup
    private lateinit var myDB: MyDatabaseHelper

    // Variables for update mode
    private var taskId: String? = null
    private var isUpdateMode = false
    private var selectedColor: String = "#FF6B6B" // Default color (Red)

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
        colorRadioGroup1 = findViewById(R.id.color_radio_group)
        colorRadioGroup2 = findViewById(R.id.color_radio_group2)

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

        //Save button - renamed from "Done" to "Save" in layout
        val saveButton: Button = findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            if (isUpdateMode) {
                updateTask()
            } else {
                addTask()
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
                    R.id.radio_blue -> "#4ECDC4"
                    R.id.radio_green -> "#95E1D3"
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
                    R.id.radio_yellow -> "#FFE66D"
                    R.id.radio_purple -> "#B388FF"
                    R.id.radio_orange -> "#FF9F43"
                    else -> "#FFE66D"
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

            // Populate fields with existing data
            titleInput.setText(title)
            descriptionInput.setText(description)
            deadlineInput.setText(deadline)

            // Set the color selection
            color?.let {
                selectedColor = it
                selectColorRadioButton(it)
            }

            // Change button text to "Update"
            findViewById<Button>(R.id.save_button).text = "Update"
        }
    }

    /**
     * Select the appropriate radio button based on color hex value
     */
    private fun selectColorRadioButton(color: String) {
        when (color) {
            "#FF6B6B" -> findViewById<RadioButton>(R.id.radio_red).isChecked = true
            "#4ECDC4" -> findViewById<RadioButton>(R.id.radio_blue).isChecked = true
            "#95E1D3" -> findViewById<RadioButton>(R.id.radio_green).isChecked = true
            "#FFE66D" -> findViewById<RadioButton>(R.id.radio_yellow).isChecked = true
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
            Toast.makeText(this, "Error: Title is required!", Toast.LENGTH_LONG).show()
            titleInput.requestFocus()
            return
        }

        // Add task to database with selected color
        val result = myDB.addTask(title, description, deadline, selectedColor)

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
            Toast.makeText(this, "Error: Title is required!", Toast.LENGTH_LONG).show()
            titleInput.requestFocus()
            return
        }

        // Update task in database with selected color
        taskId?.let {
            myDB.updateTask(it, title, description, deadline, selectedColor)

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
        deadlineInput.text.clear()
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
            Toast.makeText(this, "No task to delete", Toast.LENGTH_SHORT).show()
        }
    }
}