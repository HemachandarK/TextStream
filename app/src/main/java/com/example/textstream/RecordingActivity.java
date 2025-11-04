package com.example.textstream;

import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKeys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecordingActivity extends AppCompatActivity {

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String tempAudioFilePath; // temporary raw recording path
    private Button startRecordingButton, stopRecordingButton, playRecordingButton;
    private EditText recordingNameEditText;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        // Request microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }

        startRecordingButton = findViewById(R.id.startRecordingButton);
        stopRecordingButton = findViewById(R.id.stopRecordingButton);
        playRecordingButton = findViewById(R.id.playRecordingButton);
        recordingNameEditText = findViewById(R.id.recordingNameEditText);

        startRecordingButton.setOnClickListener(v -> {
            startRecording();
            startRecordingButton.setEnabled(false);
            stopRecordingButton.setEnabled(true);
        });

        stopRecordingButton.setOnClickListener(v -> {
            stopRecording();
            stopRecordingButton.setEnabled(false);
            playRecordingButton.setEnabled(true);
            loadRecordings();
        });

        playRecordingButton.setOnClickListener(v -> {
            playRecording();
        });

        loadRecordings();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission to record audio is required!", Toast.LENGTH_SHORT).show();
                startRecordingButton.setEnabled(false);
            }
        }
    }

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {

            String recordingName = recordingNameEditText.getText().toString().trim();
            recordingName = recordingName.replaceAll("[^a-zA-Z0-9]", "_");
            if (recordingName.isEmpty()) {
                Toast.makeText(this, "Please enter a name for the recording", Toast.LENGTH_SHORT).show();
                return;
            }

            tempAudioFilePath = getCacheDir().getAbsolutePath() + "/" + recordingName + "_temp.3gp";

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(tempAudioFilePath);

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;

        Toast.makeText(this, "Recording saved (encrypted)", Toast.LENGTH_SHORT).show();

        // Encrypt the temp file into internal storage
        try {
            String recordingName = new File(tempAudioFilePath).getName().replace("_temp.3gp", "");
            File encryptedFile = new File(getFilesDir(), recordingName + ".enc");
            encryptFile(new File(tempAudioFilePath), encryptedFile);

            // Delete temporary raw file
            new File(tempAudioFilePath).delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playRecording() {
        // For simplicity, play the first recording in internal storage
        File directory = getFilesDir();
        File[] encryptedFiles = directory.listFiles((dir, name) -> name.endsWith(".enc"));
        if (encryptedFiles == null || encryptedFiles.length == 0) {
            Toast.makeText(this, "No recordings found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File decryptedTempFile = new File(getCacheDir(), "temp_play.3gp");
            decryptFile(encryptedFiles[0], decryptedTempFile);

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(decryptedTempFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(this, "Playing recording", Toast.LENGTH_SHORT).show();

            // Delete temp decrypted file after playback finishes
            mediaPlayer.setOnCompletionListener(mp -> decryptedTempFile.delete());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRecordings() {
        File directory = getFilesDir();
        File[] encryptedFiles = directory.listFiles((dir, name) -> name.endsWith(".enc"));
        List<File> recordings = new ArrayList<>();
        if (encryptedFiles != null) {
            recordings.addAll(Arrays.asList(encryptedFiles));
        }

        RecyclerView recordingListView = findViewById(R.id.recordingListView);
        recordingListView.setLayoutManager(new LinearLayoutManager(this));
        recordingListView.setAdapter(new RecordingAdapter(recordings, this));
    }

    // Encrypt file using EncryptedFile (AES-256)
    private void encryptFile(File inputFile, File outputFile) throws Exception {
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        EncryptedFile encryptedFile = new EncryptedFile.Builder(
                outputFile,
                this,
                masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        try (InputStream input = new FileInputStream(inputFile);
             OutputStream output = encryptedFile.openFileOutput()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
    }

    // Decrypt encrypted file into a temporary file
    private void decryptFile(File encryptedFile, File outputFile) throws Exception {
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        EncryptedFile ef = new EncryptedFile.Builder(
                encryptedFile,
                this,
                masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        try (InputStream input = ef.openFileInput();
             OutputStream output = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
    }
}
