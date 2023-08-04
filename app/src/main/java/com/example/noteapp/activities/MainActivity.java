package com.example.noteapp.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
    private final static int ACTION_ADD_NOTE = 1;
    private final static int ACTION_VIEW_NOTES = 2;
    private final static int ACTION_UPDATE_NOTE = 3;
    private final static int REQUEST_READ_IMAGES_CODE = 4;
    private RecyclerView notesView;
    private List<Note> noteList;
    private static NotesAdapter notesAdapter;
    private AlertDialog addURLDialog;
    private static ConstraintLayout layoutDeleteOptions;
    private int noteClickedPosition;

    /**
     * When the user click on the checkbox, it means that they want to select/unselect all notes<br>
     * If this flag is set to false it means the user is manually selecting notes
     */
    public static boolean isEventCheckbox = false;

    /**
     * The cancel button in the <code>delete_options.xml</code> layout<br>
     * When the user click on it, unselect all the notes and hide the layout
     */
    public static boolean isCancelButtonClicked = false;
    private static int numberOfSelectedNotes;

    private final ActivityResultLauncher<Intent> createNoteActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK)
                    getNotes(ACTION_ADD_NOTE, false);
            }
    );

    private final ActivityResultLauncher<Intent> selectImageActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent intent = result.getData();
                    if (intent != null) {
                        Uri selectedImageUri = intent.getData();
                        if (selectedImageUri != null) {
                            createNoteActivity.launch(new Intent(MainActivity.this, CreateNoteActivity.class)
                                    .putExtra("isQuickAction", true)
                                    .putExtra("quickActionType", "image")
                                    .putExtra("imgPath", getPathFromUri(selectedImageUri)));
                        }
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> viewOrUpdateNoteActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent resultIntent = result.getData();
                    if (resultIntent != null)
                        getNotes(ACTION_UPDATE_NOTE, resultIntent.getBooleanExtra("isNoteDeleted", false));
                }
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
        getNotes(ACTION_VIEW_NOTES, false);

        findViewById(R.id.imgAddNote).setOnClickListener(view -> createNoteActivity.launch(new Intent(this, CreateNoteActivity.class)));

        findViewById(R.id.imgAddImage).setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(
                    getApplicationContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_READ_IMAGES_CODE
                );
            } else {
                selectImageActivity.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
            }
        });

        findViewById(R.id.imgAddWebLink).setOnClickListener(view -> showAddURLDialog());

        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (noteList.size() != 0)
                    notesAdapter.setSearch(editable.toString());
            }
        });

        layoutDeleteOptions = findViewById(R.id.layoutDeleteOptions);
        layoutDeleteOptions.findViewById(R.id.btnCancelDelete).setOnClickListener(view -> {
            isCancelButtonClicked = true;
            isEventCheckbox = false;
            notesAdapter.unselectAll();
            ((CheckBox) layoutDeleteOptions.findViewById(R.id.checkbox)).setChecked(false);
            notesAdapter.isLongClickConsumed = false;
            layoutDeleteOptions.setVisibility(View.GONE);
        });
        layoutDeleteOptions.findViewById(R.id.btnDelete).setOnClickListener(view -> {
            if (numberOfSelectedNotes == 0) return;
            showDeleteNotesDialog();
        });
        CheckBox checkBox = layoutDeleteOptions.findViewById(R.id.checkbox);
        checkBox.setOnClickListener(view -> {
            isEventCheckbox = true;
            if (checkBox.isChecked()) {
                notesAdapter.setSelectAll();
                checkBox.setChecked(true);
            } else {
                notesAdapter.unselectAll();
                checkBox.setChecked(false);
            }
        });
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

    @Override
    public void onNoteLongClicked(NotesAdapter.NoteViewHolder noteViewHolder) {
        if (isCancelButtonClicked) isCancelButtonClicked = false;
        noteViewHolder.setSelected(true);

        layoutDeleteOptions.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_IMAGES_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                selectImageActivity.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
            else showToast("Permission denied");
        }
    }

    public static ConstraintLayout getLayoutDeleteOptions() {
        return layoutDeleteOptions;
    }

    /**
     * Using an executor to run the code after the adapter updated its number of selected notes
     * (via selectedNoteViewHolderList)<br>
     * The <b>delay time</b> is set to make sure the code will run <i>after</i>
     * the number of selected notes is updated.
     */
    @SuppressLint("SetTextI18n")
    public static void setDeleteMessage() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executorService.execute(() -> handler.postDelayed(() -> {
            numberOfSelectedNotes = notesAdapter.getSelectedNoteViewHolderList().size();
            ((TextView) layoutDeleteOptions.findViewById(R.id.deleteMessage)).setText("You will delete " +
                    (numberOfSelectedNotes > 1 ?
                            (numberOfSelectedNotes + " notes") :
                            (numberOfSelectedNotes + " note")
                    )
            );
        }, 20));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void getNotes(int actionCode, boolean isNoteDeleted) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            List<Note> noteListFromDB = NoteDatabase.getInstance(getApplicationContext()).noteDAO().getAllNotes();
            handler.post(() -> {
                if (actionCode == ACTION_VIEW_NOTES) {
                    noteList.addAll(noteListFromDB);
                    notesAdapter.notifyDataSetChanged();
                } else if (actionCode == ACTION_ADD_NOTE) {
                    noteList.add(0, noteListFromDB.get(0));
                    notesAdapter.notifyItemInserted(0);
                    notesView.smoothScrollToPosition(0);
                } else if (actionCode == ACTION_UPDATE_NOTE) {
                    noteList.remove(noteClickedPosition);
                    if (isNoteDeleted) {
                        notesAdapter.notifyItemRemoved(noteClickedPosition);
                        showToast("Note deleted");
                    } else {
                        noteList.add(noteClickedPosition, noteListFromDB.get(noteClickedPosition));
                        notesAdapter.notifyItemChanged(noteClickedPosition);
                        showToast("Note updated");
                    }
                }
            });
        });
    }

    private String getPathFromUri(Uri imgUri) {
        String path;
        Cursor cursor = getContentResolver().query(imgUri, null, null, null, null);
        if (cursor == null)
            return imgUri.getPath();
        else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            path = cursor.getString(index);
            cursor.close();
            return path;
        }
    }

    private void showAddURLDialog() {
        if (addURLDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View addURLDialogView = LayoutInflater.from(this).inflate(
                    R.layout.add_url_dialog,
                    findViewById(R.id.addURLDialog)
            );
            builder.setView(addURLDialogView);
            addURLDialog = builder.create();

            if (addURLDialog.getWindow() != null)
                addURLDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));

            final EditText inputURL = addURLDialogView.findViewById(R.id.inputURL);
            inputURL.requestFocus();

            addURLDialogView.findViewById(R.id.btnAdd).setOnClickListener(view -> {
                String url = inputURL.getText().toString().trim();
                if (url.isEmpty())
                    showToast("Enter your URL");
                else if (!Patterns.WEB_URL.matcher(url).matches())
                    showToast("Enter a valid URL");
                else {
                    addURLDialog.dismiss();
                    createNoteActivity.launch(new Intent(MainActivity.this, CreateNoteActivity.class)
                            .putExtra("isQuickAction", true)
                            .putExtra("quickActionType", "url")
                            .putExtra("URL", url));
                }
            });
            addURLDialogView.findViewById(R.id.btnCancel).setOnClickListener(view -> addURLDialog.dismiss());
        }
        addURLDialog.show();
    }

    @SuppressLint("SetTextI18n")
    private void showDeleteNotesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View deleteNoteDialogView = LayoutInflater.from(this).inflate(
                R.layout.delete_note_dialog,
                findViewById(R.id.deleteNoteDialog)
        );
        builder.setView(deleteNoteDialogView);
        AlertDialog deleteNoteDialog = builder.create();
        if (deleteNoteDialog.getWindow() != null)
            deleteNoteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        ((TextView) deleteNoteDialogView.findViewById(R.id.message)).setText("Are you sure you want to delete " +
                (numberOfSelectedNotes > 1 ?
                        (numberOfSelectedNotes + " notes") :
                        (numberOfSelectedNotes + " note")
                ));
        deleteNoteDialogView.findViewById(R.id.btnDelete).setOnClickListener(view -> {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executorService.execute(() -> {
                if (numberOfSelectedNotes == notesAdapter.getItemCount()) {
                    NoteDatabase.getInstance(getApplicationContext()).noteDAO().deleteAllNotes();
                    noteList.clear();
                }
                else {
                    List<Integer> noteIds = new ArrayList<>();
                    List<NotesAdapter.NoteViewHolder> noteViewHolderList = notesAdapter.getSelectedNoteViewHolderList();
                    for (NotesAdapter.NoteViewHolder noteViewHolder : noteViewHolderList) {
                        noteIds.add(noteViewHolder.getNote().getId()); // collect all selected note's ids to remove
                        noteList.remove(noteViewHolder.getNote()); // make it in sync with NoteAdapter.listOfNotes
                    }
                    NoteDatabase.getInstance(getApplicationContext()).noteDAO().deleteNotesByIdList(noteIds);
                }
                handler.post(() -> {
                    notesAdapter.removeSelectedNotes();
                    notesAdapter.selectedNoteViewHolderList.clear();
                    numberOfSelectedNotes = 0;
                    notesAdapter.isLongClickConsumed = false;
                    layoutDeleteOptions.setVisibility(View.GONE);
                    deleteNoteDialog.dismiss();
                });
            });
        });
        deleteNoteDialogView.findViewById(R.id.btnCancel).setOnClickListener(view -> deleteNoteDialog.dismiss());
        deleteNoteDialog.show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}