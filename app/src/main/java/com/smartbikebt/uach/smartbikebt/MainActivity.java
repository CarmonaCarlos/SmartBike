package com.smartbikebt.uach.smartbikebt;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.smartbikebt.uach.smartbikebt.services.ServiceBluetooth;
import com.smartbikebt.uach.smartbikebt.services.ServiceSpeed;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;




public class MainActivity extends AppCompatActivity {

    @Bind(R.id.textViewSpeed)
    TextView textViewSpeed;
    @Bind(R.id.imageViewDown)
    ImageView imageViewDown;
    @Bind(R.id.imageViewUp)
    ImageView imageViewUp;
    @Bind(R.id.imageViewLeft)
    ImageView imageViewLeft;
    @Bind(R.id.imageViewRight)
    ImageView imageViewRight;

    private BluetoothAdapter myBluetooth = null;
    private final static int PERMISSION = 1;
    private BroadcastReceiver broadcastReceiverSpeed;
    private BroadcastReceiver broadcastReceiverLatitude;
    private BroadcastReceiver broadcastReceiverBT;
    private BroadcastReceiver broadcastReceiverBTSensor;
    private String address = null;
    Vibrator v;
    public boolean flagNumber;
    public String contactNumber;
    public boolean flagVentana = false;
    public boolean flagCancelarSMS = false;
    public  boolean flagSMSEnviado = false;
    double mps, speed, truncSpeed;
    public String latitud, longitud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        SharedPreferences preference = getSharedPreferences("UserPreference", Context.MODE_PRIVATE);
        address = preference.getString("address", "null");
        loadPreferences();
        if(address.equals("null")){
            msgFailConnect();
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        turnOnBT();


        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                  &&  ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED){
            //Continuie with the aplication
            //msg("Permisos consedidos");

            Intent iServiceBluetooth = new Intent(getApplicationContext(), ServiceBluetooth.class);
            startService(iServiceBluetooth);

            Intent iServiceSpeed = new Intent(getApplicationContext(), ServiceSpeed.class);
            startService(iServiceSpeed);


        }else{
            msg("No cuentas con los permisos");
            getPermission();
        }

