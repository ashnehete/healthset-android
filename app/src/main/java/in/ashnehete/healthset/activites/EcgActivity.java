package in.ashnehete.healthset.activites;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Paint;
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import butterknife.BindView;
import butterknife.ButterKnife;
import in.ashnehete.healthset.R;
import in.ashnehete.healthset.services.EcgBluetoothService;
import in.ashnehete.healthset.utils.BoundedQueueLinkedList;

import static in.ashnehete.healthset.AppConstants.ECG_DEVICE_NAME;
import static in.ashnehete.healthset.AppConstants.MESSAGE_READ;

public class EcgActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 10;
    private static final String TAG = "EcgActivity";

    @BindView(R.id.plotEcg)
    XYPlot plotEcg;

    private List<Integer> plotData = new ArrayList<>();
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDevice mBluetoothDevice = null;
    private EcgBluetoothService mEcgBluetoothService = null;
    private Redrawer redrawer;
    private MyFadeFormatter formatter;
    private ECGModel ecgSeries;
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    StringTokenizer stMessage = new StringTokenizer(readMessage);
                    int tokens = stMessage.countTokens();

                    while (stMessage.hasMoreTokens()) {
                        plotData.add(Integer.parseInt(stMessage.nextToken()));
                    }

                    if (ecgSeries != null) {
                        // Sending the latest data
                        ecgSeries.update(plotData.subList(
                                plotData.size() - tokens,
                                plotData.size()
                        ));
                    }
            }
        }
    };

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
            Log.i(TAG, "onStart enabled");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        } else if (mEcgBluetoothService == null) {
            Log.i(TAG, "onStart setupDevice");
            setupDevice();
        }
        setupGraph();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        setupDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mEcgBluetoothService != null) {
            mEcgBluetoothService.stop();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mEcgBluetoothService != null) {
            mEcgBluetoothService.stop();
        }
        redrawer.finish();
    }

    private void setupDevice() {
        Log.i(TAG, "setupDevice");
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.i(TAG, deviceName + " " + deviceHardwareAddress);

                if (ECG_DEVICE_NAME.equals(deviceName)) {
                    mBluetoothDevice = device;
                    break;
                }
            }
        }

        if (mBluetoothDevice != null) {
            mBluetoothAdapter.cancelDiscovery();
            if (mEcgBluetoothService == null) {
                mEcgBluetoothService = new EcgBluetoothService(mBluetoothDevice, mHandler);
            }
            mEcgBluetoothService.start();
        } else {
            Toast.makeText(this, "Device not found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupGraph() {
        ecgSeries = new ECGModel(200, 10);

        formatter = new MyFadeFormatter(200);
        formatter.setLegendIconEnabled(false);
        plotEcg.addSeries(ecgSeries, formatter);
        plotEcg.setRangeBoundaries(0, 50, BoundaryMode.FIXED);
        plotEcg.setDomainBoundaries(0, 200, BoundaryMode.FIXED);

        // reduce the number of range labels
        plotEcg.setLinesPerRangeLabel(3);

        // start generating ecg data in the background:
        ecgSeries.start(new WeakReference<>(plotEcg.getRenderer(AdvancedLineAndPointRenderer.class)));

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

    public static class MyFadeFormatter extends AdvancedLineAndPointRenderer.Formatter {
        private int trailSize;

        public MyFadeFormatter(int trailSize) {
            this.trailSize = trailSize;
        }

        @Override
        public Paint getLinePaint(int thisIndex, int latestIndex, int seriesSize) {
            // offset from the latest index:
            int offset;
            if (thisIndex > latestIndex) {
                offset = latestIndex + (seriesSize - thisIndex);
            } else {
                offset = latestIndex - thisIndex;
            }
            float scale = 255f / trailSize;
            int alpha = (int) (255 - (offset * scale));
            getLinePaint().setAlpha(alpha > 0 ? alpha : 0);
            return getLinePaint();
        }
    }

    public static class ECGModel implements XYSeries {

        private final BoundedQueueLinkedList<Integer> data;
        private final long delayMs;
        private final int blipInteral;
        //        private final Thread thread;
        private boolean keepRunning;
        private int latestIndex;

        private WeakReference<AdvancedLineAndPointRenderer> rendererRef;

        /**
         * @param size         Sample size contained within this model
         * @param updateFreqHz Frequency at which new samples are added to the model
         */
        public ECGModel(int size, int updateFreqHz) {
            data = new BoundedQueueLinkedList<>(size);

            latestIndex = 0;

            // translate hz into delay (ms):
            delayMs = 1000 / updateFreqHz;

            // add 7 "blips" into the signal:
            blipInteral = size / 7;

//            thread = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    if (latestIndex >= data.length) {
//                        latestIndex = 0;
//                    }
//
//                    // Add plotEcg to data
//
//                    if (latestIndex < data.length - 1) {
//                        // null out the point immediately following i, to disable
//                        // connecting i and i+1 with a line:
//                        data[latestIndex + 1] = null;
//                    }
//
//                    if (rendererRef.get() != null) {
//                        rendererRef.get().setLatestIndex(latestIndex);
//                    }
//                    latestIndex++;
//                }
//            });
        }

        public void update(List<Integer> newPlotData) {
            data.addAll(newPlotData);

            if (rendererRef.get() != null) {
                rendererRef.get().setLatestIndex(data.size());
            }
        }

        public void start(final WeakReference<AdvancedLineAndPointRenderer> rendererRef) {
            this.rendererRef = rendererRef;
            keepRunning = true;
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
}
