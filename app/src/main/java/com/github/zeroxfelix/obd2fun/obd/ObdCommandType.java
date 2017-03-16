package com.github.zeroxfelix.obd2fun.obd;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.control.DistanceMILOnCommand;
import com.github.pires.obd.commands.control.DistanceSinceCCCommand;
import com.github.pires.obd.commands.control.DtcNumberCommand;
import com.github.pires.obd.commands.control.EquivalentRatioCommand;
import com.github.pires.obd.commands.control.IgnitionMonitorCommand;
import com.github.pires.obd.commands.control.ModuleVoltageCommand;
import com.github.pires.obd.commands.control.TimingAdvanceCommand;
import com.github.pires.obd.commands.control.VinCommand;
import com.github.pires.obd.commands.engine.AbsoluteLoadCommand;
import com.github.pires.obd.commands.engine.LoadCommand;
import com.github.pires.obd.commands.engine.MassAirFlowCommand;
import com.github.pires.obd.commands.engine.OilTempCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.engine.RuntimeCommand;
import com.github.pires.obd.commands.engine.ThrottlePositionCommand;
import com.github.pires.obd.commands.fuel.AirFuelRatioCommand;
import com.github.pires.obd.commands.fuel.ConsumptionRateCommand;
import com.github.pires.obd.commands.fuel.FindFuelTypeCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.fuel.WidebandAirFuelRatioCommand;
import com.github.pires.obd.commands.pressure.BarometricPressureCommand;
import com.github.pires.obd.commands.pressure.FuelPressureCommand;
import com.github.pires.obd.commands.pressure.FuelRailPressureCommand;
import com.github.pires.obd.commands.pressure.IntakeManifoldPressureCommand;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_01_20;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_21_40;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_41_60;
import com.github.pires.obd.commands.protocol.DescribeProtocolCommand;
import com.github.pires.obd.commands.protocol.DescribeProtocolNumberCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.ResetTroubleCodesCommand;
import com.github.pires.obd.commands.temperature.AirIntakeTemperatureCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;

import com.github.zeroxfelix.obd2fun.obd.command.CleanResponsePendingTroubleCodesCommand;
import com.github.zeroxfelix.obd2fun.obd.command.CleanResponsePermanentTroubleCodesCommand;
import com.github.zeroxfelix.obd2fun.obd.command.CleanResponseTroubleCodesCommand;
import com.github.zeroxfelix.obd2fun.obd.command.FuelTrimLongTermBank1Command;
import com.github.zeroxfelix.obd2fun.obd.command.FuelTrimLongTermBank2Command;
import com.github.zeroxfelix.obd2fun.obd.command.FuelTrimShortTermBank1Command;
import com.github.zeroxfelix.obd2fun.obd.command.FuelTrimShortTermBank2Command;
import com.github.zeroxfelix.obd2fun.obd.command.SimpleAdaptiveTimingCommand;
import com.github.zeroxfelix.obd2fun.obd.command.SimpleSelectProtocolCommand;

public enum ObdCommandType {

