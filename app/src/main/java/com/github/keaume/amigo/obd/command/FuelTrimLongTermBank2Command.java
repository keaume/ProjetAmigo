package com.github.keaume.amigo.obd.command;

import com.github.pires.obd.commands.fuel.FuelTrimCommand;
import com.github.pires.obd.enums.FuelTrim;

public class FuelTrimLongTermBank2Command extends FuelTrimCommand {

    public FuelTrimLongTermBank2Command() {
        super(FuelTrim.LONG_TERM_BANK_2);
    }

}