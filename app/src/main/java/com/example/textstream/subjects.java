package com.example.textstream;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;

import com.example.textstream.security.RC4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class subjects extends AppCompatActivity {

    private static final String RC4_KEY = "my_super_secret_key"; // Keep this secure

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_subjects);

        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        String objectives = intent.getStringExtra("objectives");
        String notesKey = intent.getStringExtra("notes");
        String book = intent.getStringExtra("book");
        String bookbutton = intent.getStringExtra("bookbutton");

        // UI elements
        TextView tit = findViewById(R.id.titleTextView);
        tit.setText(title);
        TextView sampleTextView = findViewById(R.id.objectives);
        sampleTextView.setText(objectives);

        View pdf = findViewById(R.id.blockLayout);
        TextView pdfbutton = findViewById(R.id.osclick);
        pdfbutton.setText(bookbutton);
        pdf.setOnClickListener(v -> {
            Intent i = new Intent(subjects.this, PdfActivity.class);
            i.putExtra("book", book);
            startActivity(i);
        });

        // Notes EditText
        EditText note = findViewById(R.id.notesEditText);

        // Notes directory
        File notesDir = new File(getFilesDir(), "notes");
        if (!notesDir.exists()) notesDir.mkdirs();

        File noteFile = new File(notesDir, notesKey + ".enc");

        // Load note if file exists
        if (noteFile.exists()) {
            try {
                byte[] encryptedBytes = readFile(noteFile);
                byte[] decryptedBytes = RC4.rc4(RC4.stringToBytes(RC4_KEY), encryptedBytes);
                note.setText(RC4.bytesToString(decryptedBytes));
            } catch (Exception e) {
                e.printStackTrace();
                note.setText(""); // fallback
            }
        }

        // Save note with RC4 encryption
        Button saveNotesButton = findViewById(R.id.saveNotesButton);
        saveNotesButton.setOnClickListener(v -> {
            String newValue = note.getText().toString();
            try {
                byte[] encryptedBytes = RC4.rc4(RC4.stringToBytes(RC4_KEY), RC4.stringToBytes(newValue));
                writeFile(noteFile, encryptedBytes);
                Toast.makeText(subjects.this, "Note saved securely", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(subjects.this, "Failed to save note", Toast.LENGTH_SHORT).show();
            }
        });

        // Open Recording page
        Button openRecordingPageButton = findViewById(R.id.openRecordingPageButton);
        openRecordingPageButton.setOnClickListener(v -> {
            Intent recIntent = new Intent(subjects.this, RecordingActivity.class);
            startActivity(recIntent);
        });

        // Video activity button
        /*Button video = findViewById(R.id.videoaccess);
        video.setOnClickListener(v -> {
            Intent vi = new Intent(subjects.this, VideoActivity.class);
            vi.putExtra("subj", title);
            startActivity(vi);
        });*/
    }

    // Utility to read file into byte[]
    private byte[] readFile(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return bytes;
        }
    }

    // Utility to write byte[] to file
    private void writeFile(File file, byte[] data) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            fos.flush();
        }
    }
}
