package com.example.noteapp.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.noteapp.R;
import com.example.noteapp.adapters.NotesAdapter;
import com.example.noteapp.database.NoteDatabase;
import com.example.noteapp.entities.Note;
import com.example.noteapp.listeners.NoteListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements NoteListener {
    //    private final static int REQUEST_CODE_ADD_NOTE = 1;
    private RecyclerView notesView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;
    private int noteClickedPosition;

    private final ActivityResultLauncher<Intent> createNoteActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK)
                    getNotes();
            }
    );

    private final ActivityResultLauncher<Intent> viewOrUpdateNoteActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());
                executor.execute(() -> {
                    List<Note> notes = NoteDatabase.getInstance(getApplicationContext()).noteDAO().getAllNotes();
                    handler.post(() -> {
                        noteList.remove(noteClickedPosition);
                        noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
                        notesAdapter.notifyItemChanged(noteClickedPosition);
                    });
                });
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView addNoteBtn = findViewById(R.id.imgAddNoteMain);

        addNoteBtn.setOnClickListener(view -> createNoteActivity.launch(new Intent(this, CreateNoteActivity.class)));
        notesView = findViewById(R.id.noteRecyclerView);
        notesView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        );
        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, this);
        notesView.setAdapter(notesAdapter);
        getNotes();
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        viewOrUpdateNoteActivity.launch(
                new Intent(getApplicationContext(), CreateNoteActivity.class)
                        .putExtra("isViewOrUpdate", true)
                        .putExtra("note", note)
        );
    }

    @SuppressLint("NotifyDataSetChanged")
    private void getNotes() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            List<Note> notes = NoteDatabase.getInstance(getApplicationContext()).noteDAO().getAllNotes();
            handler.post(() -> {
                if (noteList.size() == 0) {
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                } else {
                    noteList.add(0, notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                }
                notesView.smoothScrollToPosition(0);
                Log.d("MY_NOTES", notes.toString());
            });
        });
    }
}