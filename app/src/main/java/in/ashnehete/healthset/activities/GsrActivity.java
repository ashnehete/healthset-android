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

import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import in.ashnehete.healthset.R;
import in.ashnehete.healthset.models.Record;
import in.ashnehete.healthset.utils.CustomBluetoothConfiguration;

import static in.ashnehete.healthset.AppConstants.BT_DEVICE_NAME;
import static in.ashnehete.healthset.AppConstants.GSR;
import static in.ashnehete.healthset.AppConstants.MY_UUID;

public class GsrActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 10;
    private static final String TAG = "EcgActivity";

    @BindView(R.id.tv_gsr)
    TextView tvGsr;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mBluetoothService = null;
    private BluetoothDevice mBluetoothDevice = null;
    private Handler mHandler;
    private DatabaseReference mDatabase;
    private FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gsr);
        ButterKnife.bind(this);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = new Handler();
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
        // setupGraph();
    }

    private void setupDevice() {
        Log.i(TAG, "setupDevice");

        mBluetoothService = CustomBluetoothConfiguration
                .getBluetoothServiceInstance(getApplicationContext(), MY_UUID);

        mBluetoothService.setOnEventCallback(new GsrActivity.EcgOnEventCallback());
        mBluetoothService.setOnScanCallback(new GsrActivity.EcgOnScanCallback());
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
/*
    private void setupGraph() {
        plotGsr.getGraph().getGridBackgroundPaint().setColor(Color.BLACK);
        plotGsr.getGraph().getBackgroundPaint().setColor(Color.BLACK);
        plotGsr.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).getPaint().setColor(Color.TRANSPARENT);
        plotGsr.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).getPaint().setColor(Color.TRANSPARENT);
        plotGsr.getGraph().getDomainGridLinePaint().setColor(Color.TRANSPARENT);
        plotGsr.getGraph().getDomainSubGridLinePaint().setColor(Color.TRANSPARENT);
        plotGsr.getGraph().getRangeGridLinePaint().setColor(Color.TRANSPARENT);
        plotGsr.getGraph().getRangeSubGridLinePaint().setColor(Color.TRANSPARENT);
        plotGsr.getGraph().setSize(new Size(
                1.0f, SizeMode.RELATIVE,
                1.0f, SizeMode.RELATIVE));

        ecgModel = new EcgActivity.ECGModel(1200, 1000);

        formatter = new FadeFormatter(1500, Color.GREEN);
        formatter.setLegendIconEnabled(false);
        plotGsr.addSeries(ecgModel, formatter);
        plotGsr.setRangeBoundaries(0, 10000, BoundaryMode.FIXED);
        plotGsr.setDomainBoundaries(0, 1200, BoundaryMode.FIXED);

        // reduce the number of range labels
        plotGsr.setLinesPerRangeLabel(3);

        // start generating ecg data in the background:
        ecgModel.start(new WeakReference<>(plotGsr.getRenderer(AdvancedLineAndPointRenderer.class)));

        // set a redraw rate of 30hz and start immediately:
        redrawer = new Redrawer(plotGsr, 30, true);
    }
    */

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

        String value = tvGsr.getText().toString();
        Record record = new Record(GSR, value);

        String key = mDatabase.push().getKey();
        mDatabase.child(key).setValue(record);
    }

    private void changeUi(String gsr) {
        tvGsr.setText(gsr + " kΩ");
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
            final String status = bluetoothStatus.toString();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(GsrActivity.this, status, Toast.LENGTH_SHORT).show();
                }
            });
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
