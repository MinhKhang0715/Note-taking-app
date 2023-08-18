package com.example.noteapp.ui.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.LinearLayout;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.noteapp.R;
import com.example.noteapp.data.entities.Note;
import com.example.noteapp.helpers.Utils;
import com.example.noteapp.listeners.NoteListener;
import com.example.noteapp.ui.adapters.NotesAdapter;
import com.example.noteapp.ui.viewmodels.NoteViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements NoteListener {
    private final static int REQUEST_READ_IMAGES_CODE = 4;
    private static NotesAdapter notesAdapter;
    private AlertDialog addURLDialog;
    private static ConstraintLayout selectInformation;
    private LinearLayout quickActionLayout;
    private ImageView mainAddNoteBtn;
    private int noteClickedPosition;
    private NoteViewModel noteViewModel;
    private static int numberOfSelectedNotes;
    private final Utils.Debounce debounce = new Utils.Debounce(2000);

    private ActivityResultLauncher<Intent> createNoteActivity;
    private ActivityResultLauncher<Intent> selectImageActivity;
    private ActivityResultLauncher<Intent> viewOrUpdateNoteActivity;

    /**
     * When the user click on the checkbox, it means that they want to select/unselect all notes<br>
     * If this flag is set to false it means the user is manually selecting notes
     */
    public static boolean isEventCheckboxSelectAll = false;

    public static boolean isBackButtonClicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainAddNoteBtn = findViewById(R.id.imgAddNoteMain);

        RecyclerView notesView = findViewById(R.id.noteRecyclerView);
        notesView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        );
        notesAdapter = new NotesAdapter(this);
        notesView.setAdapter(notesAdapter);

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        noteViewModel.getAllNotes().observe(this, notes -> {
            notesAdapter.submitList(notes);
            notesAdapter.setOriginalNotes(notes);
        });

        registerActivities();
        quickActionLayout = findViewById(R.id.quickActions);

        mainAddNoteBtn.setOnClickListener(view -> createNoteActivity.launch(new Intent(this, CreateNoteActivity.class)));
        quickActionLayout.findViewById(R.id.imgAddNote).setOnClickListener(view -> createNoteActivity.launch(new Intent(this, CreateNoteActivity.class)));

        quickActionLayout.findViewById(R.id.imgAddImage).setOnClickListener(view -> {
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

        quickActionLayout.findViewById(R.id.imgAddWebLink).setOnClickListener(view -> showAddURLDialog());

        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (notesAdapter.getCurrentList().size() != 0)
                    notesAdapter.setSearch(editable.toString());
            }
        });

        selectInformation = findViewById(R.id.selectInformation);
        selectInformation.findViewById(R.id.btnDelete).setOnClickListener(view -> {
            if (numberOfSelectedNotes == 0) {
                if (debounce.debounce()) return;
                showToast("No note selected");
                return;
            }
            showDeleteNotesDialog();
        });
        CheckBox checkBox = selectInformation.findViewById(R.id.checkboxSelectAll);
        checkBox.setOnClickListener(view -> {
            isEventCheckboxSelectAll = true;
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
    public void onBackPressed() {
        if (notesAdapter.isSelectingNotes) {
            isBackButtonClicked = true;
            isEventCheckboxSelectAll = false;
            notesAdapter.isSelectingNotes = false;
            notesAdapter.unselectAll();
            ((CheckBox) selectInformation.findViewById(R.id.checkboxSelectAll)).setChecked(false);
            selectInformation.setVisibility(View.GONE);
            quickActionLayout.setVisibility(View.VISIBLE);
            mainAddNoteBtn.setVisibility(View.VISIBLE);
        } else super.onBackPressed();
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        if (isBackButtonClicked) isBackButtonClicked = false;
        noteClickedPosition = position;
        viewOrUpdateNoteActivity.launch(
                new Intent(getApplicationContext(), CreateNoteActivity.class)
                        .putExtra("isViewOrUpdate", true)
                        .putExtra("note", note)
        );
    }

    @Override
    public void onNoteLongClicked(NotesAdapter.NoteViewHolder noteViewHolder) {
        if (isBackButtonClicked) isBackButtonClicked = false;
        noteViewHolder.setSelected(true);

        selectInformation.setVisibility(View.VISIBLE);
        quickActionLayout.setVisibility(View.GONE);
        mainAddNoteBtn.setVisibility(View.GONE);
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

    public static ConstraintLayout getSelectInformation() {
        return selectInformation;
    }

    /**
     * Using an executor to run the code after the adapter has updated its number of selected notes
     * (via {@code selectedNoteViewHolderList})<br>
     * The <b>delay time</b> is set to 20 milliseconds to ensure the code will run <i>after</i>
     * the number of selected notes has been updated.
     */
    public static void setDeleteMessage() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        Context applicationContext = selectInformation.getContext().getApplicationContext();
        UpdateSelectedNotesTask updateSelectedNotesTask = new UpdateSelectedNotesTask(applicationContext);
        executorService.execute(() -> handler.postDelayed(updateSelectedNotesTask, 20));
    }

    private void registerActivities() {
        createNoteActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Note noteToSave = (Note) data.getSerializableExtra("note");
                            noteViewModel.insert(noteToSave);
                            showToast("Created note");
                        }
                    }
                }
        );

        selectImageActivity = registerForActivityResult(
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
                                        .putExtra("imgPath", Utils.getPathFromUri(selectedImageUri, getContentResolver())));
                            }
                        }
                    }
                }
        );

        viewOrUpdateNoteActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent resultIntent = result.getData();
                        if (resultIntent != null) {
                            if (resultIntent.getBooleanExtra("isNoteDeleted", false)) {
                                Note noteToDelete = (Note) resultIntent.getSerializableExtra("noteToDelete");
                                noteViewModel.delete(noteToDelete);
                                notesAdapter.notifyItemRemoved(noteClickedPosition);
                                showToast("Deleted note");
                            } else if (resultIntent.getBooleanExtra("isUpdateNote", false)) {
                                Note noteToUpdate = (Note) resultIntent.getSerializableExtra("noteToUpdate");
                                noteViewModel.insert(noteToUpdate);
                                notesAdapter.notifyItemChanged(noteClickedPosition);
                                showToast("Updated note");
                            }
                        }
                    }
                }
        );
    }

    private void showAddURLDialog() {
        Utils.Debounce debounce = new Utils.Debounce(2000);
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
                if (url.isEmpty()) {
                    if (debounce.debounce()) return;
                    showToast("Enter your URL");
                }
                else if (!Patterns.WEB_URL.matcher(url).matches()) {
                    if (debounce.debounce()) return;
                    showToast("Enter a valid URL");
                }
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
            if (numberOfSelectedNotes == notesAdapter.getItemCount())
                noteViewModel.deleteAllNotes();
            else {
                List<Integer> noteIds = new ArrayList<>();
                List<NotesAdapter.NoteViewHolder> noteViewHolderList = notesAdapter.getSelectedNoteViewHolderList();
                for (NotesAdapter.NoteViewHolder noteViewHolder : noteViewHolderList)
                    noteIds.add(noteViewHolder.getNote().getId()); // collect all selected note's ids to remove
                noteViewModel.deleteByIds(noteIds);
            }
            numberOfSelectedNotes = 0;
            notesAdapter.isSelectingNotes = false;
            if (notesAdapter.getSelectedNoteViewHolderList().size() > 0)
                notesAdapter.getSelectedNoteViewHolderList().clear();
            ((CheckBox) selectInformation.findViewById(R.id.checkboxSelectAll)).setChecked(false);
            isEventCheckboxSelectAll = false;
            deleteNoteDialog.dismiss();
            selectInformation.setVisibility(View.GONE);
            quickActionLayout.setVisibility(View.VISIBLE);
            mainAddNoteBtn.setVisibility(View.VISIBLE);
        });
        deleteNoteDialogView.findViewById(R.id.btnCancel).setOnClickListener(view -> deleteNoteDialog.dismiss());
        deleteNoteDialog.show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static final class UpdateSelectedNotesTask implements Runnable {
        private final Context context;

        private UpdateSelectedNotesTask(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            int numberOfSelectedNotes = notesAdapter.getSelectedNoteViewHolderList().size();
            String message = context.getString(R.string.selected_notes, numberOfSelectedNotes);
            ((TextView) selectInformation.findViewById(R.id.numberOfSelectedNotes)).setText(message);
        }
    }
}