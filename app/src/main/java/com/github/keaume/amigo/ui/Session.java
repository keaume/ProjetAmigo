package com.github.keaume.amigo.ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.github.keaume.amigo.AmigoApplication;
import com.github.keaume.amigo.R;
import com.github.keaume.amigo.obd.ObdCommandType;
import com.github.keaume.amigo.sql.AmigoDataSource;

public class Session {
    private final String sessionId;
    private List<ObdCommandType> obdCommandTypes = new ArrayList<>();
    private final String sessionDate;
    private final String vehicle;

    public Session(String sessionId, AmigoDataSource amigoFunDataSource){
        this.sessionId = sessionId;

        this.obdCommandTypes = amigoFunDataSource.getCommandTypesForSession(sessionId);
        this.sessionDate = sessionToDate(sessionId);
        String vehicleTemp = amigoFunDataSource.getNameForVin(amigoFunDataSource.getVinForSession(sessionId));
        if (vehicleTemp != null) {
            this.vehicle = vehicleTemp;
        } else {
            this.vehicle = AmigoApplication.getResourceString(R.string.unknown_vin_name);
        }
    }

    public List<ObdCommandType> getObdCommandTypes(){
        return this.obdCommandTypes;
    }

    public String getSessionId(){
        return this.sessionId;
    }

    public String toString(){
        return this.sessionDate + " - " + this.vehicle;
    }

    private String sessionToDate(String session){
        long sessionLong =  Long.parseLong(session);
        Date date = new Date(sessionLong);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.FRANCE);
        return sdf.format(date);
    }

}
