package org.louiswilliams.phcontroller;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.Observable;
import android.support.v4.view.ViewPager;

import java.util.HashMap;
import java.util.Map;

public class CarData extends BaseObservable {

    public static final String BATT_VOLTAGE = "Battery Voltage (V)";
    public static final String BATT_CURRENT = "Battery Current (A)";
    public static final String BATT_AMPHRS = "Battery Amp-Hours (Ah)";
    public static final String BATT_SOC = "Battery SOC (%)";
    public static final String BATT_TIME = "Battery Time Left (s)";
    public static final String BATT_TEMP = "Battery Temp (C)";
    public static final String MOTOR_RPM = "Motor RPM (cycles/m)";
    public static final String MOTOR_TEMP = "Motor Temp (C)";
    public static final String MOTOR_CURRENT = "Motor Current (A)";
    public static final String MOTOR_VOLTAGE = "Motor Voltage (V)";
    public static final String MOTOR_STATOR = "Stator Frequency (Hz)";
    public static final String ENGINE_RPM = "Engine RPM (cycles/m)";
    public static final String ENGINE_FUEL = "Engine Fuel (?)";
    public static final String MODE = "Mode";

    private static Map<String, String> uuids;
    private static Map<String, Integer> brIds;
    private Map<String, Integer> values;

    public CarData() {
        values = new HashMap<>();
        if (uuids == null) {
            uuids = new HashMap<>();

            uuids.put(BATT_VOLTAGE, "00001001-0000-1000-8000-00805F9B34FB");
            uuids.put(BATT_CURRENT, "00001002-0000-1000-8000-00805F9B34FB");
            uuids.put(BATT_AMPHRS, "00001003-0000-1000-8000-00805F9B34FB");
            uuids.put(BATT_SOC, "00001004-0000-1000-8000-00805F9B34FB");
            uuids.put(BATT_TIME, "00001005-0000-1000-8000-00805F9B34FB");
            uuids.put(BATT_TEMP, "00001006-0000-1000-8000-00805F9B34FB");
            uuids.put(MOTOR_RPM, "00001007-0000-1000-8000-00805F9B34FB");
            uuids.put(MOTOR_TEMP, "00001008-0000-1000-8000-00805F9B34FB");
            uuids.put(MOTOR_CURRENT, "00001009-0000-1000-8000-00805F9B34FB");
            uuids.put(MOTOR_VOLTAGE, "0000100A-0000-1000-8000-00805F9B34FB");
            uuids.put(MOTOR_STATOR, "0000100B-0000-1000-8000-00805F9B34FB");
            uuids.put(ENGINE_RPM, "0000100C-0000-1000-8000-00805F9B34FB");
            uuids.put(ENGINE_FUEL, "0000100D-0000-1000-8000-00805F9B34FB");
            uuids.put(MODE, "0000100E-0000-1000-8000-00805F9B34FB");
        }
        if (brIds == null) {
            brIds = new HashMap<>();

            brIds.put(uuids.get(BATT_VOLTAGE), BR.battVoltage);
            brIds.put(uuids.get(BATT_CURRENT), BR.battCurrent);
            brIds.put(uuids.get(BATT_AMPHRS), BR.battAmpHrs);
            brIds.put(uuids.get(BATT_SOC), BR.battSoc);
            brIds.put(uuids.get(BATT_TIME), BR.battTimeLeft);
            brIds.put(uuids.get(BATT_TEMP), BR.battTemp);
            brIds.put(uuids.get(MOTOR_RPM), BR.motorRpm);
            brIds.put(uuids.get(MOTOR_TEMP), BR.motorTemp);
            brIds.put(uuids.get(MOTOR_CURRENT), BR.motorCurrent);
            brIds.put(uuids.get(MOTOR_VOLTAGE), BR.motorVoltage);
            brIds.put(uuids.get(MOTOR_STATOR), BR.motorStatorFreq);
            brIds.put(uuids.get(ENGINE_RPM), BR.engineRpm);
            brIds.put(uuids.get(ENGINE_FUEL), BR.engineFuelRate);
//            brIds.put(uuids.get(MODE), BR.mode);

        }

        values.put(uuids.get(BATT_VOLTAGE), 0);
        values.put(uuids.get(BATT_CURRENT), 0);
        values.put(uuids.get(BATT_AMPHRS), 0);
        values.put(uuids.get(BATT_SOC), 0);
        values.put(uuids.get(BATT_TIME), 0);
        values.put(uuids.get(BATT_TEMP), 0);
        values.put(uuids.get(MOTOR_RPM), 0);
        values.put(uuids.get(MOTOR_TEMP), 0);
        values.put(uuids.get(MOTOR_CURRENT), 0);
        values.put(uuids.get(MOTOR_VOLTAGE), 0);
        values.put(uuids.get(MOTOR_STATOR), 0);
        values.put(uuids.get(ENGINE_RPM), 0);
        values.put(uuids.get(ENGINE_FUEL), 0);
        values.put(uuids.get(MODE), 0);
    }

    public Map<String, String> getUuids() {
        return uuids;
    }

    public void setByName(String key, int value) {
        setByUUID(uuids.get(key), value);
    }

    public void setByUUID(String uuid, int value) {
        values.put(uuid.toUpperCase(), value);
        Integer id = brIds.get(uuid.toUpperCase());
        if (id != null) {
            notifyPropertyChanged(id);
        }
    }

    @Bindable
    public double getBattVoltage() {
        return values.get(uuids.get(BATT_VOLTAGE)) / 100.0;
    }

    @Bindable
    public double getBattCurrent() {
        return values.get(uuids.get(BATT_CURRENT))/100.0;
    }

    @Bindable
    public double getBattAmpHrs() {
        return values.get(uuids.get(BATT_AMPHRS))/ 10.0;
    }

    @Bindable
    public double getBattSoc() {
        return values.get(uuids.get(BATT_SOC)) / 10.0;
    }

    @Bindable
    public double getBattTimeLeft() {
        return values.get(uuids.get(BATT_TIME));
    }

    @Bindable
    public double getBattTemp() {
        return values.get(uuids.get(BATT_TEMP))/ 256.0;
    }

    @Bindable
    public double getMotorRpm() {
        return values.get(uuids.get(MOTOR_RPM));
    }

    @Bindable
    public double getMotorTemp() {
        return values.get(uuids.get(MOTOR_TEMP))- 40;
    }

    @Bindable
    public double getMotorCurrent() {
        return values.get(uuids.get(MOTOR_CURRENT)) / 10.0;
    }

    @Bindable
    public double getMotorVoltage() {
        return values.get(uuids.get(MOTOR_CURRENT)) / 10.0;
    }

    @Bindable
    public double getMotorStatorFreq() {
        return values.get(uuids.get(MOTOR_STATOR));
    }

    @Bindable
    public double getEngineRpm() {
        return values.get(uuids.get(ENGINE_RPM));
    }

    @Bindable
    public double getEngineFuelRate() {
        return values.get(uuids.get(ENGINE_FUEL));
    }

    public String[] getColumns() {
        return (String[]) uuids.keySet().toArray();
    }

    public String[] getRow() {
        String row[] = new String[uuids.size()];

        String cols[] = getColumns();
        for (int i=0; i < cols.length; i++) {
            row[i] = String.valueOf(values.get(cols[i]));
        }

        return row;
    }
}
