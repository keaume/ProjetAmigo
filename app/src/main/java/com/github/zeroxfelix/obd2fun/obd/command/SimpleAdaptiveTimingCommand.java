package com.github.zeroxfelix.obd2fun.obd.command;

import com.github.pires.obd.commands.protocol.AdaptiveTimingCommand;

public class SimpleAdaptiveTimingCommand extends AdaptiveTimingCommand {

    //Enable AT1 Adaptive Timing
    private static final int ADAPTIVE_TIMING = 1;

    public SimpleAdaptiveTimingCommand() {
        super(ADAPTIVE_TIMING);
    }
}
