package com.github.zeroxfelix.obd2fun.obd.command;

import com.github.pires.obd.commands.fuel.FuelTrimCommand;
import com.github.pires.obd.enums.FuelTrim;

public class FuelTrimShortTermBank2Command extends FuelTrimCommand {

    public FuelTrimShortTermBank2Command() {
        super(FuelTrim.SHORT_TERM_BANK_2);
    }

}