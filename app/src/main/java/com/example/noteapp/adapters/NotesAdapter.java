package com.example.noteapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.noteapp.R;
import com.example.noteapp.entities.Note;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private final List<Note> listOfNotes;

    public NotesAdapter(List<Note> listOfNotes) {
        this.listOfNotes = listOfNotes;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.note_container,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.setNote(listOfNotes.get(position));
    }

    @Override
    public int getItemCount() {
        return listOfNotes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteTitle, noteSubtitle, dateTimeText;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.titleText);
            noteSubtitle = itemView.findViewById(R.id.subtitleText);
            dateTimeText = itemView.findViewById(R.id.dateTimeText);
        }

        void setNote(Note note) {
            noteTitle.setText(note.getTitle());
            if (note.getSubtitle().isEmpty()) noteSubtitle.setVisibility(View.GONE);
            else noteSubtitle.setText(note.getSubtitle());
            dateTimeText.setText(note.getDateTime());
        }
    }
}
