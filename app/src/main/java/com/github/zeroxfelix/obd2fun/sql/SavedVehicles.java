package com.github.zeroxfelix.obd2fun.sql;

import com.github.zeroxfelix.obd2fun.Obd2FunApplication;
import com.github.zeroxfelix.obd2fun.R;

public class SavedVehicles {
    private String name;
    private final String vin;

    public SavedVehicles(String name, String vin) {
        this.name = name;
        this.vin = vin;
    }

    public String getName() {
        return this.name;
    }

    public String getVin() {
        return this.vin;
    }

    public void refreshName() {
        name = Obd2FunApplication.getNameForVin(vin);
        if (name == null) {
            name = Obd2FunApplication.getResourceString(R.string.unknown_vin_name);
        }
    }
}
