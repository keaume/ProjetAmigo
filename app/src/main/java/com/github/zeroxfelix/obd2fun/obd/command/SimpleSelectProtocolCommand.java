package com.github.zeroxfelix.obd2fun.obd.command;

import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.enums.ObdProtocols;

import com.github.zeroxfelix.obd2fun.Obd2FunApplication;
import com.github.zeroxfelix.obd2fun.R;
import com.github.zeroxfelix.obd2fun.fragment.SettingsFragment;

public class SimpleSelectProtocolCommand extends SelectProtocolCommand {

    public SimpleSelectProtocolCommand() {
        super(ObdProtocols.valueOf(Obd2FunApplication.getPreferenceString(SettingsFragment.OBD_PROTOCOL_KEY, Obd2FunApplication.getResourceString(R.string.obd_protocol_preference_default))));
    }

}
