package com.github.zeroxfelix.obd2fun.obd.command;

import com.github.pires.obd.commands.fuel.FuelTrimCommand;
import com.github.pires.obd.enums.FuelTrim;

public class FuelTrimShortTermBank1Command extends FuelTrimCommand {

    public FuelTrimShortTermBank1Command() {
        super(FuelTrim.SHORT_TERM_BANK_1);
    }

}