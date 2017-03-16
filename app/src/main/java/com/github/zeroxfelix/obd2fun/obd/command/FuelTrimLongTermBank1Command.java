package com.github.zeroxfelix.obd2fun.obd.command;

import com.github.pires.obd.commands.fuel.FuelTrimCommand;
import com.github.pires.obd.enums.FuelTrim;

public class FuelTrimLongTermBank1Command extends FuelTrimCommand {

    public FuelTrimLongTermBank1Command() {
        super(FuelTrim.LONG_TERM_BANK_1);
    }

}