package com.example.todo

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class TaskAdapter(
    private val context: Context,
    private var taskIds: ArrayList<String>,
    private var taskTitles: ArrayList<String>,
    private var taskDescriptions: ArrayList<String>,
    private var taskDeadlines: ArrayList<String>,
    private var taskColors: ArrayList<String>,
    private var taskImages: ArrayList<String>
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskCard: CardView = itemView.findViewById(R.id.task_card)
        val titleText: TextView = itemView.findViewById(R.id.task_title)
        val descriptionText: TextView = itemView.findViewById(R.id.task_description)
        val deadlineText: TextView = itemView.findViewById(R.id.task_deadline)
        val taskImageView: ImageView = itemView.findViewById(R.id.task_image)
        val deleteButton: ImageButton = itemView.findViewById(R.id.delete_task_button)
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
        val imagePath = taskImages[position]

        // Set task data
        holder.titleText.text = title
        holder.descriptionText.text = description

        // Set deadline text
        if (deadline.isNotEmpty()) {
            holder.deadlineText.text = "${context.getString(R.string.deadline_prefix)}$deadline"
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

        // REQUIREMENT: Display task image if available
        if (imagePath.isNotEmpty() && File(imagePath).exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                holder.taskImageView.setImageBitmap(bitmap)
                holder.taskImageView.visibility = View.VISIBLE
            } catch (e: Exception) {
                holder.taskImageView.visibility = View.GONE
                e.printStackTrace()
            }
        } else {
            holder.taskImageView.visibility = View.GONE
        }

        // Handle delete button click
        holder.deleteButton.setOnClickListener {
            showDeleteConfirmationDialog(id, position)
        }

        // Handle card click to edit task
        holder.taskCard.setOnClickListener {
            val intent = Intent(context, NewTask::class.java)
            intent.putExtra("task_id", id)
            intent.putExtra("task_title", title)
            intent.putExtra("task_description", description)
            intent.putExtra("task_deadline", deadline)
            intent.putExtra("task_color", color)
            intent.putExtra("task_image", imagePath)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return taskIds.size
    }

    /**
     * Show confirmation dialog before deleting a task
     */
    private fun showDeleteConfirmationDialog(taskId: String, position: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.delete_task_title))
        builder.setMessage(context.getString(R.string.delete_task_message))

        builder.setPositiveButton(context.getString(R.string.delete)) { dialog, _ ->
            deleteTask(taskId, position)
            dialog.dismiss()
        }

        builder.setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    /**
     * Delete task from database and update UI
     */
    private fun deleteTask(taskId: String, position: Int) {
        val myDB = MyDatabaseHelper(context)

        // Delete image file if exists
        val imagePath = taskImages[position]
        if (imagePath.isNotEmpty()) {
            try {
                val file = File(imagePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Delete from database
        myDB.deleteTask(taskId)

        // Remove from lists
        taskIds.removeAt(position)
        taskTitles.removeAt(position)
        taskDescriptions.removeAt(position)
        taskDeadlines.removeAt(position)
        taskColors.removeAt(position)
        taskImages.removeAt(position)

        // Notify adapter
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, taskIds.size)
    }

    /**
     * Update the adapter data and refresh the RecyclerView
     */
    fun updateData(
        newIds: ArrayList<String>,
        newTitles: ArrayList<String>,
        newDescriptions: ArrayList<String>,
        newDeadlines: ArrayList<String>,
        newColors: ArrayList<String>,
        newImages: ArrayList<String>
    ) {
        taskIds = newIds
        taskTitles = newTitles
        taskDescriptions = newDescriptions
        taskDeadlines = newDeadlines
        taskColors = newColors
        taskImages = newImages
        notifyDataSetChanged()
    }

    /**
     * Filter tasks based on search query
     */
    fun filter(query: String, allIds: ArrayList<String>, allTitles: ArrayList<String>,
               allDescriptions: ArrayList<String>, allDeadlines: ArrayList<String>,
               allColors: ArrayList<String>, allImages: ArrayList<String>) {

        if (query.isEmpty()) {
            updateData(allIds, allTitles, allDescriptions, allDeadlines, allColors, allImages)
        } else {
            val filteredIds = ArrayList<String>()
            val filteredTitles = ArrayList<String>()
            val filteredDescriptions = ArrayList<String>()
            val filteredDeadlines = ArrayList<String>()
            val filteredColors = ArrayList<String>()
            val filteredImages = ArrayList<String>()

            for (i in allTitles.indices) {
                if (allTitles[i].lowercase().contains(query.lowercase())) {
                    filteredIds.add(allIds[i])
                    filteredTitles.add(allTitles[i])
                    filteredDescriptions.add(allDescriptions[i])
                    filteredDeadlines.add(allDeadlines[i])
                    filteredColors.add(allColors[i])
                    filteredImages.add(allImages[i])
                }
            }

            updateData(filteredIds, filteredTitles, filteredDescriptions, filteredDeadlines, filteredColors, filteredImages)
        }
    }
}