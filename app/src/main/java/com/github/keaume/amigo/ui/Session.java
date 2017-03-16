package com.github.keaume.amigo.ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.github.keaume.amigo.amigoApplication;
import com.github.keaume.amigo.R;
import com.github.keaume.amigo.obd.ObdCommandType;
import com.github.keaume.amigo.sql.amigoDataSource;

public class Session {
    private final String sessionId;
    private List<ObdCommandType> obdCommandTypes = new ArrayList<>();
    private final String dateString;
    private final String vehicle;

    public Session(String sessionId, amigoDataSource amigoFunDataSource){
        this.sessionId = sessionId;

        this.obdCommandTypes = amigoDataSource.getCommandTypesForSession(sessionId);
        this.dateString = sessionToDate(sessionId);
        String vehicleTemp = amigoDataSource.getNameForVin(amigoDataSource.getVinForSession(sessionId));
        if (vehicleTemp != null) {
            this.vehicle = vehicleTemp;
        } else {
            this.vehicle = amigoApplication.getResourceString(R.string.unknown_vin_name);
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
