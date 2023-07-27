package com.example.noteapp.listeners;

import com.example.noteapp.entities.Note;

public interface NoteListener {
    void onNoteClicked(Note note, int position);
}
