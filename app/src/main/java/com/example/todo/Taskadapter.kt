package com.example.todo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private val context: Context,
    private var taskIds: ArrayList<String>,
    private var taskTitles: ArrayList<String>,
    private var taskDescriptions: ArrayList<String>,
    private var taskDeadlines: ArrayList<String>,
    private var taskColors: ArrayList<String>
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskCard: CardView = itemView.findViewById(R.id.task_card)
        val titleText: TextView = itemView.findViewById(R.id.task_title)
        val descriptionText: TextView = itemView.findViewById(R.id.task_description)
        val deadlineText: TextView = itemView.findViewById(R.id.task_deadline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.task_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val title = taskTitles[position]
        val description = taskDescriptions[position]
        val deadline = taskDeadlines[position]
        val color = taskColors[position]
        val id = taskIds[position]

        // Set task data
        holder.titleText.text = title
        holder.descriptionText.text = description

        // Set deadline text
        if (deadline.isNotEmpty()) {
            holder.deadlineText.text = "Deadline: $deadline"
            holder.deadlineText.visibility = View.VISIBLE
        } else {
            holder.deadlineText.visibility = View.GONE
        }

        // REQUIREMENT: Show task with selected color
        // Set the background color of the card
        try {
            holder.taskCard.setCardBackgroundColor(Color.parseColor(color))
        } catch (e: Exception) {
            // If color parsing fails, use default color
            holder.taskCard.setCardBackgroundColor(Color.parseColor("#FF6B6B"))
        }

        // REQUIREMENT: Set size of task tile based on text size
        // Adjust card height based on content length
        val titleLength = title.length
        val descriptionLength = description.length
        val totalLength = titleLength + descriptionLength

        // Calculate dynamic height based on content
        val baseHeight = 100 // Minimum height in dp
        val additionalHeight = (totalLength / 20) * 10 // Add 10dp for every 20 characters
        val calculatedHeight = baseHeight + additionalHeight

        // Convert dp to pixels
        val scale = context.resources.displayMetrics.density
        val heightInPixels = (calculatedHeight * scale + 0.5f).toInt()

        // Set the height
        val layoutParams = holder.taskCard.layoutParams
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT // Use wrap_content for better text display
        holder.taskCard.layoutParams = layoutParams

        // Handle click to edit task
        holder.taskCard.setOnClickListener {
            val intent = Intent(context, NewTask::class.java)
            intent.putExtra("task_id", id)
            intent.putExtra("task_title", title)
            intent.putExtra("task_description", description)
            intent.putExtra("task_deadline", deadline)
            intent.putExtra("task_color", color)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return taskIds.size
    }

    /**
     * Update the adapter data and refresh the RecyclerView
     */
    fun updateData(
        newIds: ArrayList<String>,
        newTitles: ArrayList<String>,
        newDescriptions: ArrayList<String>,
        newDeadlines: ArrayList<String>,
        newColors: ArrayList<String>
    ) {
        taskIds = newIds
        taskTitles = newTitles
        taskDescriptions = newDescriptions
        taskDeadlines = newDeadlines
        taskColors = newColors
        notifyDataSetChanged()
    }

    /**
     * Filter tasks based on search query
     */
    fun filter(query: String, allIds: ArrayList<String>, allTitles: ArrayList<String>,
               allDescriptions: ArrayList<String>, allDeadlines: ArrayList<String>,
               allColors: ArrayList<String>) {

        if (query.isEmpty()) {
            updateData(allIds, allTitles, allDescriptions, allDeadlines, allColors)
        } else {
            val filteredIds = ArrayList<String>()
            val filteredTitles = ArrayList<String>()
            val filteredDescriptions = ArrayList<String>()
            val filteredDeadlines = ArrayList<String>()
            val filteredColors = ArrayList<String>()

            for (i in allTitles.indices) {
                if (allTitles[i].lowercase().contains(query.lowercase())) {
                    filteredIds.add(allIds[i])
                    filteredTitles.add(allTitles[i])
                    filteredDescriptions.add(allDescriptions[i])
                    filteredDeadlines.add(allDeadlines[i])
                    filteredColors.add(allColors[i])
                }
            }

            updateData(filteredIds, filteredTitles, filteredDescriptions, filteredDeadlines, filteredColors)
        }
    }
}

