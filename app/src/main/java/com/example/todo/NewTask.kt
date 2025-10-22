/**
 * Second screen of the App
 * Description: This screen is used to add and save new notes. Each note has a title and a description.
 * The user can go back to the home screen by clicking the back button.
 * Author:
 * Due Date: October 1, 2025 01:59pm
 */

package com.example.todo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class NewTask : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_task)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Back button
        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            //val intent = Intent(this, MainActivity::class.java)
            finish()
        }

        //Save button
        val saveButton: Button = findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

        }

    }
}
