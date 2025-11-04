package com.example.textstream.security;

public class RC4 {

    // Encrypts or decrypts data using RC4
    public static byte[] rc4(byte[] key, byte[] data) {
        int[] S = new int[256];
        int[] K = new int[256];
        int keyLength = key.length;

        // Initialize S and K
        for (int i = 0; i < 256; i++) {
            S[i] = i;
            K[i] = key[i % keyLength] & 0xff;
        }

        // Key-scheduling algorithm (KSA)
        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + S[i] + K[i]) & 0xff;
            int temp = S[i];
            S[i] = S[j];
            S[j] = temp;
        }

        // Pseudo-random generation algorithm (PRGA)
        byte[] result = new byte[data.length];
        int i = 0;
        j = 0;
        for (int n = 0; n < data.length; n++) {
            i = (i + 1) & 0xff;
            j = (j + S[i]) & 0xff;
            int temp = S[i];
            S[i] = S[j];
            S[j] = temp;
            int Kbyte = S[(S[i] + S[j]) & 0xff];
            result[n] = (byte) (data[n] ^ Kbyte);
        }

        return result;
    }

    // Helper: Convert string to bytes
    public static byte[] stringToBytes(String s) {
        return s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // Helper: Convert bytes to string
    public static String bytesToString(byte[] b) {
        return new String(b, java.nio.charset.StandardCharsets.UTF_8);
    }
}
