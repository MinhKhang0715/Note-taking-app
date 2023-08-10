package com.example.noteapp.listeners;

import com.example.noteapp.ui.adapters.NotesAdapter.NoteViewHolder;
import com.example.noteapp.data.entities.Note;

public interface NoteListener {
    void onNoteClicked(Note note, int position);
    void onNoteLongClicked(NoteViewHolder noteViewHolder);
}
