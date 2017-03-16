package com.github.zeroxfelix.obd2fun.obd;

import com.github.pires.obd.commands.ObdCommand;

import timber.log.Timber;

public class ObdCommandJob {

    private long id;
    private final ObdCommandType obdCommandType;
    private ObdCommand obdCommand;
    private State obdCommandJobState;

    public ObdCommandJob(ObdCommandType obdCommandType) {
        id = 0L;
        this.obdCommandType = obdCommandType;
        try {
            Class<?> obdCommandClass = obdCommandType.getClassForValue();
            obdCommand = (ObdCommand) obdCommandClass.newInstance();
        } catch (Exception e) {
            Timber.e("Failed to instantiate ObdCommand, obdCommandJob type: %s", obdCommandType.getNameForValue());
            obdCommand = null;
        }
        obdCommandJobState = State.NEW;
    }

    public long getId() {
        return id;
    }

    public ObdCommandType getCommandType() {
        return obdCommandType;
    }

    public ObdCommand getCommand() {
        return obdCommand;
    }

    public State getState() {
        return obdCommandJobState;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setState(State obdCommandJobState) {
        this.obdCommandJobState = obdCommandJobState;
    }

    public enum State {
        NEW,
        RUNNING,
        FINISHED,
        UNABLE_TO_CONNECT,
        BUS_INIT_EXCEPTION,
        MISUNDERSTOOD_COMMAND,
        NO_DATA,
        STOPPED,
        NOT_SUPPORTED,
        EXECUTION_ERROR,
        TIMEOUT
    }

    public enum Action {
        REGISTER_PERIODIC,
        REGISTER_ONE_SHOT,
        UNREGISTER_PERIODIC,
    }
}
