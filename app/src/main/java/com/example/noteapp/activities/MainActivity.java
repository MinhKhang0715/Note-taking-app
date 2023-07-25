package com.example.noteapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.noteapp.R;
import com.example.noteapp.database.NoteDatabase;
import com.example.noteapp.entities.Note;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView addNoteBtn = findViewById(R.id.imgAddNoteMain);
        addNoteBtn.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), CreateNoteActivity.class));
        });
        getNotes();
    }

    private void getNotes() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            List<Note> notes = NoteDatabase.getInstance(getApplicationContext()).noteDAO().getAllNotes();
            handler.post(() -> Log.d("MY_NOTES", notes.toString()));
        });
    }
}