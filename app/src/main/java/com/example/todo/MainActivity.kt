package com.example.todo

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SearchView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var emptyStateText: TextView
    private lateinit var myDB: MyDatabaseHelper
    private lateinit var taskAdapter: TaskAdapter

    // ArrayLists to store all task data
    private var allTaskIds = ArrayList<String>()
    private var allTaskTitles = ArrayList<String>()
    private var allTaskDescriptions = ArrayList<String>()
    private var allTaskDeadlines = ArrayList<String>()
    private var allTaskColors = ArrayList<String>()
    private var allTaskImages = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        recyclerView = findViewById(R.id.my_recycler_view)
        searchView = findViewById(R.id.search_bar)
        emptyStateText = findViewById(R.id.empty_state_text)

        // Initialize database
        myDB = MyDatabaseHelper(this)

        // Setup RecyclerView
        setupRecyclerView()

        // REQUIREMENT: Display all saved tasks with their colors and images
        loadTasksFromDatabase()

        // Setup add task button (Button in Toolbar)
        val addButton = findViewById<Button>(R.id.addTaskButton)
        addButton.setOnClickListener {
            val intent = Intent(this, NewTask::class.java)
            startActivity(intent)
        }

        // REQUIREMENT: Search/filter tasks by title
        setupSearchView()
    }

    override fun onResume() {
        super.onResume()
        // Reload tasks when returning to this activity (e.g., after adding/editing a task)
        loadTasksFromDatabase()
    }

    /**
     * Setup RecyclerView with adapter and layout manager
     */
    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            this,
            allTaskIds,
            allTaskTitles,
            allTaskDescriptions,
            allTaskDeadlines,
            allTaskColors,
            allTaskImages
        )
        recyclerView.adapter = taskAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    /**
     * REQUIREMENT: Display all saved tasks from database
     * Tasks are stored permanently and remain saved even when app or phone restarts
     */
    private fun loadTasksFromDatabase() {
        // Clear existing data
        allTaskIds.clear()
        allTaskTitles.clear()
        allTaskDescriptions.clear()
        allTaskDeadlines.clear()
        allTaskColors.clear()
        allTaskImages.clear()

        // Query database for all tasks
        val cursor: Cursor? = myDB.getAllTasks()

        if (cursor == null || cursor.count == 0) {
            // Show empty state message
            emptyStateText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            // Hide empty state message
            emptyStateText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            // Read data from cursor
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(MyDatabaseHelper.getColumnId()))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(MyDatabaseHelper.getColumnTitle()))
                val description = cursor.getString(cursor.getColumnIndexOrThrow(MyDatabaseHelper.getColumnDescription()))
                val deadline = cursor.getString(cursor.getColumnIndexOrThrow(MyDatabaseHelper.getColumnDeadline()))
                val color = cursor.getString(cursor.getColumnIndexOrThrow(MyDatabaseHelper.getColumnColor()))
                val image = cursor.getString(cursor.getColumnIndexOrThrow(MyDatabaseHelper.getColumnImage()))

                allTaskIds.add(id)
                allTaskTitles.add(title)
                allTaskDescriptions.add(description ?: "")
                allTaskDeadlines.add(deadline ?: "")
                allTaskColors.add(color ?: "#FF6B6B") // Default to red if no color
                allTaskImages.add(image ?: "")
            }
            cursor.close()

            // Update adapter
            taskAdapter.updateData(
                allTaskIds,
                allTaskTitles,
                allTaskDescriptions,
                allTaskDeadlines,
                allTaskColors,
                allTaskImages
            )
        }
    }

    /**
     * REQUIREMENT: Search/filter tasks by title
     */
    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    filterTasks(newText)
                }
                return true
            }
        })
    }

    /**
     * Filter tasks based on search query
     * Searches in task titles
     */
    private fun filterTasks(query: String) {
        if (allTaskIds.isEmpty()) {
            return
        }

        taskAdapter.filter(
            query,
            allTaskIds,
            allTaskTitles,
            allTaskDescriptions,
            allTaskDeadlines,
            allTaskColors,
            allTaskImages
        )

        // Show/hide empty state based on filter results
        if (taskAdapter.itemCount == 0) {
            emptyStateText.text = getString(R.string.no_tasks_found)
            emptyStateText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}