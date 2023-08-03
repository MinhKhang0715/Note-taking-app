package com.example.noteapp.adapters;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.noteapp.R;
import com.example.noteapp.activities.MainActivity;
import com.example.noteapp.entities.Note;
import com.example.noteapp.listeners.NoteListener;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private List<Note> listOfNotes;
    private final List<Note>  intermediateListOfNotes;
    private final NoteListener noteListener;
    private final List<NoteViewHolder> selectedNoteViewHolderList = new ArrayList<>();
    public boolean isSelectedAll = false;
    public volatile boolean isLongClickConsumed = false;

    public NotesAdapter(List<Note> listOfNotes, NoteListener noteListener) {
        this.listOfNotes = listOfNotes;
        this.noteListener = noteListener;
        intermediateListOfNotes = listOfNotes;
    }

    public void setSearch(final String searchKeyword) {
        if (searchKeyword.trim().isEmpty()) listOfNotes = intermediateListOfNotes;
        else {
            List<Note> temp = new ArrayList<>();
            for (Note note : intermediateListOfNotes) {
                if (note.getTitle().toLowerCase().contains(searchKeyword.toLowerCase()) ||
                        note.getSubtitle().toLowerCase().contains(searchKeyword.toLowerCase()) ||
                        note.getNoteContent().toLowerCase().contains(searchKeyword.toLowerCase()))
                    temp.add(note);
            }
            listOfNotes = temp;
        }
//        notifyItemRangeChanged(0, listOfNotes.size());
        notifyDataSetChanged();
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

        if (MainActivity.isEventCheckbox || MainActivity.isCancelButtonClicked)
            holder.isSelected = isSelectedAll;

        if (holder.isSelected()) {
            holder.checkbox.setVisibility(View.VISIBLE);
            if (selectedNoteViewHolderList.contains(holder)) return;
            selectedNoteViewHolderList.add(holder);
            Log.d("Check holder list", "Size is " + selectedNoteViewHolderList.size());
        } else {
            holder.checkbox.setVisibility(View.GONE);
            selectedNoteViewHolderList.remove(holder);
            Log.d("Check holder list", "Size is " + selectedNoteViewHolderList.size());
        }

        holder.noteContainer.setOnClickListener(view -> {
            if (isLongClickConsumed) { // The long click event is consumed means the user is selecting/unselecting notes
                MainActivity.isEventCheckbox = false;
                ((CheckBox) MainActivity.getLayoutDeleteOptions().findViewById(R.id.checkbox)).setChecked(false);
                holder.setSelected(!holder.isSelected());
                notifyItemChanged(position, holder.isSelected);
                MainActivity.setDeleteMessage();
//                Toast.makeText(view.getContext(), "Is selected: " + holder.isSelected(), Toast.LENGTH_SHORT).show();
            }
            else noteListener.onNoteClicked(listOfNotes.get(position), position);
        });

        holder.noteContainer.setOnLongClickListener(view -> {
            if (isLongClickConsumed) return true; // If the long click event was already consumed, omit it and do nothing
            isLongClickConsumed = true;
            noteListener.onNoteLongClicked(holder);
            notifyItemChanged(position, holder.isSelected);
            MainActivity.setDeleteMessage();
//            Toast.makeText(view.getContext(), "Long click", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return listOfNotes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public List<NoteViewHolder> getSelectedNoteViewHolderList() {
        return selectedNoteViewHolderList;
    }

    public void setSelectAll() {
        isSelectedAll = true;
        notifyDataSetChanged();
        MainActivity.setDeleteMessage();
    }

    public void unselectAll() {
        isSelectedAll = false;
        notifyDataSetChanged();
        selectedNoteViewHolderList.clear();
        MainActivity.setDeleteMessage();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteTitle, noteSubtitle, dateTimeText;
        LinearLayout noteContainer;
        FrameLayout noteFrame;
        RoundedImageView roundedImageView;
        ImageView checkbox;
        boolean isSelected = false;
        Note note;

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
        }

        public Note getNote() {
            return note;
        }

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);

            noteTitle = itemView.findViewById(R.id.titleText);
            noteSubtitle = itemView.findViewById(R.id.subtitleText);
            dateTimeText = itemView.findViewById(R.id.dateTimeText);
            noteContainer = itemView.findViewById(R.id.noteContainer);
            noteFrame = itemView.findViewById(R.id.noteFrameLayout);
            roundedImageView = itemView.findViewById(R.id.roundedNoteImage);
            checkbox = itemView.findViewById(R.id.checkbox);
        }

        void setNote(@NonNull final Note note) {
            this.note = note;
            noteTitle.setText(note.getTitle());
            if (note.getSubtitle().isEmpty()) noteSubtitle.setVisibility(View.GONE);
            else noteSubtitle.setText(note.getSubtitle());
            dateTimeText.setText(note.getDateTime());

            GradientDrawable gradientDrawable = (GradientDrawable) noteFrame.getBackground();
            if (note.getColor() != null)
                gradientDrawable.setColor(Color.parseColor(note.getColor()));
            else gradientDrawable.setColor(Color.parseColor("#333333"));

            if (note.getImagePath() != null) {
                roundedImageView.setImageBitmap(BitmapFactory.decodeFile(note.getImagePath()));
                roundedImageView.setVisibility(View.VISIBLE);
            } else
                roundedImageView.setVisibility(View.GONE);
        }
    }
}
