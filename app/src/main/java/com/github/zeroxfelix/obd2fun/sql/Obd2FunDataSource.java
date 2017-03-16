package com.github.zeroxfelix.obd2fun.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.github.zeroxfelix.obd2fun.obd.ObdCommandJobResult;
import com.github.zeroxfelix.obd2fun.obd.ObdCommandType;
import timber.log.Timber;

public class Obd2FunDataSource {

    private static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSZ";

    private final SQLiteDatabase obdDataDatabase;

    private final SimpleDateFormat iso8601DateFormat = new SimpleDateFormat(ISO_8601_DATE_FORMAT, Locale.US);

    private final String[] obdDataColumns = {
        Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_SESSION_ID,
        Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_DATE,
        Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_OBD_COMMAND_TYPE,
        Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_RAW_RESULT,
        Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_FORMATTED_RESULT,
        Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_CALCULATED_RESULT,
        Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_RESULT_UNIT,
        Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_VIN
    };

    private final String[] troubleCodesResultColumns = {
        Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS_COLUMN_DATE,
        Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS_COLUMN_VIN,
        Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS_COLUMN_TYPE,
        Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS_COLUMN_TROUBLE_CODES_LIST
    };

    private final String[] vinMappingColumns = {
        Obd2FunDatabaseHelper.TABLE_VIN_MAPPINGS_COLUMN_NAME,
        Obd2FunDatabaseHelper.TABLE_VIN_MAPPINGS_COLUMN_VIN
    };

    public Obd2FunDataSource(Context context) {
        Obd2FunDatabaseHelper obd2FunDatabaseHelper = Obd2FunDatabaseHelper.getInstance(context);
        Timber.d("Getting a reference to the database");
        obdDataDatabase = obd2FunDatabaseHelper.getWritableDatabase();
        Timber.d("Got reference: %s", obdDataDatabase.getPath());
    }

    public void saveObdCommandJobResult(ObdCommandJobResult obdCommandJobResult) {
        ContentValues obdDataValues = new ContentValues();
        obdDataValues.put(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_SESSION_ID, obdCommandJobResult.getSessionId());
        obdDataValues.put(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_DATE, iso8601DateFormat.format(obdCommandJobResult.getDate()));
        obdDataValues.put(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_OBD_COMMAND_TYPE, obdCommandJobResult.getObdCommandType().getNameForValue());
        obdDataValues.put(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_RAW_RESULT, obdCommandJobResult.getRawResult());
        obdDataValues.put(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_FORMATTED_RESULT, obdCommandJobResult.getFormattedResult());
        obdDataValues.put(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_CALCULATED_RESULT, obdCommandJobResult.getCalculatedResult());
        obdDataValues.put(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_RESULT_UNIT, obdCommandJobResult.getResultUnit());
        obdDataValues.put(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_VIN, obdCommandJobResult.getVin());
        try {
            if (obdDataDatabase.insert(Obd2FunDatabaseHelper.TABLE_OBD_DATA, null, obdDataValues) == -1) {
                Timber.e("Error while inserting into table");
            }
        } catch (Exception e) {
            Timber.e(e, "Error while inserting into table");
        }
    }

    public void saveTroubleCodesList(String vin, TroubleCodesResult.Type type, List<String> troubleCodesList) {
        StringBuilder troubleCodesStringBuilder = new StringBuilder();
        for(String troubleCode : troubleCodesList) {
            if (troubleCodesStringBuilder.length() != 0) {
                troubleCodesStringBuilder.append(Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS_COLUMN_TROUBLE_CODES_LIST_SEPARATOR);
            }
            troubleCodesStringBuilder.append(troubleCode);
        }
        ContentValues troubleCodesResultValues = new ContentValues();
        troubleCodesResultValues.put(Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS_COLUMN_DATE, iso8601DateFormat.format(new Date()));
        troubleCodesResultValues.put(Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS_COLUMN_VIN, vin);
        troubleCodesResultValues.put(Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS_COLUMN_TYPE, type.name());
        troubleCodesResultValues.put(Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS_COLUMN_TROUBLE_CODES_LIST, troubleCodesStringBuilder.toString());
        try {
            if (obdDataDatabase.insert(Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS, null, troubleCodesResultValues) == -1) {
                Timber.e("Error while inserting into table");
            }
        } catch (Exception e) {
            Timber.e(e, "Error while inserting into table");
        }
    }

    public void deleteAllTroubleCodesResults() {
        obdDataDatabase.delete(Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS, null, null);
    }

    public void deleteTroubleCodesResultForDate(Date date) {
        obdDataDatabase.delete(Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS, Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS_COLUMN_DATE + "=?", new String[] {iso8601DateFormat.format(date)});
    }

