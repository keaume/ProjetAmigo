package com.github.zeroxfelix.obd2fun;

import android.app.backup.BackupAgentHelper;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;

import com.github.zeroxfelix.obd2fun.sql.Obd2FunDatabaseHelper;
import timber.log.Timber;

public class Obd2FunBackupAgent extends BackupAgentHelper {

    private static final String DEFAULT_SHARED_PREFERENCES = "com.github.zeroxfelix.obd2fun_preferences";
    private static final String DEFAULT_SHARED_PREFERENCES_BACKUP_KEY = "default_shared_preferences";
    private static final String DATABASE_BACKUP_KEY = "database";

    @Override
    public void onCreate() {
        Timber.d("onCreate called");
        addHelper(DEFAULT_SHARED_PREFERENCES_BACKUP_KEY, new SharedPreferencesBackupHelper(this, DEFAULT_SHARED_PREFERENCES));
        addHelper(DATABASE_BACKUP_KEY, new DbBackupHelper(this, Obd2FunDatabaseHelper.DB_FILE_NAME));
    }

    private class DbBackupHelper extends FileBackupHelper {
        public DbBackupHelper(Context ctx, String dbName) {
            super(ctx, ctx.getDatabasePath(dbName).getAbsolutePath());
        }
    }
}
