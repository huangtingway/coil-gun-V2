package com.example.carcontrol2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity {
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothChangeReciver bluetoothChangeReciver;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothDevice targetBluetoothDevice;
    private BluetoothGatt leftGatt, rightGatt, comGatt;
    private BluetoothGattService bluetoothGattServices;
    private BluetoothGattCharacteristic characteristic;

    private AlertDialog openBLDialog, scanningDialog;
    private MaterialButton leftUpBtn, leftDownBtn, rightUpBtn, rightDownBtn, cameraUpBtn, cameraDownBtn, leftRotateBtn, rightRotateBtn, reloadBtn, rechargeBtn, fireBtn;
    private TextView bulletText;
    private MaterialSwitch autoReloadSwitch;
    private boolean isReload, isCharge;

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        bluetoothChangeReciver = new BluetoothChangeReciver();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothChangeReciver, filter);
    }

    private class BluetoothChangeReciver extends BroadcastReceiver { //BLE state change
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            if (state == BluetoothAdapter.STATE_OFF) {
                showRequestOpenBLDialog();
                Intent intentopen = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(intentopen);
            } else if (state == BluetoothAdapter.STATE_ON) {
                openBLDialog.cancel();
                scanDevice();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().setNavigationBarColor(Color.parseColor("#111111"));
        checkPremission();
        initView();
        initBluetooth();
        setBtnListener();
    }

    @SuppressLint("NewApi")
    private void setBtnListener() {
        leftUpBtn.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    sendBTData(leftGatt, "leftUp*");
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    sendBTData(leftGatt, "leftStop*");
                }
                return true;
            }
        });
        leftDownBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    sendBTData(leftGatt, "leftDown*");
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    sendBTData(leftGatt, "leftStop*");
                }
                return true;
            }
        });
        rightUpBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    sendBTData(rightGatt, "rightUp*");
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    sendBTData(rightGatt, "rightStop*");
                }
                return true;
            }
        });
        rightDownBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    sendBTData(rightGatt, "rightDown*");
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    sendBTData(rightGatt, "rightStop*");
                }
                return true;
            }
        });
        cameraUpBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    sendBTData(comGatt, "cameraUp*");
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    sendBTData(comGatt, "cameraStop*");
                }
                return true;
            }
        });
        cameraDownBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    sendBTData(comGatt, "cameraDown*");
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    sendBTData(comGatt, "cameraStop*");
                }
                return true;
            }
        });
        reloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBTData(comGatt, "reload*");
                isReload = true;
            }
        });
        rechargeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBTData(comGatt, "recharge*");
                isCharge = true;
            }
        });
        fireBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isCharge && isReload) {
                    sendBTData(comGatt, "fire*");
                    isCharge = false;
                    isReload = false;
                }
            }
        });
        leftRotateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBTData(comGatt, "leftRotate*");
            }
        });
        rightRotateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBTData(comGatt, "rightRotate*");
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void sendBTData(BluetoothGatt gatt, String data) {
        int result = gatt.writeCharacteristic(characteristic, data.getBytes(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        Log.v("BLEresult", data + " " + result);
    }

    private void initView() {
        leftUpBtn = findViewById(R.id.leftUpBtn);
        leftDownBtn = findViewById(R.id.leftDownBtn);
        rightUpBtn = findViewById(R.id.rightUpBtn);
        rightDownBtn = findViewById(R.id.rightDownBtn);
        cameraUpBtn = findViewById(R.id.cameraUpBtn);
        cameraDownBtn = findViewById(R.id.cameraDownBtn);
        reloadBtn = findViewById(R.id.reloadBtn);
        rechargeBtn = findViewById(R.id.rechargeBtn);
        fireBtn = findViewById(R.id.fireBtn);
        leftRotateBtn = findViewById(R.id.leftRotateBtn);
        rightRotateBtn = findViewById(R.id.rightRotateBtn);
        autoReloadSwitch = findViewById(R.id.autoReloadSwitch);
        bulletText = findViewById(R.id.bulletText);
    }

    @SuppressLint("InlinedApi")
    private void checkPremission() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "your device not support bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (checkSelfPermission(android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED
        ) {
            requestPermissions(new String[]{android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i : grantResults) {
            if (i == PackageManager.PERMISSION_DENIED) finish();
        }
    }

    private void showRequestOpenBLDialog() { //request open BLE
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("request Bluetooth");
        builder.setMessage("please open your device's bluetooth");
        builder.setCancelable(false);
        openBLDialog = builder.create();
        openBLDialog.show();
    }

    private void initBluetooth() { //init BLE, open bluetooth if BLE is off
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothAdapter.isEnabled()) {
            scanDevice();
        } else {
            showRequestOpenBLDialog();
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(intent);
        }
    }

    private void scanDevice() {
        bluetoothLeScanner.startScan(scanCallback);
        showScanningDialog();
    }

    private void showScanningDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("scanning Bluetooth");
        builder.setMessage("connect to receiver...");
        builder.setCancelable(false);
        scanningDialog = builder.create();
        scanningDialog.show();
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult resultItem : results) {
                if (resultItem.getDevice().getAddress().equals("A8:E2:C1:48:DD:20")) {
                    bluetoothLeScanner.stopScan(scanCallback);
                    targetBluetoothDevice = resultItem.getDevice();
                    leftGatt = targetBluetoothDevice.connectGatt(getApplicationContext(), true, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                    rightGatt = targetBluetoothDevice.connectGatt(getApplicationContext(), true, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                    comGatt = targetBluetoothDevice.connectGatt(getApplicationContext(), true, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                    bluetoothLeScanner.stopScan(scanCallback);
                }
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            String deviceName = result.getDevice().getName();
            String address = result.getDevice().getAddress();
            Log.v("BLEresult", deviceName + " " + address);
            if (result.getDevice().getAddress().equals("A8:E2:C1:48:DD:20")) {
                targetBluetoothDevice = result.getDevice();
                Log.v("BLEresult", "get device");
                leftGatt = targetBluetoothDevice.connectGatt(getApplicationContext(), true, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                rightGatt = targetBluetoothDevice.connectGatt(getApplicationContext(), true, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                comGatt = targetBluetoothDevice.connectGatt(getApplicationContext(), true, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                bluetoothLeScanner.stopScan(scanCallback);
            }
        }
    };

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scanningDialog.cancel();
                    }
                });
                Log.v("BLEresult", "success");
                gatt.discoverServices();
            } else {
                Log.v("BLEresult", "disconnect");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scanningDialog.show();
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            bluetoothGattServices = gatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
            if (bluetoothGattServices != null) {
                Log.v("BLEresult", "get service success");
                for (BluetoothGattCharacteristic ch : bluetoothGattServices.getCharacteristics()) {
                    Log.v("BLEresult", ch.getUuid().toString());
                }
                characteristic = bluetoothGattServices.getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"));
                leftGatt.setCharacteristicNotification(characteristic, true);
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
            byteArrayToHexStr(value);
        }

    };

    public void byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null) {
            return;
        }

        StringBuilder hex = new StringBuilder(byteArray.length * 2);
        for (byte aData : byteArray) {
            hex.append(String.format("%02X", aData));
        }
        String gethex = hex.toString();
        Log.v("BLEresult", gethex);
        operateReceiveCommand(gethex);
    }

    private void operateReceiveCommand(String gethex) {
        if (gethex.equals("01")) {
            Log.v("BLEresult", "ready to fire");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fireBtn.setEnabled(true);
                    fireBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D8534B")));
                }
            });
        } else if (gethex.equals("02")) {
            Log.v("BLEresult", "recharge finish");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    rechargeBtn.setEnabled(true);
                    rechargeBtn.setBackgroundColor(Color.parseColor("#1F82CA"));
                }
            });
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        leftGatt.connect();
        rightGatt.connect();
        comGatt.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        leftGatt.disconnect();
        rightGatt.disconnect();
        comGatt.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothChangeReciver);
        leftGatt.disconnect();
        rightGatt.disconnect();
        comGatt.disconnect();
    }
}