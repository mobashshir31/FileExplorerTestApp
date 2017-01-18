package com.sunny.fileexplorertestapp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import com.sunny.fileexplorertestapp.ServerCommUtility.RequestMessage;
import com.sunny.fileexplorertestapp.ServerCommUtility.FileExpItem;

public class ClientService extends Service {
    private final IBinder clientBinder = new ClientLocalBinder();

    private Socket clientSocket = null;
    private DataInputStream inputStream = null;
    private DataOutputStream outputStream = null;

    private ArrayList<FileExpItem> fileExpItemList = new ArrayList<FileExpItem>();
    private String currDir = null;

    public static final String TAG = "Log-Message";
    public static final String COMM_REQUEST = "communication-result";
    public static final String EVENT_CONNECTION_SUCCESS = "server-connection-success";
    public static final String EVENT_CONNECTION_FAIL = "server-connection-failure";
    public static final String EVENT_COMM_SUCCESS = "communication-event-successful";
    public static final String EVENT_COMM_FAIL = "communication-event-failure";

    // An object to lock the service using synchronized() so that no two threads use the service
    // and thereby the Sockets simultaneously
    private final Object lockObject = new Object();

    public String getCurrDir() {
        return currDir;
    }

    public ArrayList<FileExpItem> getFileExpItemList() {
        return fileExpItemList;
    }

    public void connectToServer(String serverAddress, int serverPort) {
        ConnectRunnable connectRunnable = new ConnectRunnable(serverAddress, serverPort);
        Thread connectThread = new Thread(connectRunnable);
        connectThread.start();
    }

    public void disconnectFromServer() {
        synchronized (lockObject) {
            try {
                if (inputStream != null)
                    inputStream.close();
                if (outputStream != null)
                    outputStream.close();
                if (clientSocket != null)
                    clientSocket.close();
            } catch (IOException e) {
                Log.i(TAG, "Error in Disconnecting");
            }
        }
    }

    private boolean sendToServerUtil(RequestMessage message) throws IOException {
        String request = message.getRequest();
        String dir = message.getCurrentDir();
        if (dir == null)
            dir = "";
        String name = message.getFileName();
        if (request.equals(RequestMessage.LIST_ROOTS)) {
            outputStream.writeUTF(request);
        } else if (request.equals(RequestMessage.NAVIGATE_PARENT)) {
            outputStream.writeUTF(request);
            outputStream.writeUTF(dir);
        } else if (request.equals(RequestMessage.DISPLAY_DIRECTORY) || request.equals(RequestMessage.EXEC_FILE)) {
            outputStream.writeUTF(request);
            outputStream.writeUTF(dir);
            outputStream.writeUTF(name);
        } else
            return false;
        return receiveFromServerUtil(request);
    }

    private boolean receiveFromServerUtil(String request) throws IOException {
        if (request.equals(RequestMessage.LIST_ROOTS) || request.equals(RequestMessage.NAVIGATE_PARENT)
                || request.equals(RequestMessage.DISPLAY_DIRECTORY)) {
            int count = inputStream.readInt();
            Log.i(TAG, "Read: " + count);
            fileExpItemList.clear();
            String dir = inputStream.readUTF();
            Log.i(TAG, "Read: " + dir);
            if (dir.equals(""))
                dir = null;
            currDir = dir;
            while (count > 0) {
                String name = inputStream.readUTF();
                Log.i(TAG, "Read: " + name);
                boolean isDirectory = inputStream.readBoolean();
                Log.i(TAG, "Read: " + isDirectory);
                String info = inputStream.readUTF();
                Log.i(TAG, "Read: " + info);
                FileExpItem fileExpItem = new FileExpItem(name, isDirectory, info);
                fileExpItemList.add(fileExpItem);
                count--;
            }
            return true;
        } else if (request.equals(RequestMessage.EXEC_FILE))
            return true;
        else
            return false;
    }

    public void commWithServer(RequestMessage message) {
        CommunicateRunnable communicateRunnable = new CommunicateRunnable(message);
        Thread communicateThread = new Thread(communicateRunnable);
        communicateThread.start();
    }

    private void sendMessageViaBroadcast(String action) {
        Intent i = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    private void sendMessageViaBroadcast(String action, String key, String message) {
        Intent i = new Intent(action);
        i.putExtra(key, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    public ClientService() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectFromServer();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return clientBinder;
    }

    public class ClientLocalBinder extends Binder {
        ClientService getService() {
            return ClientService.this;
        }
    }

    private class ConnectRunnable implements Runnable {
        private String serverAddress;
        private int serverPort;

        public ConnectRunnable(String serverAddress, int serverPort) {
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
        }

        @Override
        public void run() {
            synchronized (lockObject) {
                try {
                    Log.i(TAG, "Trying to connect");
                    clientSocket = new Socket(serverAddress, serverPort);
                    outputStream = new DataOutputStream(clientSocket.getOutputStream());
                    inputStream = new DataInputStream(clientSocket.getInputStream());

                    RequestMessage message = new RequestMessage(RequestMessage.LIST_ROOTS);
                    boolean success = sendToServerUtil(message);
                    if (success)
                        sendMessageViaBroadcast(EVENT_CONNECTION_SUCCESS);
                    else
                        sendMessageViaBroadcast(EVENT_CONNECTION_FAIL);
                } catch (IOException e) {
                    Log.i(TAG, "Connection failed");
                    sendMessageViaBroadcast(EVENT_CONNECTION_FAIL);
                }
            }

        }
    }

    private class CommunicateRunnable implements Runnable {
        private RequestMessage message;

        public CommunicateRunnable(RequestMessage message) {
            this.message = message;
        }

        @Override
        public void run() {
            synchronized (lockObject) {
                try {
                    boolean success = sendToServerUtil(message);
                    if (success)
                        sendMessageViaBroadcast(EVENT_COMM_SUCCESS, COMM_REQUEST, message.getRequest());
                    else {
                        Log.i(TAG, "Communication failure");
                        sendMessageViaBroadcast(EVENT_COMM_FAIL);
                    }
                } catch (IOException e) {
                    Log.i(TAG, "Communication failure");
                    sendMessageViaBroadcast(EVENT_COMM_FAIL);
                }
            }

        }
    }
}
