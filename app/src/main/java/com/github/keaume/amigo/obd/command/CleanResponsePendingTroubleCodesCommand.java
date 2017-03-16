package com.github.keaume.amigo.obd.command;

import com.github.pires.obd.commands.control.PendingTroubleCodesCommand;

public class CleanResponsePendingTroubleCodesCommand extends PendingTroubleCodesCommand {

    @Override
    public String getResult() {
        if (rawData != null) {
            return rawData.replace("SEARCHING...", "").replace("NODATA", "");
        } else {
            return null;
        }
    }
}
