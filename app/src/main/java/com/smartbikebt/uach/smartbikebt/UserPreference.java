package com.smartbikebt.uach.smartbikebt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class UserPreference extends AppCompatActivity {


    @Bind(R.id.checkBoxSendSMS)
    CheckBox checkBoxSendSMS;
    @Bind(R.id.buttonSave)
    Button btnSave;
    @Bind(R.id.editTextContactNumber)
    EditText edtSaveNumber;
    @Bind(R.id.spinnerBluetooth)
    Spinner spinnerBluetooth;

    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;
    //public static String EXTRA_ADDRESS = "device_address";

    public String info;
    public String address;
    public int posSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_preference);
        ButterKnife.bind(this);
        backButton();
        initBluetooth();
        loadPreferences();


    }

    @OnClick(R.id.buttonSave)
    public void handleClick(){
        if(!checkBoxSendSMS.isChecked()){
            edtSaveNumber.setText("");
        }
        savePreferences();
        loadPreferences();

        // Reiniciar la app
        Intent i = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        android.os.Process.killProcess(android.os.Process.myPid());

    }

    public void loadPreferences(){
        SharedPreferences preference = getSharedPreferences("UserPreference", Context.MODE_PRIVATE);
        checkBoxSendSMS.setChecked(preference.getBoolean("checkedSMS", false));
        edtSaveNumber.setText(preference.getString("contactNumber", ""));
        spinnerBluetooth.setSelection(preference.getInt("posSpinner",0));
    }



    public void savePreferences(){
        SharedPreferences preference = getSharedPreferences("UserPreference", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = preference.edit();
        edit.putBoolean("checkedSMS", checkBoxSendSMS.isChecked());
        edit.putString("contactNumber", edtSaveNumber.getText().toString());
        edit.putInt("posSpinner", posSpinner);
        edit.putString("address", address);
        checkBoxSendSMS.setChecked(preference.getBoolean("checkedSMS", false));
        edtSaveNumber.setText(preference.getString("contactNumber", ""));
        //msg("Valor: " + posSpinner);
        edit.commit();
    }

    private void backButton() {
        //Habilita el boton back en la interfaz
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public void onBackPressed() {
        this.finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //Write your logic here
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    private void initBluetooth() {

        //if the device has bluetooth
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
        pairedDevicesList();
    }

    private void pairedDevicesList()
    {
        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList list = new ArrayList();

        if (pairedDevices.size()>0){
            for(BluetoothDevice bt : pairedDevices){
                list.add(bt.getName() + " " + bt.getAddress()); //Get the device's name and the address
            }
        }
        else{
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerBluetooth.setAdapter(adapter);
        spinnerBluetooth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Object item = parent.getItemAtPosition(pos);
                info = item.toString();
                address = info.substring(info.length() - 17);
                posSpinner = pos;
            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }
}
