package com.example.noteapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.noteapp.R;
import com.example.noteapp.database.NoteDatabase;
import com.example.noteapp.entities.Note;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateNoteActivity extends AppCompatActivity {

    private EditText noteTitle, noteSubtitle, noteContent;
    private TextView dateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        noteTitle = findViewById(R.id.noteTitle);
        noteSubtitle = findViewById(R.id.noteSubtitle);
        noteContent = findViewById(R.id.noteContent);

        dateTime = findViewById(R.id.textDateTime);
        dateTime.setText(
                new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault())
                        .format(new Date())
        );
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}