package com.kaka.bluetoothble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Build;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.kaka.bluetoothble.adapter.BleAdapter;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

public class MainActivity extends AppCompatActivity {

    private static final String TAG ="ble_tag" ;
    ProgressBar pbSearchBle;
    ImageView ivSerBleStatus;
    TextView tvSerBleStatus;
    TextView tvSerBindStatus;
    ListView bleListView;
    private LinearLayout operaView;
    private Button btnWrite;
    private Button btnRead;
    private EditText etWriteContent;
    private TextView tvResponse;
    private List<BluetoothDevice> mDatas;
    private List<Integer> mRssis;
    private BleAdapter mAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private boolean isScaning=false;
    private boolean isConnecting=false;
    private BluetoothGatt mBluetoothGatt;

    //??????????????????
    private UUID write_UUID_service;
    private UUID write_UUID_chara;
    private UUID read_UUID_service;
    private UUID read_UUID_chara;
    private UUID notify_UUID_service;
    private UUID notify_UUID_chara;
    private UUID notify_UUID_CONFIG ;  // saian++
    private UUID indicate_UUID_service;
    private UUID indicate_UUID_chara;
    private String hex="7B46363941373237323532443741397D";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_device);

        initView();
        initData();
        mBluetoothManager= (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter=mBluetoothManager.getAdapter();
        if (mBluetoothAdapter==null||!mBluetoothAdapter.isEnabled()){
            Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,0);
        }

    }

    private void initData() {
        mDatas=new ArrayList<>();
        mRssis=new ArrayList<>();
        mAdapter=new BleAdapter(MainActivity.this,mDatas,mRssis);
        bleListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    private void initView(){
        pbSearchBle=findViewById(R.id.progress_ser_bluetooth);
        ivSerBleStatus=findViewById(R.id.iv_ser_ble_status);
        tvSerBindStatus=findViewById(R.id.tv_ser_bind_status);
        tvSerBleStatus=findViewById(R.id.tv_ser_ble_status);
        bleListView=findViewById(R.id.ble_list_view);
        operaView=findViewById(R.id.opera_view);
        btnWrite=findViewById(R.id.btnWrite);
        btnRead=findViewById(R.id.btnRead);
        etWriteContent=findViewById(R.id.et_write);
        tvResponse=findViewById(R.id.tv_response);
        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readData();
            }
        });

        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //??????????????????
                writeData();
            }
        });


        ivSerBleStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isScaning){
                    tvSerBindStatus.setText("????????????");
                    stopScanDevice();
                }else{
                    checkPermissions();
                }
                /*new Thread() {
                    @Override
                    public void run() {
                        //just for debug function "sendByPost()"
                        byte[] tmp = {0x11, 0x22, 0x33, 0x44, 0x33, 0x22, 0x11};
                        sendByPost(tmp);
                    }
                }.start();*/
            }
        });
        bleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (isScaning){
                    stopScanDevice();
                }
                if (!isConnecting){
                    isConnecting=true;
                    BluetoothDevice bluetoothDevice= mDatas.get(position);
                    //????????????
                    tvSerBindStatus.setText("?????????");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mBluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this,
                                true, gattCallback, TRANSPORT_LE);
                    } else {
                        mBluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this,
                                true, gattCallback);
                    }
                }

            }
        });


    }

    private void readData() {
        BluetoothGattCharacteristic characteristic=mBluetoothGatt.getService(read_UUID_service)
                .getCharacteristic(read_UUID_chara);
        mBluetoothGatt.readCharacteristic(characteristic);
    }


    /**
     * ???????????? 10??????????????????
     * */
    private void scanDevice(){
        tvSerBindStatus.setText("????????????");
        isScaning=true;
        pbSearchBle.setVisibility(View.VISIBLE);
        mBluetoothAdapter.startLeScan(scanCallback);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //????????????
                mBluetoothAdapter.stopLeScan(scanCallback);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isScaning=false;
                        pbSearchBle.setVisibility(View.GONE);
                        tvSerBindStatus.setText("???????????????");
                    }
                });
            }
        },10000);
    }

    /**
     * ????????????
     * */
    private void stopScanDevice(){
        isScaning=false;
        pbSearchBle.setVisibility(View.GONE);
        mBluetoothAdapter.stopLeScan(scanCallback);
    }


    BluetoothAdapter.LeScanCallback scanCallback=new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.e(TAG, "run: scanning...");
            if (!mDatas.contains(device)){
                mDatas.add(device);
                mRssis.add(rssi);
                mAdapter.notifyDataSetChanged();
            }

        }
    };

    private BluetoothGattCallback gattCallback=new BluetoothGattCallback() {
        /**
         * ??????????????? ???????????????????????????
         * */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.e(TAG,"onConnectionStateChange()");
            if (status==BluetoothGatt.GATT_SUCCESS){
                //????????????
                if (newState== BluetoothGatt.STATE_CONNECTED){
                    Log.e(TAG,"????????????");
                    //????????????
                    gatt.discoverServices();
                }
            }else{
                //????????????
                Log.e(TAG,"??????=="+status);
                mBluetoothGatt.close();
                isConnecting=false;
            }
        }
        /**
         * ????????????????????????????????????
         * */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            //???????????????????????????????????????????????????
            isConnecting=false;
            Log.e(TAG,"onServicesDiscovered()---????????????");
            //?????????????????????????????????
            initServiceAndChara();
            //????????????
            mBluetoothGatt.setCharacteristicNotification(mBluetoothGatt
                    .getService(notify_UUID_service).getCharacteristic(notify_UUID_chara),true);
            //saian++
            BluetoothGattDescriptor descriptor = mBluetoothGatt.getService(notify_UUID_service)
                    .getCharacteristic(notify_UUID_chara).getDescriptor(notify_UUID_CONFIG);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ;
            mBluetoothGatt.writeDescriptor(descriptor) ;
            //++saian

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bleListView.setVisibility(View.GONE);
                    operaView.setVisibility(View.VISIBLE);
                    tvSerBindStatus.setText("?????????");
                    //saian++
                    addText(tvResponse, "connection builded");
                }
            });
        }
        /**
         * ??????????????????
         * */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.e(TAG,"onCharacteristicRead()");
        }
        /**
         * ??????????????????
         * */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            Log.e(TAG,"onCharacteristicWrite()  status="+status+",value="+HexUtil.encodeHexStr(characteristic.getValue()));
        }
        /**
         * ??????????????????????????????
         * */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.e(TAG,"onCharacteristicChanged()"+characteristic.getValue());
            final byte[] data=characteristic.getValue();
            runOnUiThread(new Runnable() {
                @Override
                public void run() { addText(tvResponse,bytes2hex(data)); }
            });
            // saian+ http post
            new Thread()
            {
                @Override
                public void run() {
                    sendByPost(data);
                }
            }.start();
        }
    };
    /**
     * ????????????
     */
    private void checkPermissions() {
        RxPermissions rxPermissions = new RxPermissions(MainActivity.this);
        rxPermissions.request(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(new io.reactivex.functions.Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            // ???????????????????????????
                            scanDevice();
                        } else {
                            // ?????????????????????????????????????????????????????????
                            ToastUtils.showLong("?????????????????????????????????");
                        }
                    }
                });
    }


    private void initServiceAndChara(){
        /*
        List<BluetoothGattService> bluetoothGattServices= mBluetoothGatt.getServices();
        for (BluetoothGattService bluetoothGattService:bluetoothGattServices){
            List<BluetoothGattCharacteristic> characteristics=bluetoothGattService.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic:characteristics){
                int charaProp = characteristic.getProperties();
                Log.d(TAG, "charaProp="+charaProp);
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    read_UUID_chara=characteristic.getUuid();
                    read_UUID_service=bluetoothGattService.getUuid();
                    Log.e(TAG,"read_chara="+read_UUID_chara+"----read_service="+read_UUID_service);
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                    write_UUID_chara=characteristic.getUuid();
                    write_UUID_service=bluetoothGattService.getUuid();
                    Log.e(TAG,"write_chara="+write_UUID_chara+"----write_service="+write_UUID_service);
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                    write_UUID_chara=characteristic.getUuid();
                    write_UUID_service=bluetoothGattService.getUuid();
                    Log.e(TAG,"write_chara="+write_UUID_chara+"----write_service="+write_UUID_service);

                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    notify_UUID_chara=characteristic.getUuid();
                    notify_UUID_service=bluetoothGattService.getUuid();
                    Log.e(TAG,"notify_chara="+notify_UUID_chara+"----notify_service="+notify_UUID_service);
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                    indicate_UUID_chara=characteristic.getUuid();
                    indicate_UUID_service=bluetoothGattService.getUuid();
                    Log.e(TAG,"indicate_chara="+indicate_UUID_chara+"----indicate_service="+indicate_UUID_service);

                }
            }
        }*/
        write_UUID_service = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
        write_UUID_chara = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
        notify_UUID_service = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
        notify_UUID_chara = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
        notify_UUID_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    }

    private void addText(TextView textView, String content) {
        textView.append(content);
        textView.append("\n");
        int offset = textView.getLineCount() * textView.getLineHeight();
        if (offset > textView.getHeight()) {
            textView.scrollTo(0, offset - textView.getHeight());
        }
    }

    private void writeData(){
        BluetoothGattService service=mBluetoothGatt.getService(write_UUID_service);
        BluetoothGattCharacteristic charaWrite=service.getCharacteristic(write_UUID_chara);
        //saian++
        Log.d(TAG, "write: Service="+write_UUID_service+"---Char="+write_UUID_chara);
        byte[] data;
        String content=etWriteContent.getText().toString();
        if (!TextUtils.isEmpty(content)){
            data=HexUtil.hexStringToBytes(content);
        }else{
            data=HexUtil.hexStringToBytes(hex);
        }
        if (data.length>20){//????????????????????? ???????????????
            Log.e(TAG, "writeData: length="+data.length);
            int num=0;
            if (data.length%20!=0){
                num=data.length/20+1;
            }else{
                num=data.length/20;
            }
            for (int i=0;i<num;i++){
                byte[] tempArr;
                if (i==num-1){
                    tempArr=new byte[data.length-i*20];
                    System.arraycopy(data,i*20,tempArr,0,data.length-i*20);
                }else{
                    tempArr=new byte[20];
                    System.arraycopy(data,i*20,tempArr,0,20);
                }
                charaWrite.setValue(tempArr);
                mBluetoothGatt.writeCharacteristic(charaWrite);
            }
        }else{
            charaWrite.setValue(data);
            mBluetoothGatt.writeCharacteristic(charaWrite);
        }
    }

    private static final String HEX = "0123456789abcdef";
    public static String bytes2hex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
        {
            // ????????????????????????4???????????????0x0f????????????????????????0-15????????????????????????HEX.charAt(0-15)??????16?????????
            sb.append(HEX.charAt((b >> 4) & 0x0f));
            // ?????????????????????????????????0x0f????????????????????????0-15????????????????????????HEX.charAt(0-15)??????16?????????
            sb.append(HEX.charAt(b & 0x0f));
        }
        return sb.toString();
    }

    /**
     * send data using http post. saian+
     */
    private void sendByPost(byte[] data)
    {
        HttpURLConnection connection= null;
        try
        {
            URL url = new URL("http://192.168.62.205:30082/character");
            //URL url = new URL("http://192.168.24.115:8090");
            connection = (HttpURLConnection)url.openConnection() ;
            connection.setConnectTimeout(1000) ;
            connection.setReadTimeout(1000);

            connection.setRequestMethod("POST");
            connection.setUseCaches(true);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-type", "text/plain");

            connection.connect();

            OutputStream dataPost = connection.getOutputStream();
            //dataPost.write(data);            // saian+: for bytes output
            String tmp = bytes2hex(data);
            dataPost.write(tmp.getBytes());  // saian+: for string output
            dataPost.flush();
            dataPost.close() ;
            int responseCode = connection.getResponseCode();
            Log.e(TAG, "code="+responseCode) ;
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothGatt.disconnect();
    }
}
