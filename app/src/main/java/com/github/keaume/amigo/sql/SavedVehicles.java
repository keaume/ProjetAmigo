package com.github.keaume.amigo.sql;

import com.github.keaume.amigo.AmigoApplication;
import com.github.keaume.amigo.R;

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
        name = AmigoApplication.getNameForVin(vin);
        if (name == null) {
            name = AmigoApplication.getResourceString(R.string.unknown_vin_name);
        }
    }
}
