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
import android.text.style.CharacterStyle;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.noteapp.R;
import com.example.noteapp.database.NoteDatabase;
import com.example.noteapp.entities.Note;
import com.example.noteapp.helpers.StyledTextInfo;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Linkify.addLinks((Spannable) charSequence, Linkify.WEB_URLS);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                Linkify.addLinks(editable, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
            }
        });

        //This code is used to automatically increase the height of the EditText so that the
        //difference between its height and the height of its content is 30 lines
        final int maxVisibleLines = noteContent.getMaxLines();
        final int heightDiffLines = 30;
        noteContent.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int currentVisibleLines = noteContent.getLineCount();
            int totalLines = noteContent.getLineCount();

            // Adjust for any extra lines beyond the maximum visible lines
            if (totalLines > maxVisibleLines)
                currentVisibleLines = maxVisibleLines;

            // Compute the difference between the current number of visible lines and the total number of lines
            int lineDiff = totalLines - currentVisibleLines;

            // Check if the difference is within the desired range
            if (lineDiff >= 0 && lineDiff <= heightDiffLines) {
                // Calculate the new height of the EditText
                int lineHeight = noteContent.getLineHeight();
                int newHeight = lineHeight * (currentVisibleLines + heightDiffLines - lineDiff);
                noteContent.setHeight(newHeight);
            }
        });

        initAndShowNoteOptions();
        setNoteColor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (deleteNoteDialog != null) deleteNoteDialog.dismiss();
    }

    private void setViewOrUpdateNote() {
        noteTitle.setText(fromMainActivityNote.getTitle());
        noteSubtitle.setText(fromMainActivityNote.getSubtitle());
        noteContent.setText(fromMainActivityNote.getNoteContent());
        setStyles(fromMainActivityNote.getStyledSegments());
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

    private void setStyles(String input) {
        try {
            JSONArray jsonArray = new JSONArray(input);
            int size = jsonArray.length();
            for (int i = 0; i < size; i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int startIndex = jsonObject.getInt("spanStart");
                int endIndex = jsonObject.getInt("spanEnd");
                if (jsonObject.getBoolean("isBold"))
                    toggleStyle(noteContent.getText(), startIndex, endIndex, Typeface.BOLD);
                if (jsonObject.getBoolean("isItalic"))
                    toggleStyle(noteContent.getText(), startIndex, endIndex, Typeface.ITALIC);
                if (jsonObject.getBoolean("isUnderlined"))
                    toggleUnderlined(noteContent.getText(), startIndex, endIndex);
                if (jsonObject.getBoolean("isStrikethrough"))
                    toggleStrikethrough(noteContent.getText(), startIndex, endIndex);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveNote() {
        List<StyledTextInfo> styledContent = getStyledContent();
        String styled = new Gson().toJson(styledContent);

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
        note.setStyledSegments(styled);

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

    private List<StyledTextInfo> getStyledContent() {
        Spannable spannable = noteContent.getText();
        CharacterStyle[] characterStyles = spannable.getSpans(0, noteContent.getText().length(), CharacterStyle.class);
        List<StyledTextInfo> styledTextInfos = new ArrayList<>();
        for (CharacterStyle characterStyle : characterStyles) {
            int spanStart = spannable.getSpanStart(characterStyle);
            int spanEnd = spannable.getSpanEnd(characterStyle);

            boolean isBold = characterStyle instanceof StyleSpan && ((StyleSpan) characterStyle).getStyle() == Typeface.BOLD;
            boolean isItalic = characterStyle instanceof StyleSpan && ((StyleSpan) characterStyle).getStyle() == Typeface.ITALIC;
            boolean isUnderline = characterStyle instanceof UnderlineSpan;
            boolean isStrikethrough = characterStyle instanceof StrikethroughSpan;
            StyledTextInfo info = findObject(spanStart, spanEnd, styledTextInfos);
            if (info == null)
                styledTextInfos.add(new StyledTextInfo(spanStart, spanEnd, isBold, isItalic, isUnderline, isStrikethrough));
            else {
                info.setBold(info.isBold() || isBold);
                info.setItalic(info.isItalic() || isItalic);
                info.setUnderlined(info.isUnderlined() || isUnderline);
                info.setStrikethrough(info.isStrikethrough() || isStrikethrough);
            }
        }
        return styledTextInfos;
    }

    /**
     * @param spanStart the spanStart property of the tested {@code StyledTextInfo} object
     * @param spanEnd   the spanEnd property of the tested {@code StyledTextInfo} object
     * @param list      the list of {@code StyledTextInfo} objects
     * @return {@code null} if the StyleTextInfo object with the properties {@code spanStart} and {@code spanEnd}
     * doesn't exist in the {@code list}, return {@code nonnull} otherwise
     * @see com.example.noteapp.helpers.StyledTextInfo
     */
    @Nullable
    private static StyledTextInfo findObject(int spanStart, int spanEnd, @NonNull List<StyledTextInfo> list) {
        for (StyledTextInfo obj : list) {
            if (obj.getSpanStart() == spanStart && obj.getSpanEnd() == spanEnd)
                return obj;
        }
        return null;
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