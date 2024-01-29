package com.example.applayout.Communications;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applayout.R;

public class WifiActivity extends AppCompatActivity {
    Button aSwitch, discoverButton, selectButton, sendButton;
    ListView listView;
    TextView connectionStatus;

    WifiP2pManager manager;
    WifiP2pManager.Channel channel;

    BroadcastReceiver receiver;
    IntentFilter intentFilter;

    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;
    String filepath, fileName, extension;

    Socket socket;

    ServerClass serverClass;
    ClientClass clientClass;

    boolean isHost;

    private static final int FILE_REQUEST_CODE = 42;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 44;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);
        requestWriteExternalStoragePermission();
        initialWork();
        exqListener();
    }

    private void requestWriteExternalStoragePermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=
                PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }

    private void exqListener() {
        //onClickListener for WiFi Switch
        aSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivityForResult(intent,1);
            }
        });

        //onClickListener for discoverButton
        discoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText("Discovery Started");
                    }

                    @Override
                    public void onFailure(int i) {
                        connectionStatus.setText("Discovery Not Started");
                    }
                });
            }
        });

        //onItemClickListener for listView
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //Selecting position of the listView
                final WifiP2pDevice device = deviceArray[i];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText("Connected: " + device.deviceName);
                    }

                    @Override
                    public void onFailure(int i) {
                        connectionStatus.setText("Not Connected");
                    }
                });
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isHost){
                    clientClass.write();
                    connectionStatus.setText("File sent");
                } else if (isHost) {
                    serverClass.write();
                    connectionStatus.setText("File sent");
                }
            }
        });

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent,FILE_REQUEST_CODE);
            }
        });
    }

    private void initialWork(){
        //Declaration
        aSwitch = findViewById(R.id.btOnWiFi);
        discoverButton = findViewById(R.id.btDiscoverWifi);
        selectButton = findViewById(R.id.btSelectWifi);
        sendButton = findViewById(R.id.btSendWifi);
        listView = findViewById(R.id.lvWifi);
        connectionStatus = findViewById(R.id.connection_status);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager,channel,this);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
    }

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            //If peer list has been changed
            if(!wifiP2pDeviceList.getDeviceList().equals(peers)) {
                //Clear previous list and add new list
                peers.clear();
                peers.addAll(wifiP2pDeviceList.getDeviceList());

                //Size of Array
                deviceNameArray = new String[wifiP2pDeviceList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[wifiP2pDeviceList.getDeviceList().size()];

                //Adding values into the Array
                int index = 0;
                for (WifiP2pDevice device : wifiP2pDeviceList.getDeviceList()) {
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    index++;
                }


                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray);
                listView.setAdapter(adapter);
            }

            if(peers.size() == 0) {
                connectionStatus.setText("No Device Found");
                return;
            }
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
                connectionStatus.setText("Host");
                isHost = true;
                serverClass = new ServerClass();
                serverClass.start();
            } else if (wifiP2pInfo.groupFormed){
                connectionStatus.setText("Client");
                isHost = false;
                clientClass = new ClientClass(groupOwnerAddress);
                clientClass.start();
            }
        }
    };

    //Register broadcast receiver
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
    }

    // Unregister broadcast receiver
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    public class ServerClass extends Thread {
        ServerSocket serverSocket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public void write() {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, fileName + "." + extension);
            try {
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int bytes = 0;
                while ((bytes = fis.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, bytes);
                }
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    serverSocket = new ServerSocket(8888);
                    socket = serverSocket.accept();
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();

                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        socket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                if (socket != null) {
                    try {
                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "fileName.jpg");
                        FileOutputStream fos = new FileOutputStream(file);

                        byte[] buffer = new byte[1024];
                        int bytes;

                        while ((bytes = inputStream.read(buffer)) > 0) {
                            fos.write(buffer, 0, bytes);
                        }

                        inputStream.close();
                        outputStream.close();
                        fos.close();
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public class ClientClass extends Thread {
        String hostAdd;
        private InputStream inputStream;
        private OutputStream outputStream;

        public ClientClass(InetAddress hostAddress) {
            hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
        }

        public void write() {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, fileName + "." + extension);
            try {
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int bytes = 0;
                while ((bytes = fis.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, bytes);
                }
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    socket.connect(new InetSocketAddress(hostAdd, 8888), 500);
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        socket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

                if (socket != null) {
                    try {
                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "fileName.jpg");
                        FileOutputStream fos = new FileOutputStream(file);

                        byte[] buffer = new byte[1024];
                        int bytes;

                        while ((bytes = inputStream.read(buffer)) > 0) {
                            fos.write(buffer, 0, bytes);
                        }

                        inputStream.close();
                        outputStream.close();
                        fos.close();
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode == FILE_REQUEST_CODE && resultCode == RESULT_OK && data!=null){
            filepath = new File(data.getData().getPath()).getAbsolutePath();
            Uri selectedFileUri = data.getData();

            String filename;
            Cursor cursor = getContentResolver().query(selectedFileUri,null,null,null,null);
            if(cursor == null){
                filename = selectedFileUri.getPath();
            } else {
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
                filename = cursor.getString(idx);
                cursor.close();
            }
            fileName = filename.substring(0,filename.lastIndexOf("."));
            extension = filename.substring(filename.lastIndexOf(".")+1);
            connectionStatus.setText("Selected file: "+fileName);
        }
    }
}
