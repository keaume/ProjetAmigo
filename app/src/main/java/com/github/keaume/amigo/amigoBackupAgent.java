package com.github.keaume.amigo;

import android.app.backup.BackupAgentHelper;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;

import com.github.keaume.amigo.sql.amigoDatabaseHelper;
import timber.log.Timber;

public class amigoBackupAgent extends BackupAgentHelper {

    private static final String DEFAULT_SHARED_PREFERENCES = "com.github.keaume.amigo_preferences";
    private static final String DEFAULT_SHARED_PREFERENCES_BACKUP_KEY = "default_shared_preferences";
    private static final String DATABASE_BACKUP_KEY = "database";

    @Override
    public void onCreate() {
        Timber.d("onCreate called");
        addHelper(DEFAULT_SHARED_PREFERENCES_BACKUP_KEY, new SharedPreferencesBackupHelper(this, DEFAULT_SHARED_PREFERENCES));
        addHelper(DATABASE_BACKUP_KEY, new DbBackupHelper(this, amigoDatabaseHelper.DB_FILE_NAME));
    }

    private class DbBackupHelper extends FileBackupHelper {
        public DbBackupHelper(Context ctx, String dbName) {
            super(ctx, ctx.getDatabasePath(dbName).getAbsolutePath());
        }
    }
}
