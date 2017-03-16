package com.github.zeroxfelix.obd2fun.sql;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.github.zeroxfelix.obd2fun.Obd2FunApplication;
import com.github.zeroxfelix.obd2fun.R;

public class TroubleCodesResult implements Parcelable {

    private static final String HUMAN_READABLE_DATE_FORMAT = "dd.MM.yyyy HH:mm:ss";

    private final Date date;
    private final String vin;
    private final Type type;
    private final List<String> troubleCodesList;

    public TroubleCodesResult(Date date, String vin, Type type, List<String> troubleCodesList) {
        this.date = date;
        this.vin = vin;
        this.type = type;
        this.troubleCodesList = troubleCodesList;
    }

    public Date getDate() {
        return date;
    }

    public String getVin() {
        return vin;
    }

    public Type getType() {
        return type;
    }

    public List<String> getTroubleCodesList() {
        return troubleCodesList;
    }

    public String getFormattedDate() {
        return new SimpleDateFormat(HUMAN_READABLE_DATE_FORMAT, Locale.getDefault()).format(date);
    }

    @Override
    public String toString() {
        String nameForVin = Obd2FunApplication.getNameForVin(vin);
        if (nameForVin == null) {
            nameForVin = Obd2FunApplication.getResourceString(R.string.unknown_vin_name);
        }
        return String.format(Obd2FunApplication.getResourceString(R.string.trouble_codes_result_to_string_template), getFormattedDate(), nameForVin, Obd2FunApplication.getResourceString(type.getStringResourceId()));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(date != null ? date.getTime() : -1);
        dest.writeString(this.vin);
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeStringList(this.troubleCodesList);
    }

    private TroubleCodesResult(Parcel in) {
        long tmpDate = in.readLong();
        this.date = tmpDate == -1 ? null : new Date(tmpDate);
        this.vin = in.readString();
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : Type.values()[tmpType];
        this.troubleCodesList = in.createStringArrayList();
    }

    public static final Creator<TroubleCodesResult> CREATOR = new Creator<TroubleCodesResult>() {
        @Override
        public TroubleCodesResult createFromParcel(Parcel source) {
            return new TroubleCodesResult(source);
        }

        @Override
        public TroubleCodesResult[] newArray(int size) {
            return new TroubleCodesResult[size];
        }
    };

    public enum Type {
        NORMAL(R.string.trouble_codes_type_normal),
        PENDING(R.string.trouble_codes_type_pending),
        PERMANENT(R.string.trouble_codes_type_permanent);

        private final int stringResourceId;

        Type(int stringResourceId) {
            this.stringResourceId = stringResourceId;
        }

        public final int getStringResourceId() {
            return stringResourceId;
        }

        public static Type getValueForName(String name) {
            for (Type type : Type.values()) {
                if (type.name().equals(name)) {
                    return type;
                }
            }
            return null;
        }
    }
}
