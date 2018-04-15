package in.ashnehete.healthset;

import java.util.UUID;

/**
 * Created by Aashish Nehete on 20-Dec-17.
 */

public class AppConstants {
    public static final String APP_NAME = "HealthSet";

    public static final String BT_DEVICE_NAME = "H-C-2010-06-01";
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    //
    public static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";

    // Devices
    public static final String ECG = "ECG";
    public static final String TEMPERATURE = "TEMPERATURE";
    public static final String PULSE = "PULSE";
    public static final String BEND = "BEND";
    public static final String GSR = "GSR";
}
