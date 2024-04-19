package com.example.applayout.Communications;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.applayout.FederatedLearning.FederatedLearningActivity;
import com.example.applayout.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity {
    BluetoothActivity context;
    Button listen, send, listDevices, selectFile;
    ListView listView;
    TextView status;

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[] btArray;
    SendReceive sendReceive;

    String filepath, fileName, extension;

    int REQUEST_ENABLE_BLUETOOTH = 1;

    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 7;
    static final int STATE_MESSAGE_SENT = 6;
    private static final String APP_NAME = "FileTransfer";
    private static final UUID MY_UUID = UUID.fromString("b81e7ee8-564e-11ee-8c99-0242ac120002");

    private static final int FILE_REQUEST_CODE = 42;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 44;
    private static final int YOUR_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        requestWriteExternalStoragePermission();
        findViewByIdes();

        context = this;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            status.setText("Bluetooth is not supported on this device");
            finish();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, YOUR_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT);
            } else {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
            }
        } else {
            checkPermissions();
        }

        implementListeners();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only check for Marshmallow and above
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Show an explanation to the user *asynchronously*
                    Toast.makeText(this, "This app needs storage access to download files.", Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
                }
            }
        }
    }

    private void requestWriteExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }

    private void findViewByIdes() {
        listen = findViewById(R.id.btListenBluetooth);
        send = findViewById(R.id.btSendBluetooth);
        listView = findViewById(R.id.lvBluetooth);
        status = findViewById(R.id.status_title);
        listDevices = findViewById(R.id.btListDeviceBluetooth);
        selectFile = findViewById(R.id.btSelectBluetooth);
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, YOUR_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case YOUR_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted
                    Toast.makeText(this, "Bluetooth connect permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    // Permission was denied
                    Toast.makeText(this, "Bluetooth connect permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            case PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    Toast.makeText(this, "Storage Write permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    // permission denied, boo!
                    Toast.makeText(this, "Storage Write permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }


    private void implementListeners() {
        listDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    Set<BluetoothDevice> bt = bluetoothAdapter.getBondedDevices();
                    String[] strings = new String[bt.size()];
                    btArray = new BluetoothDevice[bt.size()];
                    int index = 0;
                    if (bt.size() > 0) {
                        // Use the bonded devices as needed
                        for (BluetoothDevice device : bt) {
                            btArray[index] = device;
                            strings[index] = device.getName();
                            index++;
                        }
                        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,strings);
                        listView.setAdapter(arrayAdapter);
                    }
                } else {
                    checkBluetoothPermissions(); // Ask for permission
                }
            }
        });

        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServerClass serverClass = new ServerClass();
                serverClass.start();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ClientClass clientClass = new ClientClass(btArray[position]);
                clientClass.start();

                status.setText("Connecting");
            }
        });

        selectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent,FILE_REQUEST_CODE);
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendReceive.write();
                //status.setText("File sent");
            }
        });
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case STATE_LISTENING:
                    status.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    status.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    status.setText("Connected");
                    //Intent intent = new Intent(BluetoothActivity.this, FederatedLearningActivity.class);
                    //startActivity(intent);
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Connection Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    status.setText("File Received");
                    Intent intent = new Intent(BluetoothActivity.this, FederatedLearningActivity.class);
                    startActivity(intent);
                    break;
                case STATE_MESSAGE_SENT:
                    status.setText("File Sent");
                    break;
            }
            return false;
        }
    });

    private class ServerClass extends Thread{
        private BluetoothServerSocket serverSocket;

        public ServerClass(){
            try{
                //serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUID);
                if (ContextCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUID);
                } else {
                    checkBluetoothPermissions();  // Request permission if not already granted
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        public void run() {
            BluetoothSocket socket = null;
            while(socket == null){
                try{
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);
                    socket = serverSocket.accept();
                } catch (IOException e){
                    e.printStackTrace();
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                if(socket != null){
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);
                    sendReceive = new SendReceive(socket);
                    sendReceive.start();
                    break;
                }
            }
        }
    }

    private class ClientClass extends Thread{
        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass (BluetoothDevice device1){
            device = device1;
            try {
                if (ContextCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                } else {
                    checkBluetoothPermissions();  // Request permission if not already granted
                }
                //socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            try {
                if (ContextCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    socket.connect();
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);

                    sendReceive = new SendReceive(socket);
                    sendReceive.start();
                } else {
                    checkBluetoothPermissions();  // Request permission if not already granted
                }
                /*socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive = new SendReceive(socket);
                sendReceive.start();*/

            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    private class SendReceive extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive (BluetoothSocket socket){
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try{
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            } catch (IOException e){
                e.printStackTrace();
                handler.obtainMessage(STATE_CONNECTION_FAILED).sendToTarget();
            }
            inputStream = tempIn;
            outputStream= tempOut;
        }

        //receive
        @Override
        public void run() {
            //byte[] buffer = new byte[1024];
            //int bytes;
            //FileOutputStream fos = null;
            //while(true) {
                try {
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "model_new1.tflite");
                    FileOutputStream fos = new FileOutputStream(file);

                    byte[] buffer = new byte[1024];
                    int bytes;

                    while ((bytes = inputStream.read(buffer)) != -1) {  // Ensure it reads until the end of the stream
                        fos.write(buffer, 0, bytes);
                    }
                    Log.d("BluetoothActivity", "File received and written successfully.");
                } catch (IOException e) {
                    Log.e("BluetoothActivity", "Error while receiving or writing file.", e);
                } finally {
                    try {
                        //if (fos != null) fos.close();
                        if (inputStream != null) inputStream.close();  // Close streams in the finally block
                        Log.d("BluetoothActivity", "Sending message that file was received.");
                        handler.obtainMessage(STATE_MESSAGE_RECEIVED).sendToTarget();
                    } catch (IOException ex) {
                        Log.e("BluetoothActivity", "Error closing file streams.", ex);
                    }
                }
            //}
            /*while (!Thread.currentThread().isInterrupted()) {
                FileOutputStream fos = null;
                try {
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "model.tflite");
                    fos = new FileOutputStream(file);

                    byte[] buffer = new byte[1024];
                    int bytes;

                    while ((bytes = inputStream.read(buffer)) > 0) {
                        fos.write(buffer, 0, bytes);
                    }
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                } finally {
                    try {
                        if (fos != null) {
                            fos.close(); // Ensure the FileOutputStream is closed after use
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }*/
        }

        //send
        public void write(){
            try {
                MappedByteBuffer buffer = loadModelFile(context.getAssets(), "model.tflite");
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                outputStream.write(bytes);
                handler.obtainMessage(STATE_MESSAGE_SENT).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
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
            status.setText("Selected file: "+fileName);
        }
    }

    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
