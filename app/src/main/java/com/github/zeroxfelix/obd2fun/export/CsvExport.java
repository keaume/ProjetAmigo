package com.github.zeroxfelix.obd2fun.export;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.github.zeroxfelix.obd2fun.sql.ObdData;
import timber.log.Timber;

public class CsvExport{

    private static final String EXTERNAL_STORAGE_EXPORT_DIRECTORY = "export";
    private static final String EXTERNAL_STORAGE_MAIN_DIRECTORY = "obd2fun";
    private final String fileName;
    private final ArrayList<ObdData> obdDataList;

    public CsvExport(ArrayList<ObdData> obdDataList, Date startDate, Date endDate){

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy_HHmm", Locale.GERMANY);
        String startDateString = dateFormat.format(startDate);
        String endDateString = dateFormat.format(endDate);

        this.fileName = startDateString + "-" + endDateString + ".csv";
        this.obdDataList = obdDataList;

    }

    public CsvExport(ArrayList<ObdData> obdDataList, String sessionId){

        long sessionLong =  Long.parseLong(sessionId);
        Date date = new Date(sessionLong);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy_HHmm", Locale.GERMANY);

        this.fileName = dateFormat.format(date) + ".csv";
        this.obdDataList = obdDataList;

    }

    public void writeFileToExternalStorage() {
        List<String> csvData = new ArrayList<>();
        csvData.add("SessionId;Date;ObdCommand;Result;Unit;VIN");
        for(ObdData obdData : this.obdDataList){
            csvData.add(obdData.getSessionId() + ";" + obdData.getRecordingDate().toString() + ";" + obdData.getObdCommandType().getNameForValue() + ";" + obdData.getCalculatedResult() + ";" + obdData.getResultUnit() + ";" + obdData.getVin());
        }

        Timber.d("Writing file to external storage");
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        File externalStorageSaveDirectory = new File(externalStorageDirectory.getAbsolutePath() + "/" + EXTERNAL_STORAGE_MAIN_DIRECTORY + "/" + EXTERNAL_STORAGE_EXPORT_DIRECTORY);
        //noinspection ResultOfMethodCallIgnored
        externalStorageSaveDirectory.mkdirs();
        if (externalStorageSaveDirectory.isDirectory()) {
            File file = new File(externalStorageSaveDirectory, this.fileName);
            try {
                FileWriter fileWriter = new FileWriter(file);
                for (int i = 0; i < csvData.size(); i++) {
                    fileWriter.write( csvData.get(i) + "\n");
                }
                fileWriter.close();

            } catch (Exception e) {
                Timber.e(e, "Writing to file %s failed", file.getAbsolutePath());
            }
        } else {
            Timber.e("Failed to create directory %s", externalStorageSaveDirectory.getAbsolutePath());
        }
    }

}