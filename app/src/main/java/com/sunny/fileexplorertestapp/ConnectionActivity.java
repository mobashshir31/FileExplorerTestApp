package com.sunny.fileexplorertestapp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sunny.fileexplorertestapp.ClientService.ClientLocalBinder;

public class ConnectionActivity extends AppCompatActivity {

    private EditText servAddress, servPort;
    private TextView statusText;
    private Button connectButton;

    ClientService clientService;
    boolean isBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        servAddress = (EditText) findViewById(R.id.servAddress);
        servPort = (EditText) findViewById(R.id.servPort);
        connectButton = (Button) findViewById(R.id.connectButton);
        statusText = (TextView) findViewById(R.id.statusText);
        statusText.setText("");
        connectButton.setEnabled(true);

        Intent i = new Intent(this, ClientService.class);
        bindService(i, clientServiceConnection, Context.BIND_AUTO_CREATE);

        connectButton.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                String servAdd = servAddress.getText().toString();
                String servPortNumber = servPort.getText().toString();
                int portNo = Integer.parseInt(servPortNumber);
                if(servAdd.equals("")){
                    Toast.makeText(ConnectionActivity.this, "Please Enter Valid server address & port", Toast.LENGTH_LONG).show();
                    return;
                }
                connectButton.setEnabled(false);
                clientService.connectToServer(servAdd, portNo);
                Log.i(ClientService.TAG, "Connection Request sent");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(ClientService.EVENT_CONNECTION_SUCCESS);
        intentFilter.addAction(ClientService.EVENT_CONNECTION_FAIL);
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
            String action = intent.getAction();
            if(action.equals(ClientService.EVENT_CONNECTION_SUCCESS)){
                startFEActivity();
                connectButton.setEnabled(true);
                statusText.setText("");
            }
            else{
                connectButton.setEnabled(true);
                statusText.setText("Connection Error!!");
            }
        }
    };

    private void startFEActivity(){
        Intent i = new Intent(ConnectionActivity.this, FileExpActivity.class);
        startActivity(i);
    }

    private ServiceConnection clientServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBound = true;
            ClientLocalBinder clientLocalBinder = (ClientLocalBinder) service;
            clientService = clientLocalBinder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };
}
