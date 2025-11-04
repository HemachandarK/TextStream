package com.example.textstream;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKeys;

import com.github.barteksc.pdfviewer.PDFView;
import com.example.textstream.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class PdfActivity extends AppCompatActivity {

    private PDFView pdfView;
    private EditText pageNumberInput;
    private Button searchButton;
    private ImageButton fabSearch;
    private File decryptedTempFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        pdfView = findViewById(R.id.pdfView);
        pageNumberInput = findViewById(R.id.pageNumberInput);
        searchButton = findViewById(R.id.searchButton);
        fabSearch = findViewById(R.id.fabSearch);

        // Initially hide input and button
        pageNumberInput.setVisibility(View.GONE);
        searchButton.setVisibility(View.GONE);

        Intent intent = getIntent();
        String bookName = intent.getStringExtra("book");

        if (bookName == null) {
            Toast.makeText(this, "No textbook found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            // Step 1: Prepare encrypted file path
            File encryptedFile = new File(getFilesDir(), bookName + ".enc");

            // Step 2: Encrypt textbook if not already encrypted
            if (!encryptedFile.exists()) {
                encryptTextbookFromAssets(this, bookName, encryptedFile);
            }

            // Step 3: Decrypt into temporary cache file for viewing
            decryptedTempFile = new File(getCacheDir(), "temp_" + bookName);
            decryptFile(this, encryptedFile, decryptedTempFile);

            // Step 4: Load PDF into viewer
            pdfView.fromFile(decryptedTempFile)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .enableAnnotationRendering(true)
                    .defaultPage(0)
                    .load();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to open textbook", Toast.LENGTH_SHORT).show();
        }

        // Toggle search controls
        fabSearch.setOnClickListener(v -> {
            if (pageNumberInput.getVisibility() == View.GONE) {
                pageNumberInput.setVisibility(View.VISIBLE);
                searchButton.setVisibility(View.VISIBLE);
            } else {
                pageNumberInput.setVisibility(View.GONE);
                searchButton.setVisibility(View.GONE);
            }
        });

        // Search specific page
        searchButton.setOnClickListener(v -> {
            String pageNumberStr = pageNumberInput.getText().toString();
            if (!pageNumberStr.isEmpty()) {
                int pageNumber = Integer.parseInt(pageNumberStr) - 1;
                if (pageNumber >= 0 && pageNumber < pdfView.getPageCount()) {
                    pdfView.jumpTo(pageNumber, true);
                } else {
                    Toast.makeText(this, "Invalid page number", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Encrypt textbook from assets into internal encrypted file
    private void encryptTextbookFromAssets(Context context, String assetName, File encryptedFile) throws Exception {
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        EncryptedFile encryptedOutput = new EncryptedFile.Builder(
                encryptedFile,
                context,
                masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        try (InputStream input = context.getAssets().open(assetName);
             OutputStream output = encryptedOutput.openFileOutput()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }
    }

    // Decrypt encrypted file into temporary cache file
    private void decryptFile(Context context, File encryptedFile, File decryptedFile) throws Exception {
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        EncryptedFile encryptedInput = new EncryptedFile.Builder(
                encryptedFile,
                context,
                masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        try (InputStream input = encryptedInput.openFileInput();
             OutputStream output = new FileOutputStream(decryptedFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up decrypted temp file for security
        if (decryptedTempFile != null && decryptedTempFile.exists()) {
            decryptedTempFile.delete();
        }
    }
}
