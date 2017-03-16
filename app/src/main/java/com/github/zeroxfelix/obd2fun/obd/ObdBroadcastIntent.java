package com.github.zeroxfelix.obd2fun.obd;

import android.content.Intent;

public final class ObdBroadcastIntent {

    public static final String OBD_CONNECTION_STATE = "ObdConnectionState";
    public static final String MANAGE_OBD_COMMAND_JOB = "ManageObdCommandJob";
    public static final String VIN_CHANGED = "VinChanged";

    private ObdBroadcastIntent() {

    }

    public static Intent getObdConnectionStateIntent(ObdConnectionState obdConnectionState) {
        Intent intent = new Intent(OBD_CONNECTION_STATE);
        intent.putExtra("obdConnectionState", obdConnectionState);
        return intent;
    }

    public static Intent getRegisterPeriodicObdCommandJobIntent(ObdCommandType obdCommandType) {
        Intent intent = new Intent(MANAGE_OBD_COMMAND_JOB);
        intent.putExtra("obdCommandType", obdCommandType);
        intent.putExtra("obdCommandJobAction", ObdCommandJob.Action.REGISTER_PERIODIC);
        return intent;
    }

    public static Intent getUnregisterPeriodicObdCommandJobIntent(ObdCommandType obdCommandType) {
        Intent intent = new Intent(MANAGE_OBD_COMMAND_JOB);
        intent.putExtra("obdCommandType", obdCommandType);
        intent.putExtra("obdCommandJobAction", ObdCommandJob.Action.UNREGISTER_PERIODIC);
        return intent;
    }

    public static Intent getRegisterOneShotObdCommandJobIntent(ObdCommandType obdCommandType) {
        Intent intent = new Intent(MANAGE_OBD_COMMAND_JOB);
        intent.putExtra("obdCommandType", obdCommandType);
        intent.putExtra("obdCommandJobAction", ObdCommandJob.Action.REGISTER_ONE_SHOT);
        return intent;
    }

    public static Intent getObdCommandJobResultIntent(ObdCommandJob obdCommandJob, String vin, long sessionId) {
        Intent intent = new Intent(obdCommandJob.getCommandType().getNameForValue());
        intent.putExtra("obdCommandJobResult", new ObdCommandJobResult(obdCommandJob, vin, sessionId));
        return intent;
    }

    public static Intent getVinChangedIntent(String newVin) {
        Intent intent = new Intent(VIN_CHANGED);
        intent.putExtra("newVin", newVin);
        return intent;
    }

}
