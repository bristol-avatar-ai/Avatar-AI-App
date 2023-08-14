// 
// Decompiled by Procyon v0.5.36
// 

package com.example.avatar_ai_app.imagerecognition.tf;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.tensorflow.lite.support.common.SupportPreconditions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class AdaptedFileUtil
{
    private AdaptedFileUtil() {
    }
    
    @NonNull
    public static List<String> loadLabels(@NonNull final Context context, @NonNull final String filePath) throws IOException {
        return loadLabels(context, filePath, Charset.defaultCharset());
    }

    /**
     * Adapted version of loadLabels that can access files from outside of context assets.
     * @param context
     * @param filePath Canonical filepath to a labels file
     * @param cs
     * @return List<String>
     * @throws IOException
     */
    
    @NonNull
    public static List<String> loadLabels(@NonNull final Context context, @NonNull final String filePath, final Charset cs) throws IOException {
        SupportPreconditions.checkNotNull(context, "Context cannot be null.");
        SupportPreconditions.checkNotNull(filePath, "File path cannot be null.");
        File file = new File(filePath);
        final InputStream inputStream = new FileInputStream(file);
        try {
            final List<String> loadLabels = loadLabels(inputStream, cs);
            if (inputStream != null) {
                inputStream.close();
            }
            return loadLabels;
        }
        catch (Throwable t) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (Throwable exception) {
                    t.addSuppressed(exception);
                }
            }
            throw t;
        }
    }
    
    @NonNull
    public static List<String> loadLabels(@NonNull final InputStream inputStream) throws IOException {
        return loadLabels(inputStream, Charset.defaultCharset());
    }
    
    @NonNull
    public static List<String> loadLabels(@NonNull final InputStream inputStream, final Charset cs) throws IOException {
        final List<String> labels = new ArrayList<String>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, cs));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() > 0) {
                    labels.add(line);
                }
            }
            final List<String> list = labels;
            reader.close();
            return list;
        }
        catch (Throwable t) {
            try {
                reader.close();
            }
            catch (Throwable exception) {
                t.addSuppressed(exception);
            }
            throw t;
        }
    }
    
    @NonNull
    public static List<String> loadSingleColumnTextFile(@NonNull final Context context, @NonNull final String filePath, final Charset cs) throws IOException {
        return loadLabels(context, filePath, cs);
    }
    
    @NonNull
    public static List<String> loadSingleColumnTextFile(@NonNull final InputStream inputStream, final Charset cs) throws IOException {
        return loadLabels(inputStream, cs);
    }

    /**
     * Adapted version of loadMappedFile, which can take in files outside of context assets
     * @param context Pass in contex
     * @param filePath Canonical filepath to a .tflite model
     * @return MappedByteBuffer
     * @throws IOException
     */
    @NonNull
    public static MappedByteBuffer loadMappedFile(@NonNull final Context context, @NonNull final String filePath) throws IOException {
        SupportPreconditions.checkNotNull(context, "Context should not be null.");
        SupportPreconditions.checkNotNull(filePath, "File path cannot be null.");
        File file = new File(filePath);
        try (FileInputStream inputStream = new FileInputStream(file);
             FileChannel fileChannel = inputStream.getChannel()) {
            final long startOffset = 0L;
            final long declaredLength = file.length();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }

    }
        @NonNull
    public static byte[] loadByteFromFile(@NonNull final Context context, @NonNull final String filePath) throws IOException {
        final ByteBuffer buffer = loadMappedFile(context, filePath);
        final byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);
        return byteArray;
    }
}
