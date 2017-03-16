package com.github.zeroxfelix.obd2fun.obd;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.pires.obd.commands.ObdCommand;

import java.util.Date;

public class ObdCommandJobResult implements Parcelable {

    private final long id;
    private final long sessionId;
    private final Date date;
    private final ObdCommandType obdCommandType;
    private final ObdCommandJob.State obdCommandJobState;
    private final String rawResult;
    private final String formattedResult;
    private final String calculatedResult;
    private final String resultUnit;
    private final String vin;

    public ObdCommandJobResult(ObdCommandJob obdCommandJob, String vin, long sessionId) {
        id = obdCommandJob.getId();
        this.sessionId = sessionId;
        date = new Date();
        obdCommandType = obdCommandJob.getCommandType();
        obdCommandJobState = obdCommandJob.getState();
        final ObdCommand obdCommand = obdCommandJob.getCommand();
        if (obdCommand != null) {
            rawResult = obdCommand.getResult();
            formattedResult = obdCommand.getFormattedResult();
            calculatedResult = obdCommand.getCalculatedResult();
            resultUnit = obdCommand.getResultUnit();
            this.vin = vin;
        } else {
            rawResult = "null";
            formattedResult = "null";
            calculatedResult = "null";
            resultUnit = "null";
            this.vin = "null";
        }
    }

    public long getId() {
        return id;
    }

    public long getSessionId() {
        return sessionId;
    }

    public Date getDate() {
        return date;
    }

    public ObdCommandType getObdCommandType() {
        return obdCommandType;
    }

    public ObdCommandJob.State getState() {
        return obdCommandJobState;
    }

    public String getRawResult() {
        return rawResult;
    }

    public String getFormattedResult() {
        return formattedResult;
    }

    public String getCalculatedResult() {
        return calculatedResult;
    }

    public String getResultUnit() {
        return resultUnit;
    }

    public String getVin() {
        return vin;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeLong(this.sessionId);
        dest.writeLong(date != null ? date.getTime() : -1);
        dest.writeInt(this.obdCommandType == null ? -1 : this.obdCommandType.ordinal());
        dest.writeInt(this.obdCommandJobState == null ? -1 : this.obdCommandJobState.ordinal());
        dest.writeString(this.rawResult);
        dest.writeString(this.formattedResult);
        dest.writeString(this.calculatedResult);
        dest.writeString(this.resultUnit);
        dest.writeString(this.vin);
    }

    private ObdCommandJobResult(Parcel in) {
        this.id = in.readLong();
        this.sessionId = in.readLong();
        long tmpDate = in.readLong();
        this.date = tmpDate == -1 ? null : new Date(tmpDate);
        int tmpObdCommandType = in.readInt();
        this.obdCommandType = tmpObdCommandType == -1 ? null : ObdCommandType.values()[tmpObdCommandType];
        int tmpObdCommandJobState = in.readInt();
        this.obdCommandJobState = tmpObdCommandJobState == -1 ? null : ObdCommandJob.State.values()[tmpObdCommandJobState];
        this.rawResult = in.readString();
        this.formattedResult = in.readString();
        this.calculatedResult = in.readString();
        this.resultUnit = in.readString();
        this.vin = in.readString();
    }

    public static final Creator<ObdCommandJobResult> CREATOR = new Creator<ObdCommandJobResult>() {
        @Override
        public ObdCommandJobResult createFromParcel(Parcel source) {
            return new ObdCommandJobResult(source);
        }

        @Override
        public ObdCommandJobResult[] newArray(int size) {
            return new ObdCommandJobResult[size];
        }
    };
}
