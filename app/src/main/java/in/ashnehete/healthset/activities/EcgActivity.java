package in.ashnehete.healthset.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.androidplot.ui.Size;
import com.androidplot.ui.SizeMode;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.AdvancedLineAndPointRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothWriter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.lang.ref.WeakReference;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import in.ashnehete.healthset.R;
import in.ashnehete.healthset.models.Record;
import in.ashnehete.healthset.utils.BoundedQueueLinkedList;
import in.ashnehete.healthset.utils.CustomBluetoothConfiguration;
import in.ashnehete.healthset.utils.FadeFormatter;

import static in.ashnehete.healthset.AppConstants.BT_DEVICE_NAME;
import static in.ashnehete.healthset.AppConstants.ECG;
import static in.ashnehete.healthset.AppConstants.MY_UUID;

public class EcgActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 10;
    private static final String TAG = "EcgActivity";

    @BindView(R.id.plotEcg)
    XYPlot plotEcg;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mBluetoothService = null;
    private BluetoothDevice mBluetoothDevice = null;
    private Redrawer redrawer;
    private FadeFormatter formatter;
    private ECGModel ecgModel;
    private Handler mHandler;
    private DatabaseReference mDatabase;
    private FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ecg);
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
        setupGraph();
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

    private void setupGraph() {
        plotEcg.getGraph().getGridBackgroundPaint().setColor(Color.BLACK);
        plotEcg.getGraph().getBackgroundPaint().setColor(Color.BLACK);
        plotEcg.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).getPaint().setColor(Color.TRANSPARENT);
        plotEcg.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).getPaint().setColor(Color.TRANSPARENT);
        plotEcg.getGraph().getDomainGridLinePaint().setColor(Color.TRANSPARENT);
        plotEcg.getGraph().getDomainSubGridLinePaint().setColor(Color.TRANSPARENT);
        plotEcg.getGraph().getRangeGridLinePaint().setColor(Color.TRANSPARENT);
        plotEcg.getGraph().getRangeSubGridLinePaint().setColor(Color.TRANSPARENT);

        plotEcg.getGraph().setSize(new Size(
                1.0f, SizeMode.RELATIVE,
                1.0f, SizeMode.RELATIVE));

        ecgModel = new ECGModel(1200, 1000);

        formatter = new FadeFormatter(1500, Color.RED);
        formatter.setLegendIconEnabled(false);
        formatter.setPointLabelFormatter(null);
        plotEcg.addSeries(ecgModel, formatter);
        plotEcg.setRangeBoundaries(0, 800, BoundaryMode.FIXED);
        plotEcg.setDomainBoundaries(0, 1200, BoundaryMode.FIXED);

        // reduce the number of range labels
        plotEcg.setLinesPerRangeLabel(3);

        // start generating ecg data in the background:
        ecgModel.start(new WeakReference<>(plotEcg.getRenderer(AdvancedLineAndPointRenderer.class)));

        // set a redraw rate of 30hz and start immediately:
        redrawer = new Redrawer(plotEcg, 30, true);
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
            writer.writeln("Q");
            writer.writeln("Q");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create and upload file
        final Record record = new Record(ECG);
        String filename = mUser.getUid() + "_" + record.getTimestamp() + ".txt";

        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("ECG").child(filename);
        storageReference.putBytes(ecgModel.getDataString().getBytes()).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG, "Upload unsuccessful");
                exception.printStackTrace();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                Uri downloadUrl = taskSnapshot.getDownloadUrl();

                record.setValue(downloadUrl.toString());
                String key = mDatabase.push().getKey();
                mDatabase.child(key).setValue(record);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        mBluetoothService.disconnect();
        redrawer.finish();
    }

    public static class ECGModel implements XYSeries {

        private final BoundedQueueLinkedList<Integer> data;
        private int latestIndex;

        private WeakReference<AdvancedLineAndPointRenderer> rendererRef;

        /**
         * @param size         Sample size contained within this model
         * @param updateFreqHz Frequency at which new samples are added to the model
         */
        public ECGModel(int size, int updateFreqHz) {
            data = new BoundedQueueLinkedList<>(size);
            latestIndex = data.size();
        }

        public void update(int point) {
            data.add(point);

            if (rendererRef.get() != null) {
                rendererRef.get().setLatestIndex(data.size());
            }
        }

        public void start(final WeakReference<AdvancedLineAndPointRenderer> rendererRef) {
            this.rendererRef = rendererRef;
        }

        @Override
        public int size() {
            return data.size();
        }

        @Override
        public Number getX(int index) {
            return index;
        }

        @Override
        public Number getY(int index) {
            return data.get(index);
        }

        @Override
        public String getTitle() {
            return "Signal";
        }

        public String getDataString() {
            return data.toString();
        }
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
            String number = new String(bytes, 0, i);
            ecgModel.update((int) Double.parseDouble(number));
        }

        @Override
        public void onStatusChange(BluetoothStatus bluetoothStatus) {
            Log.d(TAG, "onStatusChange: " + bluetoothStatus.toString());
            final String status = bluetoothStatus.toString();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(EcgActivity.this, status, Toast.LENGTH_SHORT).show();
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