    ABSOLUTE_LOAD(AbsoluteLoadCommand.class, "Absolute Load", true, true, true),
    AIR_FUEL_RATIO(AirFuelRatioCommand.class, "Air/Fuel Ratio", true, false, true),
    AIR_INTAKE_TEMP(AirIntakeTemperatureCommand.class, "Air Intake Temperature", true, false, true),
    AMBIENT_AIR_TEMP(AmbientAirTemperatureCommand.class, "Ambient Air Temperature", true, false, true),
    BAROMETRIC_PRESSURE(BarometricPressureCommand.class, "Barometric Pressure", true, false, true),
    CONTROL_MODULE_VOLTAGE(ModuleVoltageCommand.class, "Control Module Power Supply", true, false, true),
    DESCRIBE_PROTOCOL(DescribeProtocolCommand.class, "Describe Protocol", false, false, false),
    DESCRIBE_PROTOCOL_NUMBER(DescribeProtocolNumberCommand.class, "Describe Protocol Number", false, false, false),
    DISTANCE_TRAVELED_AFTER_CODES_CLEARED(DistanceSinceCCCommand.class, "Distance since codes cleared", true, false, false),
    DISTANCE_TRAVELED_MIL_ON(DistanceMILOnCommand.class, "Distance traveled with MIL on", true, false, false),
    DTC_NUMBER(DtcNumberCommand.class, "Diagnostic Trouble Codes", false, false, false),
    ECHO_OFF(EchoOffCommand.class, "Echo Off", false, false, false),
    ENGINE_COOLANT_TEMP(EngineCoolantTemperatureCommand.class, "Engine Coolant Temperature", true, false, true),
    ENGINE_LOAD(LoadCommand.class, "Engine Load", true, true, true),
    ENGINE_OIL_TEMP(OilTempCommand.class, "Engine Oil Temperature", true, false, true),
    ENGINE_RPM(RPMCommand.class, "Engine RPM", true, false, true),
    ENGINE_RUNTIME(RuntimeCommand.class, "Engine Runtime", true, false, false),
    EQUIVALENT_RATIO(EquivalentRatioCommand.class, "Command Equivalence Ratio", true, false, true),
    FUEL_CONSUMPTION_RATE(ConsumptionRateCommand.class, "Fuel Consumption Rate", true, false, true),
    FUEL_LEVEL(FuelLevelCommand.class, "Fuel Level", true, true, true),
    FUEL_PRESSURE(FuelPressureCommand.class, "Fuel Pressure", true, false, true),
    FUEL_RAIL_PRESSURE(FuelRailPressureCommand.class, "Fuel Rail Pressure", true, false, true),
    FUEL_TRIM_LONG_TERM_BANK_1(FuelTrimLongTermBank1Command.class, "Fuel Trim Long Term Bank 1", true, true, true),
    FUEL_TRIM_LONG_TERM_BANK_2(FuelTrimLongTermBank2Command.class, "Fuel Trim Long Term Bank 2", true, true, true),
    FUEL_TRIM_SHORT_TERM_BANK_1(FuelTrimShortTermBank1Command.class, "Fuel Trim Short Term Bank 1", true, true, true),
    FUEL_TRIM_SHORT_TERM_BANK_2(FuelTrimShortTermBank2Command.class, "Fuel Trim Short Term Bank 2", true, true, true),
    FUEL_TYPE(FindFuelTypeCommand.class, "Fuel Type", true, false, false),
    IGNITION_MONITOR(IgnitionMonitorCommand.class, "Ignition Monitor", true, false, false),
    INTAKE_MANIFOLD_PRESSURE(IntakeManifoldPressureCommand.class, "Intake Manifold Pressure", true, false, true),
    LINE_FEED_OFF(LineFeedOffCommand.class, "Line Feed Off", false, false, false),
    MASS_AIR_FLOW(MassAirFlowCommand.class, "Mass Air Flow", true, false, true),
    PENDING_TROUBLE_CODES(CleanResponsePendingTroubleCodesCommand.class, "Pending Trouble Codes", false, false, false),
    PERMANENT_TROUBLE_CODES(CleanResponsePermanentTroubleCodesCommand.class, "Permanent Trouble Codes", false, false, false),
    PIDS_01_20(AvailablePidsCommand_01_20.class, "Available PIDs 01-20", false, false, false),
    PIDS_21_40(AvailablePidsCommand_21_40.class, "Available PIDs 21-40", false, false, false),
    PIDS_41_60(AvailablePidsCommand_41_60.class, "Available PIDs 41-60", false, false, false),
    RESET_OBD(ObdResetCommand.class, "Reset OBD", false, false, false),
    RESET_TROUBLE_CODES(ResetTroubleCodesCommand.class, "Reset Trouble Codes", false, false, false),
    SELECT_PROTOCOL(SimpleSelectProtocolCommand.class, "Select Protocol", false, false, false),
    SPEED(SpeedCommand.class, "Vehicle Speed", true, false, true),
    THROTTLE_POS(ThrottlePositionCommand.class, "Throttle Position", true, true, true),
    ADAPTIVE_TIMING(SimpleAdaptiveTimingCommand.class, "Adaptive Timing", false, false, false),
    TIMING_ADVANCE(TimingAdvanceCommand.class, "Timing Advance", true, false, true),
    TROUBLE_CODES(CleanResponseTroubleCodesCommand.class, "Trouble Codes", false, false, false),
    VIN(VinCommand.class, "Vehicle Identification Number (VIN)", false, false, false),
    WIDEBAND_AIR_FUEL_RATIO(WidebandAirFuelRatioCommand.class, "Wideband Air/Fuel Ratio", true, false, true);

    private final Class clazz;
    private final String name;
    private final boolean isRecordable;
    private final boolean isPercentage;
    private final boolean isGraphable;

    public static ObdCommandType getValueForName(String name) {
        for (ObdCommandType obdCommandType : ObdCommandType.values()) {
            if (obdCommandType.getNameForValue().equals(name)) {
                return obdCommandType;
            }
        }
        return null;
    }

    ObdCommandType(Class clazz, String name, boolean isRecordable, boolean isPercentage, boolean isGraphable) {
        this.clazz = clazz;
        this.name = name;
        this.isRecordable = isRecordable;
        this.isPercentage = isPercentage;
        this.isGraphable = isGraphable;
    }

    public final Class getClassForValue() {
        return clazz;
    }

    public final String getNameForValue() {
        return name;
    }

    public final String toString(){
        return name;
    }

    public final boolean getIsRecordable() {
        return isRecordable;
    }

    public final boolean getIsPercentage() {
        return isPercentage;
    }

    public final boolean getIsGraphable() {
        return isGraphable;
    }
}
