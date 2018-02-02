package in.ashnehete.healthset.utils;

import android.content.Context;

import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothClassicService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothConfiguration;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService;

import java.util.UUID;

import static in.ashnehete.healthset.AppConstants.APP_NAME;

/**
 * Created by Aashish Nehete on 23-Jan-18.
 */

public class CustomBluetoothConfiguration {
    public static BluetoothService getBluetoothServiceInstance(Context context, UUID uuid) {
        BluetoothConfiguration config = new BluetoothConfiguration();
        config.context = context;
        config.bluetoothServiceClass = BluetoothClassicService.class; // BluetoothClassicService.class or BluetoothLeService.class
        config.bufferSize = 1024;
        config.characterDelimiter = '\n';
        config.deviceName = APP_NAME;
        config.callListenersInMainThread = false;
        // Classic BT
        config.uuid = uuid;

        BluetoothService.init(config);

        return BluetoothService.getDefaultInstance();
    }
}
