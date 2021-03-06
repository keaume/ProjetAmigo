package com.github.keaume.amigo.obd.command;

import com.github.pires.obd.commands.control.PermanentTroubleCodesCommand;

public class CleanResponsePermanentTroubleCodesCommand extends PermanentTroubleCodesCommand {

    @Override
    public String getResult() {
        if (rawData != null) {
            return rawData.replace("SEARCHING...", "").replace("NODATA", "");
        } else {
            return null;
        }
    }
}