        initImageWarning();
        v = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);


    }

    public void loadPreferences(){
        SharedPreferences preference = getSharedPreferences("UserPreference", Context.MODE_PRIVATE);
        flagNumber = preference.getBoolean("checkedSMS", false);
        contactNumber = preference.getString("contactNumber", "");
    }

    private void initImageWarning() {
        imageViewDown.setVisibility(View.INVISIBLE);
        imageViewUp.setVisibility(View.INVISIBLE);
        imageViewLeft.setVisibility(View.INVISIBLE);
        imageViewRight.setVisibility(View.INVISIBLE);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(broadcastReceiverSpeed == null){
            broadcastReceiverSpeed = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    latitud = intent.getExtras().get("latitud").toString();
                    longitud = intent.getExtras().get("longitud").toString();
                    mps = (Double.parseDouble(intent.getExtras().get("speed").toString()));
                    speed = ((mps *3600) / 1000); //Km/h
                    truncSpeed = Math.floor(speed * 100) / 100;
                    //textViewSpeed.setText(mps + " m/s");
                    textViewSpeed.setText("\n" + truncSpeed + " Km/h");

                }
            };
        }
        registerReceiver(broadcastReceiverSpeed,new IntentFilter("location_update"));


        //Start Bluetooth
        if(broadcastReceiverBT == null){
            broadcastReceiverBT = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String BT;
                    BT = (intent.getExtras().get("status").toString());
                    if(BT.equals("conected")){
                        //msg("Conectado!");
                        View parentLayout = findViewById(R.id.activity_main);
                        Snackbar.make(parentLayout, "Conectado a: " + address, Snackbar.LENGTH_LONG)
                                .setAction("", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                    }
                                })
                                .setActionTextColor(getResources().getColor(android.R.color.holo_red_light))
                                .show();
                    }else if(BT.equals("fail_connect")){
                        msgFailConnect();
                    }
                }
            };
        }
        registerReceiver(broadcastReceiverBT,new IntentFilter("bluetooth_status"));

        if(broadcastReceiverBTSensor == null){
            broadcastReceiverBTSensor = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String BTAsync;
                    BTAsync = (intent.getExtras().get("sensor").toString());
                    //msg(BTAsync);
                    if(BTAsync.equals("lost")){
                        unregisterReceiver(broadcastReceiverBTSensor);
                        restart_appMSG();

                    }
                    runningStatus(BTAsync);
                }

            };
        }

        registerReceiver(broadcastReceiverBTSensor,new IntentFilter("bluetooth_async"));
        //End bluetooth
    }

    private void runningStatus(String btAsync) {
        String status = btAsync;
        //msg(status);
        if(status.equals("0")){
            //Ninguna alerta
            initImageWarning();
        }else if(status.equals("1")){
            //Sensor detecto algo enfrente
            imageViewUp.setVisibility(View.VISIBLE);
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
                v.vibrate(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if(status.equals("2")){
            //Sensor detecto algo izquierda
            imageViewLeft.setVisibility(View.VISIBLE);
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
                v.vibrate(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if(status.equals("3")){
            //Sensor detecto algo derecha
            imageViewRight.setVisibility(View.VISIBLE);
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
                v.vibrate(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if(status.equals("4")){
            //sensor detecto algo atras
            imageViewDown.setVisibility(View.VISIBLE);
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
                v.vibrate(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if(status.equals("9")){
            if(flagNumber){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                if (flagVentana == false && mps < 2.0){
                    alertSMSContact();
                }

            }

        }
    }

    private void alertSMSContact() {
        flagVentana = true;
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        //final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("¿Enviar mensaje de alerta?");
        alertDialog.setMessage("00:30");
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                "Enviar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        flagVentana = false;
                        alertDialog.hide();
                        SmsManager sms = SmsManager.getDefault();
                        if(flagNumber){

                            if(latitud != null && longitud != null){
                                String uri = "https://maps.google.com/?q="+latitud+","+longitud;
                                sms.sendTextMessage(contactNumber, null,"Tuve un accidente en mi bicicleta, ocurrio en: " + uri, null, null);
                                msg("Mensaje enviado!");
                                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                            }else{

                                sms.sendTextMessage(contactNumber, null,"Tuve un accidente en mi bicicleta.", null, null);
                                msg("Mensaje enviado!");
                                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                            }
                            flagSMSEnviado = true;



                        }



                    }
                });

        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                "Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        flagVentana = false;
                        flagCancelarSMS = true;
                        alertDialog.hide();
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                        //Se cancelo
                    }
                });
        alertDialog.show();

        new CountDownTimer(20000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                alertDialog.setMessage("00:"+ (millisUntilFinished/1000));
            }

            @Override
            public void onFinish() {
                if (flagCancelarSMS) {
                    //Se cancelo la envida del mensaje o se envio el mensaje
                    alertDialog.hide();

                }else if(!flagSMSEnviado && !flagCancelarSMS){
                    //Enviar menaje
                    flagVentana = false;
                    alertDialog.hide();
                    sendSMS();
                }else{
                    flagVentana = false;
                    alertDialog.hide();
                    sendSMS();
                }

            }

            public void sendSMS() {
                flagCancelarSMS = false;
                flagSMSEnviado = false;
                SmsManager sms = SmsManager.getDefault();
                //TODO: comprobar velocidad
                if(flagNumber){
                    if(latitud != null && longitud != null){
                        String uri = "https://maps.google.com/?q="+latitud+","+longitud;
                        sms.sendTextMessage(contactNumber, null,"Tuve un accidente en mi bicicleta, ocurrio en: " + uri, null, null);
                        msg("Mensaje enviado!");
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    }else{
                        sms.sendTextMessage(contactNumber, null,"Tuve un accidente en mi bicicleta.", null, null);
                        msg("Mensaje enviado!");
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    }
                }

            }

        }.start();

    }

    private void msgFailConnect() {
        /*if(getOrientation().equals("landscape")){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }*/

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);


        View parentLayout = findViewById(R.id.activity_main);
        Snackbar.make(parentLayout, "Fallo la conexión", Snackbar.LENGTH_INDEFINITE)
                .setAction("Configuración", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent iSetting = new Intent(MainActivity.this, UserPreference.class);
                        startActivity(iSetting);
                    }
                })
                .setActionTextColor(getResources().getColor(android.R.color.holo_red_light ))
                .show();
    }





    public void restart_appMSG(){
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        View parentLayout = findViewById(R.id.activity_main);
        Snackbar.make(parentLayout, "Conexión perdida", Snackbar.LENGTH_INDEFINITE)
                .setAction("Reintentar conectarse", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent i = getBaseContext().getPackageManager()
                              .getLaunchIntentForPackage(getBaseContext().getPackageName());
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                })
                .setActionTextColor(getResources().getColor(android.R.color.holo_red_light))
                .show();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(broadcastReceiverSpeed!= null){
            unregisterReceiver(broadcastReceiverSpeed);
        }

        if(broadcastReceiverBT!= null){
            unregisterReceiver(broadcastReceiverBT);
        }

        if(broadcastReceiverBTSensor!= null){
            unregisterReceiver(broadcastReceiverBTSensor);
        }
    }

    private void getPermission() {
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.SEND_SMS},PERMISSION);
    }

    private void turnOnBT() {
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if(myBluetooth == null){
            //Show a menssage. that the device has no bluetooth adapter
            Toast.makeText(getApplicationContext(), "Dispositivo Bluetooth no disponible", Toast.LENGTH_LONG).show();

            //finish apk
            finish();
        }else if(!myBluetooth.isEnabled()){
            //Ask to the user turn the bluetooth on
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon,1);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            goPreferences();
        }
        return super.onOptionsItemSelected(item);
    }

    public void goPreferences(){
        Intent intPref = new Intent(MainActivity.this, UserPreference.class);
        startActivity(intPref);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_manu, menu);
        return super.onCreateOptionsMenu(menu);
    }



    private void msg(String s) {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }










}
