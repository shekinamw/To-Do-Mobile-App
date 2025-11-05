package com.example.todo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class MyDatabaseHelper extends SQLiteOpenHelper {

    private Context context;
    private static final String DATABASE_NAME = "TaskList.db";
    private static final int DATABASE_VERSION = 2; // Incremented version for schema change
    private static final String TABLE_NAME = "my_tasks";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_DEADLINE = "deadline";
    private static final String COLUMN_COLOR = "color";
    private static final String COLUMN_IMAGE = "image"; // New column for image path/URI


    public MyDatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query =
                "CREATE TABLE " + TABLE_NAME +
                        " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_TITLE + " TEXT, " +
                        COLUMN_DESCRIPTION + " TEXT, " +
                        COLUMN_DEADLINE + " TEXT, " +
                        COLUMN_COLOR + " TEXT, " +
                        COLUMN_IMAGE + " TEXT);"; // Added image column

        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add image column to existing table
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_IMAGE + " TEXT");
        }
    }

    /**
     * CREATE - Add a new task to the database
     * @param title Task title (required)
     * @param description Task description
     * @param deadline Task deadline
     * @param color Task color (hex string)
     * @param imagePath Path or URI to the task image
     * @return row ID of the newly inserted row, or -1 if an error occurred
     */
    public long addTask(String title, String description, String deadline, String color, String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_TITLE, title);
        cv.put(COLUMN_DESCRIPTION, description);
        cv.put(COLUMN_DEADLINE, deadline);
        cv.put(COLUMN_COLOR, color);
        cv.put(COLUMN_IMAGE, imagePath);

        long result = db.insert(TABLE_NAME, null, cv);

        if (result == -1) {
            Toast.makeText(context, "Failed to add task", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Task saved successfully!", Toast.LENGTH_SHORT).show();
        }

        return result;
    }

    /**
     * READ - Get all tasks from the database ordered by ID descending (newest first)
     * @return Cursor containing all tasks
     */
    public Cursor getAllTasks() {
        String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_ID + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(query, null);
        }
        return cursor;
    }

    /**
     * READ - Search tasks by title
     * @param searchQuery The search term to filter by title
     * @return Cursor containing matching tasks
     */
    public Cursor searchTasksByTitle(String searchQuery) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME +
                " WHERE " + COLUMN_TITLE + " LIKE ? ORDER BY " + COLUMN_ID + " DESC";

        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(query, new String[]{"%" + searchQuery + "%"});
        }
        return cursor;
    }

    /**
     * READ - Get a single task by ID
     * @param id Task ID
     * @return Cursor containing the task data
     */
    public Cursor getTaskById(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        if (db != null) {
            cursor = db.query(TABLE_NAME, null, COLUMN_ID + "=?",
                    new String[]{id}, null, null, null);
        }
        return cursor;
    }

    /**
     * UPDATE - Update an existing task
     * @param id Task ID
     * @param title New title
     * @param description New description
     * @param deadline New deadline
     * @param color New color
     * @param imagePath New image path or URI
     */
    public void updateTask(String id, String title, String description, String deadline, String color, String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_TITLE, title);
        cv.put(COLUMN_DESCRIPTION, description);
        cv.put(COLUMN_DEADLINE, deadline);
        cv.put(COLUMN_COLOR, color);
        cv.put(COLUMN_IMAGE, imagePath);

        long result = db.update(TABLE_NAME, cv, COLUMN_ID + "=?", new String[]{id});

        if (result == -1) {
            Toast.makeText(context, "Failed to update task", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Task updated successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * DELETE - Delete a single task
     * @param id Task ID to delete
     */
    public void deleteTask(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        long result = db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[]{id});

        if (result == -1) {
            Toast.makeText(context, "Failed to delete task", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Task deleted successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * DELETE - Delete all tasks
     */
    public void deleteAllTasks() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
        Toast.makeText(context, "All tasks deleted!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Get column name constants for use in adapters
     */
    public static String getColumnId() { return COLUMN_ID; }
    public static String getColumnTitle() { return COLUMN_TITLE; }
    public static String getColumnDescription() { return COLUMN_DESCRIPTION; }
    public static String getColumnDeadline() { return COLUMN_DEADLINE; }
    public static String getColumnColor() { return COLUMN_COLOR; }
    public static String getColumnImage() { return COLUMN_IMAGE; }
}