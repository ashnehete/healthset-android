package in.ashnehete.healthset.activites;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import in.ashnehete.healthset.R;
import in.ashnehete.healthset.utils.CustomBluetoothConfiguration;

import static in.ashnehete.healthset.AppConstants.BT_DEVICE_NAME;
import static in.ashnehete.healthset.AppConstants.MY_UUID;

public class TemperatureActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 10;
    private static final String TAG = "EcgActivity";

    @BindView(R.id.tv_temperature_c)
    TextView tvTempC;
    @BindView(R.id.tv_temperature_f)
    TextView tvTempF;

    private List<Integer> plotData = new ArrayList<>();
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mBluetoothService = null;
    private BluetoothDevice mBluetoothDevice = null;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature);
        ButterKnife.bind(this);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "onStart enabled");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        } else if (mBluetoothService == null) {
            Log.d(TAG, "onStart setupDevice");
            setupDevice();
        }
    }

    private void setupDevice() {
        Log.i(TAG, "setupDevice");

        mBluetoothService = CustomBluetoothConfiguration
                .getBluetoothServiceInstance(getApplicationContext(), MY_UUID);

        mBluetoothService.setOnEventCallback(new EcgOnEventCallback());
        mBluetoothService.setOnScanCallback(new EcgOnScanCallback());
        mBluetoothService.startScan();

        Set<BluetoothDevice> bluetoothDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
            Log.d(TAG, bluetoothDevice.getName());
            if (BT_DEVICE_NAME.equals(bluetoothDevice.getName())) {
                Log.d(TAG, "Found: " + bluetoothDevice.getName());
                mBluetoothDevice = bluetoothDevice;
                mBluetoothService.connect(bluetoothDevice);
                mBluetoothService.stopScan();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Bluetooth Enable
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth Enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth Enable Cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void startStream(View view) {
        Log.d(TAG, "write (e)");
        try {
            BluetoothWriter writer = new BluetoothWriter(mBluetoothService);
            writer.writeln("e");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopStream(View view) {
        Log.d(TAG, "write (Q)");
        try {
            BluetoothWriter writer = new BluetoothWriter(mBluetoothService);
            writer.writeln("Q");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void changeUi(double doubleTempC, double doubleTempF) {
        tvTempC.setText(String.valueOf(doubleTempC).concat(" °C"));
        tvTempF.setText(String.valueOf(doubleTempF).concat(" °F"));
    }

    public class EcgOnScanCallback implements BluetoothService.OnBluetoothScanCallback {

        @Override
        public void onDeviceDiscovered(BluetoothDevice bluetoothDevice, int i) {
            Log.d(TAG, "onDeviceDiscovered: " + bluetoothDevice.toString());
            if (BT_DEVICE_NAME.equals(bluetoothDevice.getName())) {
                mBluetoothDevice = bluetoothDevice;
                mBluetoothService.connect(bluetoothDevice);
                mBluetoothService.stopScan();
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

    public class EcgOnEventCallback implements BluetoothService.OnBluetoothEventCallback {

        @Override
        public void onDataRead(byte[] bytes, int i) {
            Log.d(TAG, "onDataRead: " + new String(bytes, 0, i));
            // TODO: Add data to plotEcg and plotData
            String tempC = new String(bytes, 0, i);
            final double doubleTempC = Double.parseDouble(tempC);
            final double doubleTempF = (doubleTempC * 9) / 5 + 32;

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    changeUi(doubleTempC, doubleTempF);
                }
            });
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
    }
}
