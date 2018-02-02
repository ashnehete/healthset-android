package in.ashnehete.healthset.activites;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.androidplot.util.Redrawer;
import com.androidplot.xy.AdvancedLineAndPointRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import butterknife.BindView;
import butterknife.ButterKnife;
import in.ashnehete.healthset.R;
import in.ashnehete.healthset.utils.BoundedQueueLinkedList;
import in.ashnehete.healthset.utils.CustomBluetoothConfiguration;
import in.ashnehete.healthset.utils.FadeFormatter;

import static in.ashnehete.healthset.AppConstants.BT_DEVICE_NAME;
import static in.ashnehete.healthset.AppConstants.MESSAGE_READ;
import static in.ashnehete.healthset.AppConstants.MESSAGE_WRITE;
import static in.ashnehete.healthset.AppConstants.MY_UUID;

public class EcgActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 10;
    private static final String TAG = "EcgActivity";
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.d(TAG, "MESSAGE_READ: " + readMessage);

                    StringTokenizer stMessage = new StringTokenizer(readMessage);
                    int tokens = stMessage.countTokens();

//                    while (stMessage.hasMoreTokens()) {
//                        plotData.add(Integer.parseInt(stMessage.nextToken()));
//                    }
//
//                    if (ecgModel != null) {
//                        // Sending the latest data
//                        ecgModel.update(plotData.subList(
//                                plotData.size() - tokens,
//                                plotData.size()
//                        ));
//                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf, 0, msg.arg1);
                    Log.d(TAG, "MESSAGE_WRITE: " + writeMessage);
                    break;
            }
        }
    };

    @BindView(R.id.plotEcg)
    XYPlot plotEcg;

    private List<Integer> plotData = new ArrayList<>();
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mBluetoothService = null;
    private BluetoothDevice mBluetoothDevice = null;
    private Redrawer redrawer;
    private FadeFormatter formatter;
    private ECGModel ecgModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ecg);
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
        ecgModel = new ECGModel(200, 5);

        formatter = new FadeFormatter(200);
        formatter.setLegendIconEnabled(false);
        plotEcg.addSeries(ecgModel, formatter);
        plotEcg.setRangeBoundaries(0, 1023, BoundaryMode.FIXED);
        plotEcg.setDomainBoundaries(0, 200, BoundaryMode.FIXED);

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

        public void update(int addPoint) {
            data.add(addPoint);

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
