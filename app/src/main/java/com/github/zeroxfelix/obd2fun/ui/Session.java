package com.github.zeroxfelix.obd2fun.ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.github.zeroxfelix.obd2fun.Obd2FunApplication;
import com.github.zeroxfelix.obd2fun.R;
import com.github.zeroxfelix.obd2fun.obd.ObdCommandType;
import com.github.zeroxfelix.obd2fun.sql.Obd2FunDataSource;

public class Session {
    private final String sessionId;
    private List<ObdCommandType> obdCommandTypes = new ArrayList<>();
    private final String dateString;
    private final String vehicle;

    public Session(String sessionId, Obd2FunDataSource obd2FunDataSource){
        this.sessionId = sessionId;

        this.obdCommandTypes = obd2FunDataSource.getCommandTypesForSession(sessionId);
        this.dateString = sessionToDate(sessionId);
        String vehicleTemp = obd2FunDataSource.getNameForVin(obd2FunDataSource.getVinForSession(sessionId));
        if (vehicleTemp != null) {
            this.vehicle = vehicleTemp;
        } else {
            this.vehicle = Obd2FunApplication.getResourceString(R.string.unknown_vin_name);
        }
    }

    public List<ObdCommandType> getObdCommandTypes(){
        return this.obdCommandTypes;
    }

    public String getSessionId(){
        return this.sessionId;
    }

    public String toString(){
        return this.dateString + " - " + this.vehicle;
    }

    private String sessionToDate(String session){
        long sessionLong =  Long.parseLong(session);
        Date date = new Date(sessionLong);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY);
        return sdf.format(date);
    }

}
