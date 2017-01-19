package com.sunny.fileexplorertestapp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.sunny.fileexplorertestapp.ClientService.ClientLocalBinder;
import com.sunny.fileexplorertestapp.ServerCommUtility.FileExpItem;
import com.sunny.fileexplorertestapp.ServerCommUtility.RequestMessage;

import java.util.ArrayList;

public class FileExpActivity extends AppCompatActivity {

    private ListView fileListView;
    private FileExpAdapter fileExpAdapter;
    private ArrayList<FileExpItem> listItems;
    private String currDir;

    //values for back button to exit activity
    private static final int TIME_INTERVAL_BACK_PRESS = 2000;
    private long lastBackPressed;

    ClientService clientService;
    boolean isBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_exp);
        fileListView = (ListView) findViewById(R.id.fileListView);

        Intent i = new Intent(this, ClientService.class);
        bindService(i, clientServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initializeList() {
        //gets called after binding successful
        currDir = null;
        listItems = clientService.getFileExpItemList();
        fileExpAdapter = new FileExpAdapter(FileExpActivity.this, listItems);
        fileListView.setAdapter(fileExpAdapter);
        fileListView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        FileExpItem item = (FileExpItem) parent.getItemAtPosition(position);
                        itemClicked(item);
                    }
                }
        );
    }

    private void itemClicked(FileExpItem item){
        String name = item.getName();
        String action;
        if(item.isDirectory()){
            action = RequestMessage.DISPLAY_DIRECTORY;
        }
        else{
            action = RequestMessage.EXEC_FILE;
            String type = item.getFileType();
            if(type!=null&&type.startsWith("video/")){
                AlertDialog dialog = new AlertDialog.Builder(FileExpActivity.this).create();
                dialog.setTitle("Video File");
                dialog.setMessage("Opened a video file");
                dialog.show();
            }
        }
        RequestMessage message = new RequestMessage(action, currDir, name);
        clientService.commWithServer(message);
    }

    @Override
    public void onBackPressed() {
        if(clientService!=null && currDir != null){
            String action = RequestMessage.NAVIGATE_PARENT;
            RequestMessage message = new RequestMessage(action, currDir);
            clientService.commWithServer(message);
        }
        else{
            if((lastBackPressed + TIME_INTERVAL_BACK_PRESS) > System.currentTimeMillis()){
                clientService.disconnectFromServer();
                super.onBackPressed();
                return;
            }
            else{
                Toast.makeText(FileExpActivity.this, "Press once again to exit!", Toast.LENGTH_SHORT).show();
            }
            lastBackPressed = System.currentTimeMillis();
        }
    }

    private ServiceConnection clientServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ClientLocalBinder clientLocalBinder = (ClientLocalBinder) service;
            clientService = clientLocalBinder.getService();
            isBound = true;
            initializeList();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(ClientService.EVENT_COMM_SUCCESS);
        intentFilter.addAction(ClientService.EVENT_COMM_FAIL);
        LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bReceiver);
    }

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getAction();
            if(event.equals(ClientService.EVENT_COMM_SUCCESS)){
                Bundle b = intent.getExtras();
                if(b == null)
                    return;
                String request = b.getString(ClientService.COMM_REQUEST);
                if (request.equals(RequestMessage.LIST_ROOTS) || request.equals(RequestMessage.NAVIGATE_PARENT)
                        || request.equals(RequestMessage.DISPLAY_DIRECTORY))
                {
                    listItems = clientService.getFileExpItemList();
                    currDir = clientService.getCurrDir();
                    fileExpAdapter.changeListItems(listItems);
                    //to scroll to top
                    fileListView.setSelectionAfterHeaderView();
                }
                else if (request.equals(RequestMessage.EXEC_FILE)){
                    return;
                }
            }
            else if(event.equals(ClientService.EVENT_COMM_FAIL)){
                Toast.makeText(FileExpActivity.this, "Communication Error!", Toast.LENGTH_LONG).show();
                return;
            }
        }
    };
}
