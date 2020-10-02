package com.example.bluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ChatUtils {
    //member variables
    private Context context;
    private Handler handler;
    private int state;
    private int newState;

    //bluetooth adapter
    private BluetoothAdapter bluetoothAdapter;

    //Connect thread
    private ConnectThread connectThread;
    //accept thread
    private AcceptThread acceptThread;
    //connected thread
    private ConnectedThread connectedThread;
    //UUID for app

    private final UUID APP_UUID = UUID.fromString("79433a70-ec23-4c09-8c39-70b6584c9e34");
    private final String APP_NAME = "BluetoothChat";

    // Constants that indicate the current connection state
    public static final int STATE_NONE= 1;// we're doing nothing
    public static final int STATE_LISTEN = 2;// now listening for incoming connections
    public static final int STATE_CONNECTING = 3;// now initiating an outgoing connection
    public static final int STATE_CONNECTED = 4; // now connected to a remote device


    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public ChatUtils(Context context, Handler handler){
        this.context = context;
        this.handler = handler;

        state = STATE_NONE;
        newState = state;
        //initaialize adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Return the current connection state.
     */
    public  synchronized int getState() {
        return state;
    }

    private synchronized void updateUserInterfaceTitle(){
        state = getState();
        newState = state;

        // Give the new state to the Handler so the UI Activity can update
        handler.obtainMessage(Constants.MESSAGE_STATE_CHANGED,newState,-1).sendToTarget();
    }



    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start(){

        // Cancel any thread attempting to make a connection
        if (connectThread!=null){
            connectThread.cancel();
            connectThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (acceptThread == null){
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        // Cancel any thread currently running a connection
        if (connectedThread !=null){
            connectedThread.cancel();
            connectedThread = null;
        }


        //update UI title
        updateUserInterfaceTitle();

    }
    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {

        //cancel the thread that completed the connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        //cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        //Cancel the accept thread because we only want to connect to one device
        if (acceptThread !=null){
            acceptThread.cancel();
            acceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message message = handler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * stop all threads
     */
    public synchronized void stop(){
        if (connectThread!=null){
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread !=null){
            acceptThread.cancel();
            acceptThread = null;

        }

        if (connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }
        state = STATE_NONE;

        //update UI title
        updateUserInterfaceTitle();

    }
    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device){

        //cancel any thread attempting to make a connection
        if(state == STATE_CONNECTING){
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread!=null){
            acceptThread.cancel();
            acceptThread = null;
        }


        //cancel any thread currently  running a connection
        if (connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }
        connectThread = new ConnectThread(device);
        connectThread.start();
        updateUserInterfaceTitle();

    }
    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param buffer The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] buffer) {
        // Create temporary object
        ConnectedThread connThread;

        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (state != STATE_CONNECTED) {
                return;
            }

            connThread = connectedThread;
        }
        // Perform the write unsynchronized
        connThread.write(buffer);
    }
    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed(){
        Message message = handler.obtainMessage(Constants.MESSAGE_TOAST);
        //create a bundle to put data into the message to pass it to the main activity
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST,"Can't connect to the device");

        message.setData(bundle);
        handler.sendMessage(message);

        state = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        //start chat utils again to start listening again
        ChatUtils.this.start();

    }
    /**
     * * Indicate that the connection was lost and notify the UI Activity .
     */
    private void connectionLost(){
        // Send a failure message back to the Activity
        Message message = handler.obtainMessage(Constants.MESSAGE_TOAST);

        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Disconnected");
        message.setData(bundle);
        handler.sendMessage(message);

        state = STATE_NONE;

        // Start the service over to restart listening mode
        ChatUtils.this.start();
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        ChatUtils.this.start();
    }




    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread{
        //member variables
        private BluetoothServerSocket serverSocket;

        public AcceptThread(){
            BluetoothServerSocket temp = null;
            try {
                temp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,APP_UUID);
            } catch (IOException e) {
                Log.e("Accept ->constructor",e.toString());
            }
            serverSocket = temp;
            state = STATE_LISTEN;
        }

        public void run(){

            BluetoothSocket socket = null;

            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                Log.e("Accept->run",e.toString());
                try {
                    serverSocket.close();
                } catch (IOException ex) {
                    Log.e("Accept->close",ex.toString());
                }
            }
            if (socket != null) {
                synchronized (this) {
                    switch (state) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(socket,socket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e("Accept->closesocket", e.toString());
                            }
                            break;
                    }
                }
            }

        }
        public void cancel(){
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e("Accept->cancel",e.toString());
            }
        }

    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread{

        //member variables
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public ConnectThread(BluetoothDevice bluetoothDevice){
            this.bluetoothDevice = bluetoothDevice;

            //setting up the socket
            BluetoothSocket temp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                temp = bluetoothDevice.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e("Connect -> constructor",e.toString());
            }

            bluetoothSocket = temp;
            state = STATE_CONNECTING;
        }
        public void run(){
            // Always cancel discovery because it will slow down a connection
            bluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                bluetoothSocket.connect();
            } catch (IOException e) {
                // Close the socket
                Log.e("Connect -> run",e.toString());

                try {
                    bluetoothSocket.close();
                } catch (IOException ex) {
                    Log.e("Connect -> closesocket",ex.toString());
                }

                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (ChatUtils.this){
                connectThread = null;
            }

            // Start the connected thread
            connected(bluetoothSocket,bluetoothDevice);
        }

        public void cancel(){
            //close the socket
            try {
                bluetoothSocket.close();
            }catch (IOException e){
                Log.e("Connect -> cancel",e.toString());
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread{
        //member variables
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket){
            this.socket = socket;

            InputStream tempIn = null;
            OutputStream tempOut = null;

            // Get the BluetoothSocket input and output streams
            try{
                tempIn = socket.getInputStream();
                tempOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = tempIn;
            outputStream = tempOut;
            state = STATE_CONNECTED;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            // Keep listening to the InputStream while connected
            while (state == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = inputStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    handler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer){
            try {
                outputStream.write(buffer);

                // Share the sent message back to the UI Activity
                handler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void cancel(){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }




}