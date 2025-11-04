package com.example.textstream;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class StaffAssignmentActivity extends AppCompatActivity {

    private ListView lvAssignments;
    private ArrayList<String> fileNames = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private File assignmentDir;
    private KeyPair keyPair;
    private List<File> assignmentFiles = new ArrayList<>();
// for digital signing
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_assignment);

        lvAssignments = findViewById(R.id.lvAssignments);
        Button btnView = findViewById(R.id.btnView);
        Button btnSign = findViewById(R.id.btnSign);
        Button btnReject = findViewById(R.id.btnReject);

        // Initialize file lists
        assignmentFiles = new ArrayList<>();

        // Load assignment files directory
        // Load assignment files directory
        assignmentDir = new File(getExternalFilesDir(null), "EncryptedAssignments");
        if (!assignmentDir.exists()) assignmentDir.mkdirs();

        File[] files = assignmentDir.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.getName().endsWith(".pdf")) {
                    assignmentFiles.add(file);
                    fileNames.add(file.getName()); // ✅ now uses the field variable
                }
            }
        } else {
            Toast.makeText(this, "No assignments found", Toast.LENGTH_SHORT).show();
        }

// Attach adapter to ListView
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, fileNames);
        lvAssignments.setAdapter(adapter);
        lvAssignments.setChoiceMode(ListView.CHOICE_MODE_SINGLE);


        // Generate RSA KeyPair for digital signature
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            keyPair = keyGen.generateKeyPair();
        } catch (Exception e) {
            Toast.makeText(this, "Key generation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        // Button actions
        btnView.setOnClickListener(v -> openSelectedFile());
        btnSign.setOnClickListener(v -> digitallySignFile());
        btnReject.setOnClickListener(v -> rejectFile());
    }


    private void openSelectedFile() {
        int position = lvAssignments.getCheckedItemPosition();
        if (position == ListView.INVALID_POSITION) {
            Toast.makeText(this, "Select a file first!", Toast.LENGTH_SHORT).show();
            return;
        }

        File encryptedFile = new File(assignmentDir, fileNames.get(position));

        try {
            // ✅ Correct 32-byte key and 16-byte IV (MUST MATCH STUDENT SIDE)
            String base64Key = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="; // exactly 32 bytes
            String base64IV = "MDEyMzQ1Njc4OTAxMjM0NQ=="; // exactly 16 bytes

            byte[] keyBytes = Base64.decode(base64Key, Base64.DEFAULT);
            byte[] ivBytes = Base64.decode(base64IV, Base64.DEFAULT);

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            // Decrypt and save to temp file
            File decryptedFile = new File(getExternalFilesDir(null), "decrypted_assignment.pdf");
            decryptFile(encryptedFile, decryptedFile, secretKey, ivSpec);

            // Open PDF viewer
            Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", decryptedFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Decryption failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Proper AES/CBC/PKCS5 decryption method matching student-side encryption
     */
    private void decryptFile(File inputFile, File outputFile, SecretKeySpec keySpec, IvParameterSpec ivSpec) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        try (
                FileInputStream fis = new FileInputStream(inputFile);
                CipherInputStream cis = new CipherInputStream(fis, cipher);
                FileOutputStream fos = new FileOutputStream(outputFile)
        ) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }




    private void digitallySignFile() {
        int pos = lvAssignments.getCheckedItemPosition();
        if (pos == ListView.INVALID_POSITION) {
            Toast.makeText(this, "Select a file to sign", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File encryptedFile = new File(assignmentDir, fileNames.get(pos));

            // Step 1: Decrypt the file first
            String base64Key = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
            String base64IV = "MDEyMzQ1Njc4OTAxMjM0NQ==";

            byte[] keyBytes = Base64.decode(base64Key, Base64.DEFAULT);
            byte[] ivBytes = Base64.decode(base64IV, Base64.DEFAULT);

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            // Decrypt to temporary file
            File decryptedFile = new File(getExternalFilesDir(null), "temp_for_signing.pdf");
            decryptFile(encryptedFile, decryptedFile, secretKey, ivSpec);

            // Step 2: Create digital signature on the DECRYPTED file
            byte[] data = java.nio.file.Files.readAllBytes(decryptedFile.toPath());

            Signature signature = Signature.getInstance("SHA256withRSA");
            PrivateKey privateKey = keyPair.getPrivate();
            signature.initSign(privateKey);
            signature.update(data);
            byte[] digitalSignature = signature.sign();

            // Step 3: Save signature file
            File sigFile = new File(assignmentDir, encryptedFile.getName() + ".sig");
            java.nio.file.Files.write(sigFile.toPath(), digitalSignature);

            // Step 4: Add digital stamp to the decrypted PDF
            addDigitalStamp(decryptedFile);

            // Clean up temp file
            decryptedFile.delete();

            logAction(encryptedFile.getName(), "APPROVED and Digitally Signed ✅");
            Toast.makeText(this, "File Signed Successfully", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Signing failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void rejectFile() {
        int pos = lvAssignments.getCheckedItemPosition();
        if (pos == ListView.INVALID_POSITION) {
            Toast.makeText(this, "Select a file to reject", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(assignmentDir, fileNames.get(pos));
        logAction(file.getName(), "REJECTED ❌");
        Toast.makeText(this, "File Rejected", Toast.LENGTH_SHORT).show();
    }

    private void addDigitalStamp(File pdfFile) {
        try {
            // Read the decrypted PDF
            PdfReader reader = new PdfReader(pdfFile.getAbsolutePath());

            // Create output file with stamp
            File stampedFile = new File(assignmentDir, "stamped_" + pdfFile.getName());
            PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(stampedFile));

            // Add "APPROVED" stamp on first page
            PdfContentByte canvas = stamper.getOverContent(1);

            // Set stamp properties
            canvas.saveState();
            canvas.setColorFill(BaseColor.GREEN);
            canvas.setFontAndSize(BaseFont.createFont(), 48);
            canvas.beginText();
            canvas.showTextAligned(Element.ALIGN_CENTER, "APPROVED", 300, 400, 45);
            canvas.endText();
            canvas.restoreState();

            stamper.close();
            reader.close();

            android.util.Log.d("STAMP_DEBUG", "Stamped file created: " + stampedFile.exists());
            android.util.Log.d("STAMP_DEBUG", "Stamped file size: " + stampedFile.length() + " bytes");
            Toast.makeText(this, "Digital stamp added successfully", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to add stamp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void logAction(String fileName, String status) {
        try {
            String timestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
            String logEntry = timestamp + " - File: " + fileName + " - Status: " + status + "\n";

            // Staff log
            File staffLog = new File(getExternalFilesDir(null), "staff_log.txt");
            FileWriter staffWriter = new FileWriter(staffLog, true);
            staffWriter.append(logEntry);
            staffWriter.close();

            // Student log (shared notification)
            File studentLog = new File(getExternalFilesDir(null), "student_log.txt");
            FileWriter studentWriter = new FileWriter(studentLog, true);
            studentWriter.append(logEntry);
            studentWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
