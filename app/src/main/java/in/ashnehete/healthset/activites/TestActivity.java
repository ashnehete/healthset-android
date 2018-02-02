package in.ashnehete.healthset.activites;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothClassicService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothConfiguration;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothWriter;

import java.util.Set;

import in.ashnehete.healthset.R;

import static in.ashnehete.healthset.AppConstants.BT_DEVICE_NAME;
import static in.ashnehete.healthset.AppConstants.MY_UUID;

public class TestActivity extends AppCompatActivity
        implements BluetoothService.OnBluetoothEventCallback,
        BluetoothService.OnBluetoothScanCallback {

    public static final String TAG = "TestActivity";
    private BluetoothDevice mBluetoothDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothService mBluetoothService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        BluetoothConfiguration config = new BluetoothConfiguration();
        config.context = getApplicationContext();
        config.bluetoothServiceClass = BluetoothClassicService.class; // BluetoothClassicService.class or BluetoothLeService.class
        config.bufferSize = 1024;
        config.characterDelimiter = '\n';
        config.deviceName = "HealthSet";
        config.callListenersInMainThread = true;
        // Classic BT
        config.uuid = MY_UUID;

        BluetoothService.init(config);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothService = BluetoothService.getDefaultInstance();

        mBluetoothService.setOnEventCallback(this);
        mBluetoothService.setOnScanCallback(this);

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.i(TAG, deviceName + " " + deviceHardwareAddress);

                if (BT_DEVICE_NAME.equals(deviceName)) {
                    mBluetoothDevice = device;
                    break;
                }
            }
        }

        if (mBluetoothDevice != null) {
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothService.connect(mBluetoothDevice);
        } else {
            Toast.makeText(this, "Device not found.", Toast.LENGTH_SHORT).show();
        }
    }

    public void requestData(View view) {
        Log.d(TAG, "write (e)");
        try {
            BluetoothWriter writer = new BluetoothWriter(mBluetoothService);
            writer.writeln("t");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDataRead(byte[] bytes, int i) {
        Log.d(TAG, "onDataRead: " + new String(bytes, 0, i));
    }

    @Override
    public void onStatusChange(BluetoothStatus bluetoothStatus) {
        Log.d(TAG, "onStatusChange: " + bluetoothStatus.toString());
    }

    @Override
    public void onDeviceName(String s) {
        Log.d(TAG, "onDeviceName: " + s);
    }

    @Override
    public void onToast(String s) {
        Log.d(TAG, "onToast: " + s);
    }

    @Override
    public void onDataWrite(byte[] bytes) {
        Log.d(TAG, "onWrite: " + new String(bytes));
    }

    @Override
    public void onDeviceDiscovered(BluetoothDevice bluetoothDevice, int i) {
        Log.d(TAG, "onDeviceDiscovered: " + bluetoothDevice.toString());
        if (BT_DEVICE_NAME.equals(bluetoothDevice.getName())) {
            mBluetoothDevice = bluetoothDevice;
            mBluetoothService.connect(bluetoothDevice);
        }
    }

    @Override
    public void onStartScan() {
        Log.d(TAG, "onStartScan");
    }

    @Override
    public void onStopScan() {
        Log.d(TAG, "onStopScan");
    }
}