    public void setNameForVin(String vin, String name) {
        ContentValues vinMappingValues = new ContentValues();
        vinMappingValues.put(Obd2FunDatabaseHelper.TABLE_VIN_MAPPINGS_COLUMN_VIN, vin);
        vinMappingValues.put(Obd2FunDatabaseHelper.TABLE_VIN_MAPPINGS_COLUMN_NAME, name);
        try {
            if (obdDataDatabase.replace(Obd2FunDatabaseHelper.TABLE_VIN_MAPPINGS, null, vinMappingValues) == -1) {
                Timber.e("Error while replacing into table");
            }
        } catch (Exception e) {
            Timber.e(e, "Error while replacing into table");
        }
    }

    public void deleteAllDataForVin(String vin){
        obdDataDatabase.delete(Obd2FunDatabaseHelper.TABLE_OBD_DATA, Obd2FunDatabaseHelper.TABLE_VIN_MAPPINGS_COLUMN_VIN + "=?", new String[] {vin});
        obdDataDatabase.delete(Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS, Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS_COLUMN_VIN + "=?", new String[] {vin});
        obdDataDatabase.delete(Obd2FunDatabaseHelper.TABLE_VIN_MAPPINGS, Obd2FunDatabaseHelper.TABLE_VIN_MAPPINGS_COLUMN_VIN + "=?", new String[] {vin});
    }

