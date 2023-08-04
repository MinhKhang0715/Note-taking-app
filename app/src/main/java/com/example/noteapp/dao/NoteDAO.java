package com.example.noteapp.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.noteapp.entities.Note;

import java.util.List;

@Dao
public interface NoteDAO {
    @Query("SELECT * FROM note ORDER BY id DESC")
    List<Note> getAllNotes();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNote(Note note);

    @Delete
    void deleteNote(Note note);

    @Query("DELETE FROM note WHERE id in (:idList)")
    void deleteNotesByIdList(List<Integer> idList);

    @Query("DELETE FROM note")
    void deleteAllNotes();
}
