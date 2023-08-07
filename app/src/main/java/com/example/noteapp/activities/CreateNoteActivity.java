package com.example.noteapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.text.util.Linkify;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.noteapp.R;
import com.example.noteapp.database.NoteDatabase;
import com.example.noteapp.entities.Note;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateNoteActivity extends AppCompatActivity {

    private EditText noteTitle, noteSubtitle, noteContent;
    private TextView dateTime;
    private View noteColor;
    private ImageView noteImage;
    private LinearLayout layoutURL;
    private TextView mainURL;
    private AlertDialog addURLDialog, deleteNoteDialog;
    private Note fromMainActivityNote;
    private String selectedNoteColor;
    private final static int REQUEST_READ_MEDIA_IMAGES = 1;
    private String selectedImagePath = "";

    private final ActivityResultLauncher<Intent> selectImageActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent intent = result.getData();
                    if (intent != null) {
                        Uri selectedImageUri = intent.getData();
                        if (selectedImageUri != null) {
                            try (InputStream inputStream = getContentResolver().openInputStream(selectedImageUri)) {
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                noteImage.setImageBitmap(bitmap);
                                noteImage.setVisibility(View.VISIBLE);
                                findViewById(R.id.removeImage).setVisibility(View.VISIBLE);
                                selectedImagePath = getPathFromUri(selectedImageUri);
                            } catch (Exception e) {
                                showToast(e.getMessage());
                            }
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        noteTitle = findViewById(R.id.noteTitle);
        noteSubtitle = findViewById(R.id.noteSubtitle);
        noteContent = findViewById(R.id.noteContent);
        noteColor = findViewById(R.id.viewSubtitle);
        noteImage = findViewById(R.id.noteImage);
        dateTime = findViewById(R.id.textDateTime);
        layoutURL = findViewById(R.id.layoutURL);
        mainURL = findViewById(R.id.mainURL);
        selectedNoteColor = "#333333"; // default color
        findViewById(R.id.imgBack).setOnClickListener(view -> finish());
        ImageView saveButton = findViewById(R.id.imgSave);
        saveButton.setOnClickListener(view -> saveNote());

        noteContent.setAutoLinkMask(Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
        noteContent.setMovementMethod(LinkMovementMethod.getInstance());

        Intent fromMainActivityIntent = getIntent();

        if (fromMainActivityIntent.getBooleanExtra("isViewOrUpdate", false)) {
            fromMainActivityNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        } else if (fromMainActivityIntent.getBooleanExtra("isQuickAction", false)) {
            String type = fromMainActivityIntent.getStringExtra("quickActionType");
            if (type != null) {
                switch (type) {
                    case "image": {
                        selectedImagePath = fromMainActivityIntent.getStringExtra("imgPath");
                        noteImage.setImageBitmap(BitmapFactory.decodeFile(selectedImagePath));
                        noteImage.setVisibility(View.VISIBLE);
                        findViewById(R.id.removeImage).setVisibility(View.VISIBLE);
                        break;
                    }
                    case "url": {
                        String url = fromMainActivityIntent.getStringExtra("URL");
                        layoutURL.setVisibility(View.VISIBLE);
                        mainURL.setText(url);
                        break;
                    }
                }
            }
            dateTime.setText(
                    new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault())
                            .format(new Date())
            );
        } else {
            dateTime.setText(
                    new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault())
                            .format(new Date())
            );
        }

        findViewById(R.id.removeImage).setOnClickListener(view -> {
            noteImage.setImageBitmap(null);
            noteImage.setVisibility(View.GONE);
            findViewById(R.id.removeImage).setVisibility(View.GONE);
            selectedImagePath = "";
        });

        findViewById(R.id.removeURL).setOnClickListener(view -> {
            mainURL.setText(null);
            layoutURL.setVisibility(View.GONE);
        });

        noteContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Linkify.addLinks((Spannable) charSequence, Linkify.WEB_URLS);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                Linkify.addLinks(editable, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
            }
        });

        initAndShowNoteOptions();
        setNoteColor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (deleteNoteDialog != null ) deleteNoteDialog.dismiss();
    }

    private void setViewOrUpdateNote() {
        noteTitle.setText(fromMainActivityNote.getTitle());
        noteSubtitle.setText(fromMainActivityNote.getSubtitle());
        noteContent.setText(fromMainActivityNote.getNoteContent());
        dateTime.setText(fromMainActivityNote.getDateTime());
        if (fromMainActivityNote.getImagePath() != null && !fromMainActivityNote.getImagePath().trim().isEmpty()) {
            noteImage.setImageBitmap(BitmapFactory.decodeFile(fromMainActivityNote.getImagePath()));
            noteImage.setVisibility(View.VISIBLE);
            findViewById(R.id.removeImage).setVisibility(View.VISIBLE);
            selectedImagePath = fromMainActivityNote.getImagePath();
        }
        if (fromMainActivityNote.getWebLink() != null && !fromMainActivityNote.getWebLink().trim().isEmpty()) {
            mainURL.setText(fromMainActivityNote.getWebLink());
            layoutURL.setVisibility(View.VISIBLE);
        }
    }

    private void saveNote() {
        if (noteTitle.getText().toString().trim().isEmpty()) {
            showToast("Note title can't be empty");
            return;
        } else if (noteSubtitle.getText().toString().trim().isEmpty() && noteContent.getText().toString().trim().isEmpty()) {
            showToast("Note subtitle can't be empty");
            return;
        }

        final Note note = new Note();
        note.setTitle(noteTitle.getText().toString().trim());
        note.setSubtitle(noteSubtitle.getText().toString().trim());
        note.setNoteContent(noteContent.getText().toString());
        note.setDateTime(dateTime.getText().toString());
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath);

        if (layoutURL.getVisibility() == View.VISIBLE)
            note.setWebLink(mainURL.getText().toString());

        if (fromMainActivityNote != null)
            note.setId(fromMainActivityNote.getId());

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

    //    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void initAndShowNoteOptions() {
        LinearLayout noteOptionsLayout = findViewById(R.id.noteOptions);
        BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(noteOptionsLayout);
        noteOptionsLayout.findViewById(R.id.textColorPicker).setOnClickListener(view -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED)
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            else bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        });

        configLayoutNoteOptions(noteOptionsLayout);

        if (fromMainActivityNote != null &&
                fromMainActivityNote.getColor() != null &&
                !fromMainActivityNote.getColor().trim().isEmpty()) {
            switch (fromMainActivityNote.getColor()) {
                case "#FDBE3B":
                    noteOptionsLayout.findViewById(R.id.imgColor2).performClick();
                    break;
                case "#FF4842":
                    noteOptionsLayout.findViewById(R.id.imgColor3).performClick();
                    break;
                case "#3A52FC":
                    noteOptionsLayout.findViewById(R.id.imgColor4).performClick();
                    break;
                case "000000":
                    noteOptionsLayout.findViewById(R.id.imgColor5).performClick();
                    break;
            }
        }

        noteOptionsLayout.findViewById(R.id.layoutAddImage).setOnClickListener(view -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            if (ContextCompat.checkSelfPermission(
                    getApplicationContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        CreateNoteActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_READ_MEDIA_IMAGES
                );
            } else selectImage();
        });

        noteOptionsLayout.findViewById(R.id.layoutAddURL).setOnClickListener(view -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            showAddURLDialog();
        });

        if (fromMainActivityNote != null) {
            noteOptionsLayout.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            noteOptionsLayout.findViewById(R.id.layoutDeleteNote).setOnClickListener(view -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showDeleteNoteDialog();
            });
        }
    }

    private void selectImage() {
        selectImageActivity.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
    }

    private void configLayoutNoteOptions(@NonNull LinearLayout layout) {
        final int[] colors = {
                ContextCompat.getColor(getApplicationContext(), R.color.defaultBackgroundColor),
                ContextCompat.getColor(getApplicationContext(), R.color.note2),
                ContextCompat.getColor(getApplicationContext(), R.color.note3),
                ContextCompat.getColor(getApplicationContext(), R.color.note4),
                ContextCompat.getColor(getApplicationContext(), R.color.note5)
        };

        final ImageView[] colorViews = {
                layout.findViewById(R.id.imgColor1),
                layout.findViewById(R.id.imgColor2),
                layout.findViewById(R.id.imgColor3),
                layout.findViewById(R.id.imgColor4),
                layout.findViewById(R.id.imgColor5)
        };

        for (int i = 0; i < colorViews.length; i++) {
            final int index = i;
            colorViews[i].setOnClickListener(view -> {
                selectedNoteColor = String.format("#%06X", (0xFFFFFF & colors[index]));
                setNoteColor();
                for (int j = 0; j < colorViews.length; j++) {
                    colorViews[j].setImageResource(j == index ? R.drawable.ic_check_24 : 0);
                }
            });
        }
    }

    private void showAddURLDialog() {
        if (addURLDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
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
                    mainURL.setText(url);
                    layoutURL.setVisibility(View.VISIBLE);
                    addURLDialog.dismiss();
                }
            });
            addURLDialogView.findViewById(R.id.btnCancel).setOnClickListener(view -> addURLDialog.dismiss());
        }
        addURLDialog.show();
    }

    private void showDeleteNoteDialog() {
        if (deleteNoteDialog == null) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(CreateNoteActivity.this);
            View deleteNoteDialogView = LayoutInflater.from(this).inflate(
                    R.layout.delete_note_dialog,
                    findViewById(R.id.deleteNoteDialog)
            );
            dialogBuilder.setView(deleteNoteDialogView);
            deleteNoteDialog = dialogBuilder.create();

            if (deleteNoteDialog.getWindow() != null)
                deleteNoteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));

            deleteNoteDialogView.findViewById(R.id.btnCancel).setOnClickListener(view -> deleteNoteDialog.dismiss());
            deleteNoteDialogView.findViewById(R.id.btnDelete).setOnClickListener(view -> {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Objects.requireNonNull(Looper.myLooper()));
                executor.execute(() -> {
                    NoteDatabase.getInstance(getApplicationContext()).noteDAO().deleteNote(fromMainActivityNote);
                    handler.post(() -> {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra("isNoteDeleted", true);
                        setResult(RESULT_OK, intent);
                        finish();
                    });
                });
            });
        }
        deleteNoteDialog.show();
    }

    private void setNoteColor() {
        final GradientDrawable gradientDrawable = (GradientDrawable) noteColor.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
    }

    private void showToast(String message) {
        Toast.makeText(CreateNoteActivity.this, message, Toast.LENGTH_SHORT).show();
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

    private void toggleStyle(@NonNull Spannable spannable, int selectionStart, int selectionEnd, int style) {
        StyleSpanRemover styleSpanRemover = new StyleSpanRemover();
        boolean isStyleApplied = isStyleAlreadyApplied(spannable, selectionStart, selectionEnd, style);

        if (isStyleApplied) {
            styleSpanRemover.removeStyle(spannable, selectionStart, selectionEnd, style);
        } else {
            spannable.setSpan(new StyleSpan(style), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private boolean isStyleAlreadyApplied(@NonNull Spannable spannable, int selectionStart, int selectionEnd, int style) {
        StyleSpan[] styleSpans = spannable.getSpans(selectionStart, selectionEnd, StyleSpan.class);

        for (StyleSpan styleSpan : styleSpans) {
            if (styleSpan.getStyle() == style) {
                return true;
            }
        }

        return false;
    }

    private void toggleUnderlined(@NonNull Spannable spannable, int selectionStart, int selectionEnd) {
        StyleSpanRemover styleSpanRemover = new StyleSpanRemover();
        boolean isUnderlinedAlreadyApplied = spannable.getSpans(selectionStart, selectionEnd, UnderlineSpan.class).length > 0;

        if (isUnderlinedAlreadyApplied)
            styleSpanRemover.removeOne(spannable, selectionStart, selectionEnd, UnderlineSpan.class);
        else
            spannable.setSpan(new UnderlineSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void toggleStrikethrough(@NonNull Spannable spannable, int selectionStart, int selectionEnd) {
        StyleSpanRemover styleSpanRemover = new StyleSpanRemover();
        boolean isStrikethroughAlreadyApplied = spannable.getSpans(selectionStart, selectionEnd, StrikethroughSpan.class).length > 0;

        if (isStrikethroughAlreadyApplied)
            styleSpanRemover.removeOne(spannable, selectionStart, selectionEnd, StrikethroughSpan.class);
        else
            spannable.setSpan(new StrikethroughSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_MEDIA_IMAGES && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                selectImage();
            else showToast("Permission denied");
        }
    }

    public void onBoldTextIconClicked(View view) {
        int selectionStart = noteContent.getSelectionStart();
        int selectionEnd = noteContent.getSelectionEnd();
        Spannable spannable = noteContent.getText();
        toggleStyle(spannable, selectionStart, selectionEnd, Typeface.BOLD);
    }

    public void onItalicTextIconClicked(View view) {
        int selectionStart = noteContent.getSelectionStart();
        int selectionEnd = noteContent.getSelectionEnd();
        Spannable spannable = noteContent.getText();
        toggleStyle(spannable, selectionStart, selectionEnd, Typeface.ITALIC);
    }

    public void onUnderlinedTextIconClicked(View view) {
        int selectionStart = noteContent.getSelectionStart();
        int selectionEnd = noteContent.getSelectionEnd();
        Spannable spannable = noteContent.getText();
        toggleUnderlined(spannable, selectionStart, selectionEnd);
    }

    public void onStrikethroughIconClicked(View view) {
        int selectionStart = noteContent.getSelectionStart();
        int selectionEnd = noteContent.getSelectionEnd();
        Spannable spannable = noteContent.getText();
        toggleStrikethrough(spannable, selectionStart, selectionEnd);
    }
}