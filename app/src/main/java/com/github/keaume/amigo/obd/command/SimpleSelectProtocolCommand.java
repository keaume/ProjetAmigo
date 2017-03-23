package com.github.keaume.amigo.obd.command;

import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.enums.ObdProtocols;

import com.github.keaume.amigo.AmigoApplication;
import com.github.keaume.amigo.R;
import com.github.keaume.amigo.fragment.SettingsFragment;

public class SimpleSelectProtocolCommand extends SelectProtocolCommand {

    public SimpleSelectProtocolCommand() {
        super(ObdProtocols.valueOf(AmigoApplication.getPreferenceString(SettingsFragment.OBD_PROTOCOL_KEY, AmigoApplication.getResourceString(R.string.obd_protocol_preference_default))));
    }

}
