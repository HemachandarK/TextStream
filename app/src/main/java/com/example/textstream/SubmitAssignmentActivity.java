package com.example.textstream;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.crypto.Cipher;
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

    // Animation views
    private View encryptionOverlay;
    private View characterContainer;
    private TextView character;
    private TextView fileIcon;
    private TextView fileInHand;
    private TextView encryptionEffect;
    private TextView tvEncryptionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_assignment);
        tvLog = findViewById(R.id.tvLog);
        loadLogs();

        tvFileName = findViewById(R.id.tvFileName);
        btnUpload = findViewById(R.id.btnUpload);
        btnEncrypt = findViewById(R.id.btnEncrypt);

        // Initialize animation views
        encryptionOverlay = findViewById(R.id.encryptionOverlay);
        characterContainer = findViewById(R.id.characterContainer);
        character = findViewById(R.id.character);
        fileIcon = findViewById(R.id.fileIcon);
        fileInHand = findViewById(R.id.fileInHand);
        encryptionEffect = findViewById(R.id.encryptionEffect);
        tvEncryptionStatus = findViewById(R.id.tvEncryptionStatus);

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
                    showEncryptionAnimation();
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

            File logFile = new File(getExternalFilesDir(null), "student_log.txt");
            java.io.FileWriter writer = new java.io.FileWriter(logFile, true);
            writer.append(newEntry);
            writer.close();

            loadLogs();
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

    private void showEncryptionAnimation() {
        // Show overlay
        encryptionOverlay.setVisibility(View.VISIBLE);
        encryptionOverlay.setAlpha(0f);
        encryptionOverlay.animate().alpha(1f).setDuration(300).start();

        // Disable submit button
        btnEncrypt.setEnabled(false);

        // Get screen width
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;

        Handler handler = new Handler();

        // Reset positions
        characterContainer.setTranslationX(-120);
        fileIcon.setVisibility(View.VISIBLE);
        fileInHand.setVisibility(View.GONE);
        encryptionEffect.setVisibility(View.GONE);

        tvEncryptionStatus.setText("Student approaching...");

        // Step 1: Character walks to file (1.5 seconds)
        ObjectAnimator walkToFile = ObjectAnimator.ofFloat(characterContainer, "translationX", -120f, screenWidth / 2 - 100);
        walkToFile.setDuration(1500);
        walkToFile.setInterpolator(new LinearInterpolator());
        walkToFile.start();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Step 2: Pick up file
                tvEncryptionStatus.setText("Picking up assignment...");
                fileIcon.setVisibility(View.GONE);
                fileInHand.setVisibility(View.VISIBLE);

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Step 3: Show encryption effect
                        tvEncryptionStatus.setText("🔐 Encrypting assignment...");
                        encryptionEffect.setVisibility(View.VISIBLE);
                        encryptionEffect.setAlpha(0f);
                        encryptionEffect.setScaleX(0.5f);
                        encryptionEffect.setScaleY(0.5f);

                        // Pulse animation for encryption effect
                        encryptionEffect.animate()
                                .alpha(1f)
                                .scaleX(1.5f)
                                .scaleY(1.5f)
                                .setDuration(500)
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        encryptionEffect.animate()
                                                .alpha(0f)
                                                .scaleX(0.5f)
                                                .scaleY(0.5f)
                                                .setDuration(500)
                                                .start();
                                    }
                                })
                                .start();

                        // Perform actual encryption in background
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                encryptAndSavePdf();
                            }
                        }).start();

                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // Step 4: Walk away with encrypted file
                                tvEncryptionStatus.setText("✅ Submitting to teacher...");
                                character.setText("🚶");
                                fileInHand.setText("🔐📄");

                                ObjectAnimator walkAway = ObjectAnimator.ofFloat(characterContainer, "translationX",
                                        screenWidth / 2 - 100, screenWidth + 120);
                                walkAway.setDuration(1500);
                                walkAway.setInterpolator(new LinearInterpolator());
                                walkAway.start();

                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Step 5: Success message and fade out
                                        tvEncryptionStatus.setText("✅ Assignment submitted successfully!");

                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                encryptionOverlay.animate()
                                                        .alpha(0f)
                                                        .setDuration(500)
                                                        .withEndAction(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                encryptionOverlay.setVisibility(View.GONE);
                                                                btnEncrypt.setEnabled(true);
                                                                pdfUri = null;
                                                                tvFileName.setText("No file selected");
                                                            }
                                                        })
                                                        .start();
                                            }
                                        }, 1000);
                                    }
                                }, 1500);
                            }
                        }, 1200);
                    }
                }, 600);
            }
        }, 1500);
    }

    private void encryptAndSavePdf() {
        try {
            String base64Key = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
            String base64IV = "MDEyMzQ1Njc4OTAxMjM0NQ==";

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

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvEncryptionStatus.setText("❌ Error: " + e.getMessage());
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            encryptionOverlay.animate()
                                    .alpha(0f)
                                    .setDuration(300)
                                    .withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            encryptionOverlay.setVisibility(View.GONE);
                                            btnEncrypt.setEnabled(true);
                                        }
                                    })
                                    .start();
                        }
                    }, 2000);
                }
            });
        }
    }
}