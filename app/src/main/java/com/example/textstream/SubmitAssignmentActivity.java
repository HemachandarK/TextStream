package com.example.textstream;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SubmitAssignmentActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 1;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private Uri pdfUri;
    private TextView tvFileName;
    private Button btnUpload, btnEncrypt;

    private SecretKey secretKey;
    private byte[] iv;
    private TextView tvLog;
    private static final String PREFS_NAME = "AssignmentLogs";
    private static final String LOG_KEY = "logs";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_assignment);
        tvLog = findViewById(R.id.tvLog);
        loadLogs(); // Load existing logs when app starts


        tvFileName = findViewById(R.id.tvFileName);
        btnUpload = findViewById(R.id.btnUpload);
        btnEncrypt = findViewById(R.id.btnEncrypt);

        // Ask for Storage Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPdfFile();
            }
        });

        btnEncrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pdfUri != null) {
                    encryptAndSavePdf();
                } else {
                    Toast.makeText(SubmitAssignmentActivity.this, "Please select a file first", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void selectPdfFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), PICK_PDF_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null) {
            pdfUri = data.getData();
            String fileName = getFileNameFromUri(pdfUri);
            tvFileName.setText("Selected File: " + fileName);
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = "unknown.pdf";
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
            }
        }
        return fileName;
    }

    private void addLog(String message) {
        try {
            String timestamp = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                    .format(new java.util.Date());
            String newEntry = "[" + timestamp + "] " + message + "\n";

            // Append to shared log file (same as staff)
            File logFile = new File(getExternalFilesDir(null), "student_log.txt");
            java.io.FileWriter writer = new java.io.FileWriter(logFile, true);
            writer.append(newEntry);
            writer.close();

            loadLogs(); // refresh UI after writing
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadLogs() {
        try {
            File logFile = new File(getExternalFilesDir(null), "student_log.txt");
            if (!logFile.exists()) {
                tvLog.setText("No activity yet.");
                return;
            }

            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(logFile));
            StringBuilder logs = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                logs.append(line).append("\n");
            }
            reader.close();
            tvLog.setText(logs.toString());
        } catch (Exception e) {
            e.printStackTrace();
            tvLog.setText("Error loading logs");
        }
    }



    private void encryptAndSavePdf() {
        try {
            // ✅ Correct 32-byte key and 16-byte IV
            String base64Key = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="; // exactly 32 bytes
            String base64IV = "MDEyMzQ1Njc4OTAxMjM0NQ=="; // exactly 16 bytes

            byte[] keyBytes = Base64.decode(base64Key, Base64.DEFAULT);
            byte[] ivBytes = Base64.decode(base64IV, Base64.DEFAULT);

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            InputStream inputStream = getContentResolver().openInputStream(pdfUri);
            byte[] inputBytes = new byte[inputStream.available()];
            inputStream.read(inputBytes);
            inputStream.close();

            byte[] encryptedBytes = cipher.doFinal(inputBytes);

            File dir = new File(getExternalFilesDir(null), "EncryptedAssignments");
            if (!dir.exists()) dir.mkdirs();

            File encryptedFile = new File(dir, "assignment_encrypted.pdf");
            FileOutputStream fos = new FileOutputStream(encryptedFile);
            fos.write(encryptedBytes);
            fos.close();

            addLog("Assignment encrypted and submitted successfully");
            Toast.makeText(this, "File encrypted and uploaded!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}
