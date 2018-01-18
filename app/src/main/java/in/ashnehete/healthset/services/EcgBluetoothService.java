package in.ashnehete.healthset.services;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static in.ashnehete.healthset.AppConstants.MESSAGE_READ;
import static in.ashnehete.healthset.AppConstants.MY_UUID;

/**
 * Created by Aashish Nehete on 02-Jan-18.
 */

public class EcgBluetoothService {
    private static final String TAG = "EcgBluetoothService";

    private BluetoothDevice mBluetoothDevice;
    private Handler mHandler;
    private ConnectThread mConnectThread;
    private UUID mUuid;

    public EcgBluetoothService(BluetoothDevice bluetoothDevice, Handler handler) {
        mBluetoothDevice = bluetoothDevice;
        mHandler = handler;
        mUuid = MY_UUID;
    }

    public synchronized void start() {
        Log.i(TAG, "start");
        if (mConnectThread != null) {
            mConnectThread.cancel();
        }
        mConnectThread = new ConnectThread(mBluetoothDevice);
        mConnectThread.start();
    }

    public void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            try {
                manageMyConnectedSocket(mmSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void manageMyConnectedSocket(BluetoothSocket mmSocket) throws IOException {
            InputStream in = mmSocket.getInputStream();
            byte[] mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = in.read(mmBuffer);
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = mHandler.obtainMessage(
                            MESSAGE_READ, numBytes, -1,
                            mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }
}
