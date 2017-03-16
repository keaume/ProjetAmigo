package com.github.zeroxfelix.obd2fun.sql;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

import com.github.zeroxfelix.obd2fun.obd.ObdCommandType;

public class ObdData implements Parcelable {

    private final long sessionId;
    private final Date recordingDate;
    private final ObdCommandType obdCommandType;
    private final String rawResult;
    private final String formattedResult;
    private final String calculatedResult;
    private final String resultUnit;
    private final String vin;

    public ObdData(long sessionId, Date recordingDate, ObdCommandType obdCommandType, String rawResult, String formattedResult, String calculatedResult, String resultUnit, String vin) {
        this.sessionId = sessionId;
        this.recordingDate = recordingDate;
        this.obdCommandType = obdCommandType;
        this.rawResult = rawResult;
        this.formattedResult = formattedResult;
        this.calculatedResult = calculatedResult;
        this.resultUnit = resultUnit;
        this.vin = vin;
    }

    public long getSessionId() {
        return sessionId;
    }

    public Date getRecordingDate() {
        return recordingDate;
    }

    public ObdCommandType getObdCommandType() {
        return obdCommandType;
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
        dest.writeLong(this.sessionId);
        dest.writeLong(recordingDate != null ? recordingDate.getTime() : -1);
        dest.writeInt(this.obdCommandType == null ? -1 : this.obdCommandType.ordinal());
        dest.writeString(this.rawResult);
        dest.writeString(this.formattedResult);
        dest.writeString(this.calculatedResult);
        dest.writeString(this.resultUnit);
        dest.writeString(this.vin);
    }

    private ObdData(Parcel in) {
        this.sessionId = in.readLong();
        long tmpRecordingDate = in.readLong();
        this.recordingDate = tmpRecordingDate == -1 ? null : new Date(tmpRecordingDate);
        int tmpObdCommandType = in.readInt();
        this.obdCommandType = tmpObdCommandType == -1 ? null : ObdCommandType.values()[tmpObdCommandType];
        this.rawResult = in.readString();
        this.formattedResult = in.readString();
        this.calculatedResult = in.readString();
        this.resultUnit = in.readString();
        this.vin = in.readString();
    }

    public static final Creator<ObdData> CREATOR = new Creator<ObdData>() {
        @Override
        public ObdData createFromParcel(Parcel source) {
            return new ObdData(source);
        }

        @Override
        public ObdData[] newArray(int size) {
            return new ObdData[size];
        }
    };
}
