package com.smartbikebt.uach.smartbikebt.services;

import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;


import com.smartbikebt.uach.smartbikebt.MainActivity;
import com.smartbikebt.uach.smartbikebt.UserPreference;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Created by cmcar on 22/10/2016.
 */

public class ServiceBluetooth extends Service {

    private String address = null;


    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private ProgressDialog progress;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences preference = getSharedPreferences("UserPreference", Context.MODE_PRIVATE);
        address = preference.getString("address", "null");
        //msg(address);
        if(address != "null"){
            connectBT();
        }else{
            msg("Fallo la direccion");
        }
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }



    private void connectBT(){


        boolean ConnectSuccess = true;
        try{
            if (btSocket == null || !isBtConnected){
                myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                BluetoothDevice device = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                btSocket = device.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                btSocket.connect();//start connection
            }
        }catch (IOException e){
            ConnectSuccess = false;//if the try failed, you can check the exception here
        }


        if (!ConnectSuccess){
            //msg("Fallo en la conexion, Intentalo de nuevo.");
            Intent i = new Intent("bluetooth_status");
            i.putExtra("status","fail_connect");
            sendBroadcast(i);
            //restart_app();
            //finish();
        }
        else
        {
            Intent i = new Intent("bluetooth_status");
            i.putExtra("status","conected");
            sendBroadcast(i);
            isBtConnected = true;
            new Thread(new Runnable(){
                public void run() {
                    while(true)
                    {
                        Intent i2 = new Intent("bluetooth_async");
                        final InputStream connectedInputStream;
                        InputStream in = null;
                        try {
                            in = btSocket.getInputStream();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        connectedInputStream = in;

                        byte[] buffer = new byte[1024];
                        int bytes;

                        try{
                            bytes = connectedInputStream.read(buffer);
                            String strReceived = new String(buffer,0, bytes);
                            final String msgReceived = strReceived;
                            i2.putExtra("sensor", msgReceived);
                            sendBroadcast(i2);

                        }catch (IOException e){
                            e.printStackTrace();
                            final String msgConnectionLost = "lost";
                            i2.putExtra("sensor", msgConnectionLost);
                            sendBroadcast(i2);
                            //connectBT();
                        }
                    }

                }
            }).start();
        }


    }

    private void restart_app() {
        //msg("lost");
        //Intent iSetting = new Intent(getBaseContext(), UserPreference.class);
        //startActivity(iSetting);
    }


}
