package com.example.textstream;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import androidx.appcompat.widget.SearchView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.itextpdf.kernel.pdf.EncryptionConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StudentHome extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 100;

    private SearchView searchView;
    private RecyclerView recyclerView;
    private BlockAdapter blockAdapter;
    private List<BlockItem> blockList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_home);

        searchView = findViewById(R.id.searchView);
        recyclerView = findViewById(R.id.recyclerView);

        // Sample subject list
        blockList = new ArrayList<>();
        blockList.add(new BlockItem("Operating Systems", "Open Text Book", subjects.class, "", "os_notes", ""));
        blockList.add(new BlockItem("Computer Networks", "Open Text Book", subjects.class, "", "cn_notes", ""));
        blockList.add(new BlockItem("Graph Theory", "Open Text Book", subjects.class, getString(R.string.gt_obj), "gt_notes", getString(R.string.gt_book)));
        blockList.add(new BlockItem("Web Technologies", "Open Text Book", subjects.class, getString(R.string.wt_obj), "wt_notes", getString(R.string.wt_book)));
        blockList.add(new BlockItem("Compiler Engineering", "Open Text Book", subjects.class, getString(R.string.ce_obj), "ce_notes", getString(R.string.ce_book)));
        blockList.add(new BlockItem("Advances in Databases", "Open Text Book", subjects.class, getString(R.string.adb_obj), "adb_notes", getString(R.string.adb_book)));
        blockList.add(new BlockItem("Digital Logic and Design", "Open Text Book", subjects.class, getString(R.string.dld_obj), "dld_notes", getString(R.string.dld_book)));
        blockList.add(new BlockItem("Programming and Data Structures", "Open Text Book", subjects.class, getString(R.string.pds_obj), "pds_notes", getString(R.string.pds_book)));
        blockList.add(new BlockItem("Database Management Systems", "Open Text Book", subjects.class, getString(R.string.dbms_obj), "dbms_notes", getString(R.string.dbms_book)));
        blockList.add(new BlockItem("OOPS and ADS", "Open Text Book", subjects.class, getString(R.string.oops_obj), "oops_notes", getString(R.string.oops_book)));

        blockAdapter = new BlockAdapter(blockList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(blockAdapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                blockAdapter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                blockAdapter.filter(newText);
                return false;
            }
        });

        Button shareButton = findViewById(R.id.shareButton);
        shareButton.setOnClickListener(v -> openFilePicker());
    }

    /** Step 1: Launch file picker **/
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select PDF to Encrypt & Share"), PICK_FILE_REQUEST);
    }

    /** Step 2: Read file bytes **/
    private byte[] readBytesFromUri(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] tmp = new byte[4096];
            int read;
            while ((read = is.read(tmp)) != -1) {
                buffer.write(tmp, 0, read);
            }
            return buffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Step 3: Get file display name **/
    private String getFileDisplayName(Uri uri) {
        String name = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (name == null) {
            String path = uri.getLastPathSegment();
            if (path != null) name = path.replaceAll(".*/", "");
        }
        return name;
    }

    /** Step 4: Handle selected file **/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) {
                Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                byte[] pdfBytes = readBytesFromUri(uri);
                if (pdfBytes == null) {
                    Toast.makeText(this, "Unable to read PDF", Toast.LENGTH_SHORT).show();
                    return;
                }

                String displayName = getFileDisplayName(uri);
                if (displayName == null) displayName = "shared_file.pdf";

                // Step 5: Generate random password
                String password = generateRandomPassword(8);

                // Step 6: Encrypt PDF
                File encryptedDir = new File(getFilesDir(), "shared_enc");
                if (!encryptedDir.exists()) encryptedDir.mkdirs();
                File encryptedFile = new File(encryptedDir, displayName);

                try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
                     PdfWriter writer = new PdfWriter(new FileOutputStream(encryptedFile),
                             new com.itextpdf.kernel.pdf.WriterProperties()
                                     .setStandardEncryption(
                                             password.getBytes(),
                                             password.getBytes(),
                                             EncryptionConstants.ALLOW_PRINTING,
                                             EncryptionConstants.ENCRYPTION_AES_128))) {
                    PdfDocument pdfDoc = new PdfDocument(reader, writer);
                    pdfDoc.close();
                }

                // Step 7: Show password to sharer
                showPasswordDialog(password, encryptedFile);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "PDF encryption failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    /** Step 8: Random password generator **/
    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < length; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    /** Step 9: Show password and share **/
    private void showPasswordDialog(String password, File encryptedFile) {
        new AlertDialog.Builder(this)
                .setTitle("PDF Password")
                .setMessage("Password: " + password)
                .setPositiveButton("Share via Bluetooth", (dialog, which) -> shareEncryptedFile(encryptedFile))
                .setCancelable(false)
                .show();
    }

    /** Step 10: Share file **/
    private void shareEncryptedFile(File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(this, "Encrypted file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".provider",
                file
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share Encrypted PDF via Bluetooth"));
    }
}
