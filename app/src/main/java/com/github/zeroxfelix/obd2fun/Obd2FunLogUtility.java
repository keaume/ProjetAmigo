package com.github.zeroxfelix.obd2fun;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public final class Obd2FunLogUtility implements Thread.UncaughtExceptionHandler {

    private static final String EXTERNAL_STORAGE_MAIN_DIRECTORY = "obd2fun";
    private static final String EXTERNAL_STORAGE_LOG_DIRECTORY = "logs";
    private static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSZ";
    private static final String PROCESS_ID = Integer.toString(android.os.Process.myPid());

    private final Thread.UncaughtExceptionHandler defaultExceptionHandler;

    public Obd2FunLogUtility() {
        this.defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        saveStacktraceToExternalStorage(ex);
        saveLogToExternalStorage();
        defaultExceptionHandler.uncaughtException(thread, ex);
    }

    private static String getLog() {
        Timber.d("Getting log from logcat");
        StringBuilder builder = new StringBuilder();
        try {
            String[] command = new String[] { "logcat", "-d", "-v", "threadtime" };
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(PROCESS_ID)) {
                    builder.append(line).append("\n");
                }
            }
        } catch (IOException ioe) {
            Timber.e(ioe, "Reading the logs failed");
        }
        return builder.toString();
    }

    private static void writeFileToExternalStorage(String fileName, String content) {
        Timber.d("Writing file to external storage");
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        File externalStorageSaveDirectory = new File(externalStorageDirectory.getAbsolutePath() + "/" + EXTERNAL_STORAGE_MAIN_DIRECTORY + "/" + EXTERNAL_STORAGE_LOG_DIRECTORY);
        //noinspection ResultOfMethodCallIgnored
        externalStorageSaveDirectory.mkdirs();
        if (externalStorageSaveDirectory.isDirectory()) {
            File file = new File(externalStorageSaveDirectory, fileName);
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(content.getBytes());
                fileOutputStream.close();
            } catch (Exception e) {
                Timber.e(e, "Writing to file %s failed", file.getAbsolutePath());
            }
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri uri = Uri.fromFile(file);
            intent.setData(uri);
            Obd2FunApplication.sendBroadcastStatic(intent);
        } else {
            Timber.e("Failed to create directory %s", externalStorageSaveDirectory.getAbsolutePath());
        }
    }

    public static void saveLogToExternalStorage() {
        Timber.d("Saving current log to external storage");
        final SimpleDateFormat iso8601DateFormat = new SimpleDateFormat(ISO_8601_DATE_FORMAT, Locale.US);
        writeFileToExternalStorage("OBD2Fun " + iso8601DateFormat.format(new Date()) + ".log", getLog());
    }

    private static void saveStacktraceToExternalStorage(Throwable ex) {
        Timber.d("Saving a stacktrace to external storage");
        final SimpleDateFormat iso8601DateFormat = new SimpleDateFormat(ISO_8601_DATE_FORMAT, Locale.US);
        StringWriter stackTrace = new StringWriter();
        ex.printStackTrace(new PrintWriter(stackTrace));
        writeFileToExternalStorage("OBD2Fun " + iso8601DateFormat.format(new Date()) + ".stacktrace", stackTrace.toString());
    }
}