    private ArrayList<ObdData> cursorToObdDataList(Cursor cursor) {
        ArrayList<ObdData> obdDataList = new ArrayList<>();
        cursor.moveToFirst();
        int idSessionId = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_SESSION_ID);
        int idDate = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_DATE);
        int idObdCommandType = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_OBD_COMMAND_TYPE);
        int idRawResult = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_RAW_RESULT);
        int idFormattedResult = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_FORMATTED_RESULT);
        int idCalculatedResult = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_CALCULATED_RESULT);
        int idResultUnit = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_RESULT_UNIT);
        int idVin = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_VIN);
        while(!cursor.isAfterLast()) {
            Date date;
            try {
                date = iso8601DateFormat.parse(cursor.getString(idDate));
            } catch (Exception e) {
                Timber.e(e, "Error while parsing date");
                date = new Date();
            }
            ObdCommandType obdCommandType = ObdCommandType.getValueForName(cursor.getString(idObdCommandType));
            if (obdCommandType == null) {
                Timber.e("Error while parsing obdCommandType, setting it to Absolute Load (this is very likely to be wrong)");
                obdCommandType = ObdCommandType.ABSOLUTE_LOAD;
            }
            long sessionId = cursor.getLong(idSessionId);
            String rawResult = cursor.getString(idRawResult);
            String formattedResult = cursor.getString(idFormattedResult);
            String calculatedResult = cursor.getString(idCalculatedResult);
            String resultUnit = cursor.getString(idResultUnit);
            String vin = cursor.getString(idVin);
            obdDataList.add(new ObdData(sessionId, date, obdCommandType, rawResult, formattedResult, calculatedResult, resultUnit, vin));
            cursor.moveToNext();
        }
        cursor.close();
        return obdDataList;
    }

    private List<TroubleCodesResult> cursorToTroubleCodesResultList(Cursor cursor) {
        ArrayList<TroubleCodesResult> troubleCodesResultList = new ArrayList<>();
        cursor.moveToFirst();
        int idDate = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS_COLUMN_DATE);
        int idVin = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS_COLUMN_VIN);
        int idType = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS_COLUMN_TYPE);
        int idTroubleCodesList = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS_COLUMN_TROUBLE_CODES_LIST);
        while(!cursor.isAfterLast()) {
            Date date;
            try {
                date = iso8601DateFormat.parse(cursor.getString(idDate));
            } catch (Exception e) {
                Timber.e(e, "Error while parsing date");
                date = new Date();
            }
            String vin = cursor.getString(idVin);
            TroubleCodesResult.Type type = TroubleCodesResult.Type.getValueForName(cursor.getString(idType));
            List<String> troubleCodesList = Arrays.asList(cursor.getString(idTroubleCodesList).split(Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS_COLUMN_TROUBLE_CODES_LIST_SEPARATOR));
            troubleCodesResultList.add(new TroubleCodesResult(date, vin, type, troubleCodesList));
            cursor.moveToNext();
        }
        cursor.close();
        return troubleCodesResultList;
    }

    private String cursorToNameForVin(Cursor cursor) {
        int idName = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_VIN_MAPPINGS_COLUMN_NAME);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            String nameForVin = cursor.getString(idName);
            cursor.close();
            return nameForVin;
        } else {
            cursor.close();
            return null;
        }
    }

    public ArrayList<ObdData> getObdDataForObdCommandTypeInSession(ObdCommandType obdCommandType, String sessionId) {
        Cursor cursor = obdDataDatabase.query(Obd2FunDatabaseHelper.TABLE_OBD_DATA, obdDataColumns, Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_OBD_COMMAND_TYPE + "=? AND " + Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_SESSION_ID + "=?", new String[] {obdCommandType.getNameForValue(), sessionId}, null, null, null);
        return cursorToObdDataList(cursor);
    }

    public ArrayList<ObdData> getObdDataForSession(String sessionId){
        Cursor cursor = obdDataDatabase.query(Obd2FunDatabaseHelper.TABLE_OBD_DATA, obdDataColumns, Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_SESSION_ID + "=?", new String[] {sessionId}, null, null, null);
        return cursorToObdDataList(cursor);
    }

    public List<String> getAllSessions() {
        List<String> sessionList = new ArrayList<>();
        try {
            Cursor cursor = obdDataDatabase.query(true, Obd2FunDatabaseHelper.TABLE_OBD_DATA, obdDataColumns, null, null, Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_SESSION_ID, null, null, null);
            cursor.moveToFirst();
            int id = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_SESSION_ID);
            while (!cursor.isAfterLast()) {
                String session = cursor.getString(id);
                sessionList.add(session);
                cursor.moveToNext();
            }
            cursor.close();
        } catch (Throwable e){
            Timber.d(e, "No Sessions recorded");
        }
        return sessionList;
    }

    public List<ObdCommandType> getCommandTypesForSession(String session) {
        Cursor cursor = obdDataDatabase.query(true,Obd2FunDatabaseHelper.TABLE_OBD_DATA, obdDataColumns, Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_SESSION_ID + "=?", new String[] {session},Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_OBD_COMMAND_TYPE, null, null, null);
        List<ObdCommandType> commandTypeList = new ArrayList<>();
        cursor.moveToFirst();
        int id = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_OBD_COMMAND_TYPE);
        while(!cursor.isAfterLast()) {
            String commandTypeString = cursor.getString(id);
            ObdCommandType commandType = ObdCommandType.getValueForName(commandTypeString);
            if(commandType != null && commandType.getIsGraphable()) {
                commandTypeList.add(commandType);
            }
            cursor.moveToNext();
        }
        cursor.close();
        return commandTypeList;
    }

    public String getVinForSession(String session) {
        Cursor cursor = obdDataDatabase.query(Obd2FunDatabaseHelper.TABLE_OBD_DATA, obdDataColumns, Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_SESSION_ID + "=?", new String[] {session}, null, null, null);
        cursor.moveToFirst();
        int id = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_VIN);
        String vin = cursor.getString(id);
        cursor.close();
        return vin;
    }

    public ArrayList<ObdData> getObdDataBetweenDates(Date startDate, Date endDate) {
        Cursor cursor = obdDataDatabase.query(Obd2FunDatabaseHelper.TABLE_OBD_DATA, obdDataColumns, Obd2FunDatabaseHelper.TABLE_OBD_DATA_COLUMN_DATE + " BETWEEN ? AND ?", new String[] {iso8601DateFormat.format(startDate), iso8601DateFormat.format(endDate)}, null, null, null);
        return cursorToObdDataList(cursor);
    }

    public List<TroubleCodesResult> getAllAvailableTroubleCodesResults() {
        Cursor cursor = obdDataDatabase.query(Obd2FunDatabaseHelper.TABLE_TROUBLE_CODES_RESULTS, troubleCodesResultColumns, null, null, null, null, null);
        return cursorToTroubleCodesResultList(cursor);
    }

    public String getNameForVin(String vin) {
        Cursor cursor = obdDataDatabase.query(Obd2FunDatabaseHelper.TABLE_VIN_MAPPINGS, vinMappingColumns, Obd2FunDatabaseHelper.TABLE_VIN_MAPPINGS_COLUMN_VIN + "=?", new String[] {vin}, null, null, null);
        return cursorToNameForVin(cursor);
    }

    public ArrayList<SavedVehicles> getAllSavedVehicles() {
        Cursor cursor = obdDataDatabase.query(Obd2FunDatabaseHelper.TABLE_VIN_MAPPINGS, vinMappingColumns , null, null, null, null, null);
        return cursorToVehicleList(cursor);
    }

    private ArrayList<SavedVehicles> cursorToVehicleList(Cursor cursor) {
        ArrayList<SavedVehicles> vehicleResultList = new ArrayList<>();
        cursor.moveToFirst();

        int idVin = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_VIN_MAPPINGS_COLUMN_VIN);
        int idName = cursor.getColumnIndex(Obd2FunDatabaseHelper.TABLE_VIN_MAPPINGS_COLUMN_NAME);

        while(!cursor.isAfterLast()) {
            String vin = cursor.getString(idVin);
            String name = cursor.getString(idName);
            vehicleResultList.add(new SavedVehicles(name, vin));
            cursor.moveToNext();
        }
        cursor.close();
        return vehicleResultList;
    }
}
