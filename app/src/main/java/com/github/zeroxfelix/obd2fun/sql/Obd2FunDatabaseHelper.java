package com.github.zeroxfelix.obd2fun.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import timber.log.Timber;

public class Obd2FunDatabaseHelper extends SQLiteOpenHelper {

    private static Obd2FunDatabaseHelper instance;

    public static final String DB_FILE_NAME = "obd2fun.db";
    public static final int DB_VERSION = 1;

    public static final String TABLE_OBD_DATA = "obd_data";
    public static final String TABLE_OBD_DATA_COLUMN_ID = "_id";
    public static final String TABLE_OBD_DATA_COLUMN_SESSION_ID = "session_id";
    public static final String TABLE_OBD_DATA_COLUMN_DATE = "date";
    public static final String TABLE_OBD_DATA_COLUMN_OBD_COMMAND_TYPE = "obd_command_type";
    public static final String TABLE_OBD_DATA_COLUMN_RAW_RESULT = "raw_result";
    public static final String TABLE_OBD_DATA_COLUMN_FORMATTED_RESULT = "formatted_result";
    public static final String TABLE_OBD_DATA_COLUMN_CALCULATED_RESULT = "calculated_result";
    public static final String TABLE_OBD_DATA_COLUMN_RESULT_UNIT = "result_unit";
    public static final String TABLE_OBD_DATA_COLUMN_VIN = "vin";

    private static final String SQL_CREATE_TABLE_OBD_DATA =
        "CREATE TABLE " + TABLE_OBD_DATA + "(" +
            TABLE_OBD_DATA_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            TABLE_OBD_DATA_COLUMN_SESSION_ID + " INTEGER NOT NULL, " +
            TABLE_OBD_DATA_COLUMN_DATE + " TEXT NOT NULL, " +
            TABLE_OBD_DATA_COLUMN_OBD_COMMAND_TYPE + " TEXT NOT NULL, " +
            TABLE_OBD_DATA_COLUMN_RAW_RESULT + " TEXT NOT NULL, " +
            TABLE_OBD_DATA_COLUMN_FORMATTED_RESULT + " TEXT NOT NULL, " +
            TABLE_OBD_DATA_COLUMN_CALCULATED_RESULT + " TEXT NOT NULL, " +
            TABLE_OBD_DATA_COLUMN_RESULT_UNIT + " TEXT NOT NULL," +
            TABLE_OBD_DATA_COLUMN_VIN + " TEXT NOT NULL" +
        ");";

    public static final String TABLE_TROUBLE_CODES_RESULTS = "trouble_codes_results";
    public static final String TABLE_TROUBLE_CODES_RESULTS_COLUMN_DATE = "date";
    public static final String TABLE_TROUBLE_CODES_RESULTS_COLUMN_VIN = "vin";
    public static final String TABLE_TROUBLE_CODES_RESULTS_COLUMN_TYPE = "type";
    public static final String TABLE_TROUBLE_CODES_RESULTS_COLUMN_TROUBLE_CODES_LIST = "trouble_codes_list";
    public static final String TABLE_TROUBLE_CODES_RESULTS_COLUMN_TROUBLE_CODES_LIST_SEPARATOR = ";";

    private static final String SQL_CREATE_TABLE_TROUBLE_CODES_RESULTS =
        "CREATE TABLE " + TABLE_TROUBLE_CODES_RESULTS + "(" +
            TABLE_TROUBLE_CODES_RESULTS_COLUMN_DATE + " TEXT PRIMARY KEY NOT NULL, " +
            TABLE_TROUBLE_CODES_RESULTS_COLUMN_VIN + " TEXT NOT NULL, " +
            TABLE_TROUBLE_CODES_RESULTS_COLUMN_TYPE + " TEXT NOT NULL, " +
            TABLE_TROUBLE_CODES_RESULTS_COLUMN_TROUBLE_CODES_LIST + " TEXT NOT NULL" +
        ");";

    public static final String TABLE_VIN_MAPPINGS = "vin_mappings";
    public static final String TABLE_VIN_MAPPINGS_COLUMN_VIN = "vin";
    public static final String TABLE_VIN_MAPPINGS_COLUMN_NAME = "name";

    private static final String SQL_CREATE_TABLE_VIN_MAPPINGS =
        "CREATE TABLE " + TABLE_VIN_MAPPINGS + "(" +
            TABLE_VIN_MAPPINGS_COLUMN_VIN + " TEXT PRIMARY KEY NOT NULL, " +
            TABLE_VIN_MAPPINGS_COLUMN_NAME + " TEXT NOT NULL" +
        ");";

    public static synchronized Obd2FunDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new Obd2FunDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private Obd2FunDatabaseHelper(Context context) {
        super(context, DB_FILE_NAME, null, DB_VERSION);
        Timber.d("Opened database: %s", getDatabaseName());
        Timber.d("Enabling write ahead logging on sqlite database");
        getWritableDatabase().enableWriteAheadLogging();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Timber.d("onCreate called");
        try {
            Timber.d("Creating table %s %s %s", TABLE_OBD_DATA, "in database", getDatabaseName());
            db.execSQL(SQL_CREATE_TABLE_OBD_DATA);
            Timber.d("Creating table %s %s %s", TABLE_TROUBLE_CODES_RESULTS, "in database", getDatabaseName());
            db.execSQL(SQL_CREATE_TABLE_TROUBLE_CODES_RESULTS);
            Timber.d("Creating table %s %s %s", TABLE_VIN_MAPPINGS, "in database", getDatabaseName());
            db.execSQL(SQL_CREATE_TABLE_VIN_MAPPINGS);
        } catch (Exception e) {
            Timber.e(e, "Error while creating table");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Timber.d("onUpgrade called");
    }
}
