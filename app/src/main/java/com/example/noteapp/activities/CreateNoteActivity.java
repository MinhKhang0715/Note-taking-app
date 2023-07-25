package com.example.noteapp.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.noteapp.R;
import com.example.noteapp.database.NoteDatabase;
import com.example.noteapp.entities.Note;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateNoteActivity extends AppCompatActivity {

    private EditText noteTitle, noteSubtitle, noteContent;
    private TextView dateTime;
    private View noteColor;
    private String selectedNoteColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        noteTitle = findViewById(R.id.noteTitle);
        noteSubtitle = findViewById(R.id.noteSubtitle);
        noteContent = findViewById(R.id.noteContent);
        noteColor = findViewById(R.id.viewSubtitle);
        dateTime = findViewById(R.id.textDateTime);

        dateTime.setText(
                new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault())
                        .format(new Date())
        );

        selectedNoteColor = "#333333"; // default color

        showColorPicker();
        setNoteColor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ImageView saveButton = findViewById(R.id.imgSave);
        saveButton.setOnClickListener(view -> saveNote());
    }

    private void saveNote() {
        if (noteTitle.getText().toString().trim().isEmpty()) {
            showToast("Note title can't be empty");
            return;
        } else if (noteSubtitle.getText().toString().trim().isEmpty() && noteContent.getText().toString().trim().isEmpty()) {
            showToast("Note can't be empty");
            return;
        }

        final Note note = new Note();
        note.setTitle(noteTitle.getText().toString().trim());
        note.setSubtitle(noteSubtitle.getText().toString().trim());
        note.setNoteContent(noteContent.getText().toString());
        note.setDateTime(dateTime.getText().toString());
        note.setColor(selectedNoteColor);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            NoteDatabase.getInstance(getApplicationContext()).noteDAO().insertNote(note);
            handler.post(() -> {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            });
        });
    }

    private void showColorPicker() {
        LinearLayout colorPicker = findViewById(R.id.color_picker);
        BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(colorPicker);
        colorPicker.findViewById(R.id.textColorPicker).setOnClickListener(view -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED)
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            else bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        });

        final int[] colors = {
                ContextCompat.getColor(getApplicationContext(), R.color.defaultTextColor),
                ContextCompat.getColor(getApplicationContext(), R.color.note2),
                ContextCompat.getColor(getApplicationContext(), R.color.note3),
                ContextCompat.getColor(getApplicationContext(), R.color.note4),
                ContextCompat.getColor(getApplicationContext(), R.color.note5)
        };

        final ImageView[] colorViews = {
                colorPicker.findViewById(R.id.imgColor1),
                colorPicker.findViewById(R.id.imgColor2),
                colorPicker.findViewById(R.id.imgColor3),
                colorPicker.findViewById(R.id.imgColor4),
                colorPicker.findViewById(R.id.imgColor5)
        };

        for (int i = 0; i < colorViews.length; i++) {
            final int index = i;
            colorViews[i].setOnClickListener(view -> {
                selectedNoteColor = String.format("#%06X", (0xFFFFFF & colors[index]));
                setNoteColor();
                for (int j = 0; j < colorViews.length; j++) {
                    colorViews[j].setImageResource(j == index ? R.drawable.ic_done_24 : 0);
                }
            });
        }
    }

    private void setNoteColor() {
        final GradientDrawable gradientDrawable = (GradientDrawable) noteColor.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}