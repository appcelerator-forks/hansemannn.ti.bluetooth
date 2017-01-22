/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package ti.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.titanium.TiApplication;
import android.Manifest;

@Kroll.module(name = "TiBluetooth", id = "ti.bluetooth")
public class TiBluetoothModule extends KrollModule {

    public interface ConnectionCallback {
        void onConnectionStateChange(BluetoothDevice device, int newState);
    }

    public static final String LCAT = "BLE";
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private TiApplication appContext;
    private Activity activity;
    private BluetoothLeScanner btScanner;

    @Kroll.constant 
    public static final int MANAGER_STATE_UNKNOWN      = 0;
    @Kroll.constant 
    public static final int MANAGER_STATE_UNSUPPORTED  = 1;
    @Kroll.constant 
    public static final int MANAGER_STATE_UNAUTHORIZED = 2;
    @Kroll.constant 
    public static final int MANAGER_STATE_POWERED_OFF  = 10;
    @Kroll.constant 
    public static final int MANAGER_STATE_POWERED_ON   = 12;
    @Kroll.constant 
    public static final int MANAGER_STATE_RESETTING    = 5;
    @Kroll.constant 
    public static final int SCAN_MODE_BALANCED         = 1;
    @Kroll.constant 
    public static final int SCAN_MODE_LOW_LATENCY      = 2;
    @Kroll.constant 
    public static final int SCAN_MODE_LOW_POWER        = 0;
    @Kroll.constant 
    public static final int SCAN_MODE_OPPORTUNISTIC    = -1;

    private int currentState = MANAGER_STATE_UNKNOWN;
    private int currentScanMode = SCAN_MODE_LOW_POWER;
    private boolean isScanning = false;

    public TiBluetoothModule() {
        super();
        appContext = TiApplication.getInstance();
        activity   = appContext.getCurrentActivity();
        
        IntentFilter intentFilter = new IntentFilter();
        
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        appContext.registerReceiver(new TiBluetoohBroadcastReceiver(TiBluetoothModule.this, btAdapter), intentFilter);
    }

    @Kroll.onAppCreate
    public static void onAppCreate(TiApplication app) {
        Log.d(LCAT, "inside onAppCreate");
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device != null) {
                TiBluetoothDeviceProxy btDeviceProxy = new TiBluetoothDeviceProxy(device);
                
                Log.d(LCAT, "Found something " + btDeviceProxy.getAddress());
                if (btDeviceProxy.getAddress() != null) {
                    
                    KrollDict kd = new KrollDict();
                    kd.put("name", btDeviceProxy.getName());
                    //kd.put("device", btDeviceProxy);  // will crash
                    kd.put("address", btDeviceProxy.getAddress());
                    kd.put("ids", btDeviceProxy.getUuids());
                    fireEvent("didDiscoverPeripheral", kd);

                    BluetoothGatt bluetoothGatt = device.connectGatt(appContext, false, new TiBluetoothGattCallbackHandler(TiBluetoothModule.this));
                    btScanner.stopScan(scanCallback);
                }
            }
        }
    };

    private List<ScanFilter> scanFilters(String[] ids) {
        List<ScanFilter> list = new ArrayList<ScanFilter>(1);

        for (int i = 0; i < ids.length; i++) {
            ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(ids[i])).build();
            list.add(filter);
        }
        return list;
    }
    
    public void setCurrentState(int cs) {
		currentState = cs;
    }

    @Kroll.getProperty
    @Kroll.method
    public int getState() {
        return currentState;
    }

    @Kroll.getProperty
    @Kroll.method
    public boolean isScanning() {
        return isScanning;
    }

    @Kroll.method
    public void initialize() {
        // TODO check if permissions are correctly set:
        //int permissionCheck = activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        btManager = (BluetoothManager)appContext.getSystemService(appContext.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        if (btAdapter != null) {
            Log.d(LCAT, "BT init");
            currentState = btAdapter.getState();
        } else {
            currentState = MANAGER_STATE_UNSUPPORTED;
        }
    }

    @Kroll.getProperty
    @Kroll.method
    public int getScanMode() {
        return currentScanMode;
    }

    @Kroll.setProperty
    @Kroll.method
    public void setScanMode(int scanMode) {
        if (scanMode == SCAN_MODE_BALANCED || scanMode == SCAN_MODE_LOW_POWER || scanMode == SCAN_MODE_LOW_LATENCY || scanMode == SCAN_MODE_OPPORTUNISTIC) {
            currentScanMode = scanMode;
        } else {
            currentScanMode = SCAN_MODE_LOW_POWER;
        }

    }

    @Kroll.method
    public void startScan() {
        // start scanning for every device
        if (btAdapter != null) {
            ScanSettings settings = new ScanSettings.Builder().setScanMode(currentScanMode).build();
            btScanner = btAdapter.getBluetoothLeScanner();
            btScanner.startScan(new ArrayList<ScanFilter>(), settings, scanCallback);
            isScanning = true;
            Log.i(LCAT,"Start scan");
        }
    }

    @Kroll.method
    public void startScanWithServices(String[] obj) {
        // start scanning for a list of devices
        if (btAdapter != null) {
            ScanSettings settings = new ScanSettings.Builder().setScanMode(currentScanMode).build();
            btScanner = btAdapter.getBluetoothLeScanner();
            btScanner.startScan(scanFilters(obj), settings, scanCallback);
            isScanning = true;
        }
    }

    @Kroll.method
    public void stopScan() {
        // stop scanning
        if (btAdapter != null) {
            btScanner.stopScan(scanCallback);
            isScanning = false;
        }
    }
    
    @Kroll.method
	public void flushPendingScanResults() {
		if (btAdapter != null) {
			btScanner.flushPendingScanResults(scanCallback);
			//isScanning = false;
		}
    }
}
