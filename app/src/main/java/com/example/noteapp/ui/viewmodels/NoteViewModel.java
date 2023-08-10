package com.example.noteapp.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.noteapp.data.entities.Note;
import com.example.noteapp.data.repositories.NoteRepository;

import java.util.List;

public class NoteViewModel extends AndroidViewModel {
    private final NoteRepository noteRepository;
    private final LiveData<List<Note>> allNotes;

    public NoteViewModel(@NonNull Application application) {
        super(application);
        noteRepository = new NoteRepository(application);
        allNotes = noteRepository.getAllNotes();
    }

    public void insert(Note note) {
        noteRepository.insertNote(note);
    }

    public void delete(Note note) {
        noteRepository.deleteNote(note);
    }

    public void deleteByIds(List<Integer> ids) {
        noteRepository.deleteNotesByIdList(ids);
    }

    public void deleteAllNotes() {
        noteRepository.deleteAllNotes();
    }

    public LiveData<List<Note>> getAllNotes() {
        return allNotes;
    }
}
