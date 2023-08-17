package com.example.noteapp.ui.adapters;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.noteapp.R;
import com.example.noteapp.data.entities.Note;
import com.example.noteapp.listeners.NoteListener;
import com.example.noteapp.ui.activities.MainActivity;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;

public class NotesAdapter extends ListAdapter<Note, NotesAdapter.NoteViewHolder> {
    private List<Note> originalNotes;
    private final NoteListener noteListener;
    private final List<NoteViewHolder> selectedNoteViewHolderList = new ArrayList<>();
    public boolean isSelectedAll = false;
    public boolean isLongClickConsumed = false;

    private static final DiffUtil.ItemCallback<Note> diffCallback = new DiffUtil.ItemCallback<Note>() {
        @Override
        public boolean areItemsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getSubtitle().equals(newItem.getSubtitle()) &&
                    oldItem.getColor().equals(newItem.getColor()) &&
                    oldItem.getImagePath().equals(newItem.getImagePath()) &&
                    oldItem.getDateTime().equals(newItem.getDateTime());
        }
    };

    public NotesAdapter(NoteListener noteListener) {
        super(diffCallback);
        this.noteListener = noteListener;
    }

    public void setSearch(@NonNull final String searchKeyword) {
        if (searchKeyword.trim().isEmpty()) submitList(originalNotes);
        else {
            List<Note> filteredNotes = new ArrayList<>();
            for (Note note : originalNotes) {
                if (note.getTitle().toLowerCase().contains(searchKeyword.toLowerCase()) ||
                        note.getSubtitle().toLowerCase().contains(searchKeyword.toLowerCase()) ||
                        note.getNoteContent().toLowerCase().contains(searchKeyword.toLowerCase()))
                    filteredNotes.add(note);
            }
            submitList(filteredNotes);
        }
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
        holder.setNote(getItem(position));

        if (MainActivity.isEventCheckbox || MainActivity.isCancelButtonClicked)
            holder.setSelected(isSelectedAll);

        if (holder.isSelected) {
            if (!isLongClickConsumed) holder.setSelected(false);
            holder.checkbox.setVisibility(isLongClickConsumed ? View.VISIBLE : View.GONE);
            if (isLongClickConsumed) {
                if (!selectedNoteViewHolderList.contains(holder))
                    selectedNoteViewHolderList.add(holder);
            } else
                selectedNoteViewHolderList.remove(holder);
        } else {
            holder.checkbox.setVisibility(View.GONE);
            selectedNoteViewHolderList.remove(holder);
        }

        holder.noteContainer.setOnClickListener(view -> {
            if (isLongClickConsumed) { // The long click event is consumed means the user is selecting/unselecting notes
                MainActivity.isEventCheckbox = false;
                ((CheckBox) MainActivity.getLayoutDeleteOptions().findViewById(R.id.checkbox)).setChecked(false);
                holder.setSelected(!holder.isSelected);
                notifyItemChanged(position, holder.isSelected);
                MainActivity.setDeleteMessage();
            } else noteListener.onNoteClicked(getItem(position), position);
        });

        holder.noteContainer.setOnLongClickListener(view -> {
            if (isLongClickConsumed) return true; // If the long click event was already consumed, omit it and do nothing
            isLongClickConsumed = true; // true -> the user want to select multiple notes
            noteListener.onNoteLongClicked(holder);
            notifyItemChanged(position, holder.isSelected);
            MainActivity.setDeleteMessage();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return getCurrentList().size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public void setOriginalNotes(List<Note> notes) {
        originalNotes = notes;
    }

    public List<NoteViewHolder> getSelectedNoteViewHolderList() {
        return selectedNoteViewHolderList;
    }

    public void setSelectAll() {
        isSelectedAll = true;
        selectedNoteViewHolderList.clear();
        notifyItemRangeChanged(0, getItemCount());
        MainActivity.setDeleteMessage();
    }

    public void unselectAll() {
        isSelectedAll = false;
        selectedNoteViewHolderList.clear();
        notifyItemRangeChanged(0, getItemCount());
        MainActivity.setDeleteMessage();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        private final TextView noteTitle, noteSubtitle, dateTimeText;
        private final LinearLayout noteContainer;
        private final FrameLayout noteFrame;
        private final RoundedImageView roundedImageView;
        private final ImageView checkbox;
        private boolean isSelected = false;
        private Note note;

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

        private void setNote(@NonNull final Note note) {
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
