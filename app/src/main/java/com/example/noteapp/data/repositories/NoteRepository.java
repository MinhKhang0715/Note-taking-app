package com.example.noteapp.data.repositories;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.noteapp.data.dao.NoteDAO;
import com.example.noteapp.data.database.NoteDatabase;
import com.example.noteapp.data.entities.Note;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteRepository {
    private final NoteDAO noteDAO;
    private final LiveData<List<Note>> allNotes;

    public NoteRepository(Application application) {
        NoteDatabase noteDatabase = NoteDatabase.getInstance(application);
        noteDAO = noteDatabase.noteDAO();
        allNotes = noteDAO.getAllNotes();
    }

    public void insertNote(Note note) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> noteDAO.insertNote(note));
    }

    public void deleteNote(Note note) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> noteDAO.deleteNote(note));
    }

    public void deleteAllNotes() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(noteDAO::deleteAllNotes);
    }

    public void deleteNotesByIdList(List<Integer> listOfIds) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> noteDAO.deleteNotesByIdList(listOfIds));
    }

    public LiveData<List<Note>> getAllNotes() {
        return allNotes;
    }
}
