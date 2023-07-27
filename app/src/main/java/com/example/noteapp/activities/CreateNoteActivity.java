package com.example.noteapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateNoteActivity extends AppCompatActivity {

    private EditText noteTitle, noteSubtitle, noteContent;
    private TextView dateTime;
    private View noteColor;
    private ImageView noteImage;
    private LinearLayout layoutURL;
    private TextView mainURL;
    private AlertDialog addURLDialog;
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

        dateTime.setText(
                new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault())
                        .format(new Date())
        );

        noteContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                noteContent.setAutoLinkMask(Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
                noteContent.setMovementMethod(LinkMovementMethod.getInstance());
                Linkify.addLinks(noteContent, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            fromMainActivityNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        }

        initAndShowColorPicker();
        setNoteColor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ImageView saveButton = findViewById(R.id.imgSave);
        saveButton.setOnClickListener(view -> saveNote());
    }

    private void setViewOrUpdateNote() {
        noteTitle.setText(fromMainActivityNote.getTitle());
        noteSubtitle.setText(fromMainActivityNote.getSubtitle());
        noteContent.setText(fromMainActivityNote.getNoteContent());
        dateTime.setText(fromMainActivityNote.getDateTime());
        if (fromMainActivityNote.getImagePath() != null && !fromMainActivityNote.getImagePath().trim().isEmpty()) {
            noteImage.setImageBitmap(BitmapFactory.decodeFile(fromMainActivityNote.getImagePath()));
            noteImage.setVisibility(View.VISIBLE);
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
            showToast("Note can't be empty");
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
    private void initAndShowColorPicker() {
        LinearLayout colorPicker = findViewById(R.id.color_picker);
        BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(colorPicker);
        colorPicker.findViewById(R.id.textColorPicker).setOnClickListener(view -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED)
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            else bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        });

        configColorPicker(colorPicker);

        if (fromMainActivityNote != null &&
                fromMainActivityNote.getColor() != null &&
                !fromMainActivityNote.getColor().trim().isEmpty()) {
            switch (fromMainActivityNote.getColor()) {
                case "#FDBE3B":
                    colorPicker.findViewById(R.id.imgColor2).performClick();
                    break;
                case "#FF4842":
                    colorPicker.findViewById(R.id.imgColor3).performClick();
                    break;
                case "#3A52FC":
                    colorPicker.findViewById(R.id.imgColor4).performClick();
                    break;
                case "000000":
                    colorPicker.findViewById(R.id.imgColor5).performClick();
                    break;
            }
        }

        colorPicker.findViewById(R.id.layoutAddImage).setOnClickListener(view -> {
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

        colorPicker.findViewById(R.id.layoutAddURL).setOnClickListener(view -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            showAddURLDialog();
        });
    }

    private void selectImage() {
        selectImageActivity.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
    }

    private void configColorPicker(LinearLayout layout) {
        final int[] colors = {
                ContextCompat.getColor(getApplicationContext(), R.color.defaultTextColor),
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
                    colorViews[j].setImageResource(j == index ? R.drawable.ic_done_24 : 0);
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
            addURLDialog.show();
        }
    }

    private void setNoteColor() {
        final GradientDrawable gradientDrawable = (GradientDrawable) noteColor.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
    }

    private void showToast(String message) {
        Toast.makeText(CreateNoteActivity.this, message, Toast.LENGTH_SHORT).show();
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
}