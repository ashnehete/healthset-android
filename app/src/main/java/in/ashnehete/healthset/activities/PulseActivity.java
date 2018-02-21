package in.ashnehete.healthset.activities;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import in.ashnehete.healthset.R;
import in.ashnehete.healthset.models.Record;
import in.ashnehete.healthset.utils.CustomBluetoothConfiguration;

import static in.ashnehete.healthset.AppConstants.BT_DEVICE_NAME;
import static in.ashnehete.healthset.AppConstants.MY_UUID;
import static in.ashnehete.healthset.AppConstants.PULSE;

public class PulseActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 10;
    private static final String TAG = "PulseActivity";

    @BindView(R.id.tv_pulse)
    TextView tvPulse;

    private List<Integer> plotData = new ArrayList<>();
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mBluetoothService = null;
    private BluetoothDevice mBluetoothDevice = null;
    private Handler mHandler = new Handler();
    private DatabaseReference mDatabase;
    private FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse);
        ButterKnife.bind(this);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference()
                .child("records").child(mUser.getUid());
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

        mBluetoothService.setOnEventCallback(new OnEventCallback());
        mBluetoothService.setOnScanCallback(new OnScanCallback());
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
        Log.d(TAG, "write (g)");
        try {
            BluetoothWriter writer = new BluetoothWriter(mBluetoothService);
            writer.writeln("g");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopStream(View view) {
        Log.d(TAG, "write (Q)");
        try {
            BluetoothWriter writer = new BluetoothWriter(mBluetoothService);
            writer.writeln("Q");
            writer.writeln("Q");
            writer.writeln("Q");
        } catch (Exception e) {
            e.printStackTrace();
        }

        String value = tvPulse.getText().toString();
        Record record = new Record(PULSE, value);

        String key = mDatabase.push().getKey();
        mDatabase.child(key).setValue(record);
    }

    private void changeUi(String pulse) {
        tvPulse.setText(pulse.concat(" bpm"));
    }

    public class OnScanCallback implements BluetoothService.OnBluetoothScanCallback {

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

    public class OnEventCallback implements BluetoothService.OnBluetoothEventCallback {

        @Override
        public void onDataRead(byte[] bytes, int i) {
            Log.d(TAG, "onDataRead: " + new String(bytes, 0, i));
            // TODO: Add data to plotEcg and plotData
            final String data = new String(bytes, 0, i);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    changeUi(data);
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
