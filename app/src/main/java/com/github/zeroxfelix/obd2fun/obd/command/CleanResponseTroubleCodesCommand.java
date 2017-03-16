package com.github.zeroxfelix.obd2fun.obd.command;

import com.github.pires.obd.commands.control.TroubleCodesCommand;

public class CleanResponseTroubleCodesCommand extends TroubleCodesCommand {

    @Override
    public String getResult() {
        if (rawData != null) {
            return rawData.replace("SEARCHING...", "").replace("NODATA", "");
        } else {
            return null;
        }
    }
}
