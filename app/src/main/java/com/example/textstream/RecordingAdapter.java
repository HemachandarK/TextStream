package com.example.textstream;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKeys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class RecordingAdapter extends RecyclerView.Adapter<RecordingAdapter.ViewHolder> {

    private List<File> recordings;
    private Context context;

    public RecordingAdapter(List<File> recordings, Context context) {
        this.recordings = recordings;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recording_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File recording = recordings.get(position);
        holder.recordingName.setText(recording.getName());

        holder.playButton.setOnClickListener(v -> {
            try {
                // Decrypt the file into a temporary cache file
                File tempDecrypted = new File(context.getCacheDir(), "temp_play.3gp");
                decryptFile(recording, tempDecrypted);

                MediaPlayer mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(tempDecrypted.getAbsolutePath());
                mediaPlayer.prepare();
                mediaPlayer.start();

                // Delete the temp file after playback
                mediaPlayer.setOnCompletionListener(mp -> tempDecrypted.delete());

                Toast.makeText(context, "Playing recording", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Failed to play recording", Toast.LENGTH_SHORT).show();
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (recording.delete()) {
                recordings.remove(position);
                notifyItemRemoved(position);
                Toast.makeText(context, "Recording deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to delete recording", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return recordings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView recordingName;
        Button playButton, deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recordingName = itemView.findViewById(R.id.recordingName);
            playButton = itemView.findViewById(R.id.playButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    // Utility method to decrypt an encrypted voice note
    private void decryptFile(File encryptedFile, File outputFile) throws Exception {
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        EncryptedFile ef = new EncryptedFile.Builder(
                encryptedFile,
                context,
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
