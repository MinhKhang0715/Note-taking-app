package com.example.noteapp.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.noteapp.data.dao.NoteDAO;
import com.example.noteapp.data.entities.Note;

@Database(entities = Note.class, version = 1, exportSchema = false)
public abstract class NoteDatabase extends RoomDatabase {

    private static NoteDatabase noteDatabase;

    public static synchronized NoteDatabase getInstance(Context context) {
        if (noteDatabase == null)
            noteDatabase = Room.databaseBuilder(
                    context.getApplicationContext(),
                    NoteDatabase.class,
                    "note_db"
            ).fallbackToDestructiveMigration().build();
        return noteDatabase;
    }

    public abstract NoteDAO noteDAO();
}
