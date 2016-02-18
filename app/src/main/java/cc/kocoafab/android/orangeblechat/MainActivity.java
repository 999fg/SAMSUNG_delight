/*
    Copyright (c) 2005 nepes, kocoafab
    See the file license.txt for copying permission.
 */
package cc.kocoafab.android.orangeblechat;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cc.kocoafab.android.bluetooth.BluetoothService;
import cc.kocoafab.android.bluetooth.BluetoothServiceCallback;
import cc.kocoafab.android.bluetooth.BluetoothServiceFactory;
import cc.kocoafab.orangeblechat.R;

/**
 * 오렌지보드BLE와 메시지를 주고 받는 채팅 어플리케이션으로 메시지는 '\n' 로 구분된다.
 */
public class MainActivity extends Activity implements BluetoothServiceCallback, View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    // 블루투스 연결 요청 식별자
    private static final int REQUEST_BT_ENABLE = 1;

    // 블루투스 장치 검색 유효 시간 (10초)
    private static final long SCAN_PERIOD = 10000;

    // 비동기 UI 처리 핸들러
    private Handler mHandler;

    // 블루투스 장치 검색 다이얼로그 뷰
    private Dialog mScanDialog;
    private LinearLayout mDialogScanningLabel;
    private TextView mDialogScanEnable;
    private ListView mScannedDeviceList;
    private ScannedDeviceListAdapter mScannedDeviceListAdapter;

    // 블루투스 서비스
    private BluetoothService mBluetoothService;

    // 검색된 블루투스 장치 리스트
    private static List<ScannedDevice> mDevicesScanned = new ArrayList<ScannedDevice>();

    // 선택된 디바이스 식별자 (주소)
    private String mSelectedDeviceAddress;

    // 블루투스 수신 데이터 버퍼
    private byte[] remained = null;


    private SoundPool m_soundpool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
    private int m_sound_idc;
    private int m_sound_idd;
    private int m_sound_ide;
    private int m_sound_idf;
    private int m_sound_idg;
    private int m_sound_ida;
    private int m_sound_idb;
    private int m_sound_idc6;

    private SurfaceView surface;
    private SurfaceHolder s_holder;
    //public ViewThread sThread;
    private int clicked = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();

        mBluetoothService = BluetoothServiceFactory.getService(BluetoothServiceFactory.BT_LOW_ENERGY);
        mBluetoothService.setServiceCallback(this);



        m_sound_idc = m_soundpool.load(getApplicationContext(), R.raw.c5, 1);
        m_sound_idd = m_soundpool.load(getApplicationContext(), R.raw.d5, 1);
        m_sound_ide = m_soundpool.load(getApplicationContext(), R.raw.e5, 1);
        m_sound_idf = m_soundpool.load(getApplicationContext(), R.raw.f5, 1);
        m_sound_idg = m_soundpool.load(getApplicationContext(), R.raw.g5, 1);
        m_sound_ida = m_soundpool.load(getApplicationContext(), R.raw.a5, 1);
        m_sound_idb = m_soundpool.load(getApplicationContext(), R.raw.b5, 1);
        m_sound_idc6 = m_soundpool.load(getApplicationContext(), R.raw.c6, 1);

        surface = (SurfaceView) findViewById(R.id.surface);
        s_holder = surface.getHolder();

        s_holder.addCallback(new SurfaceHolder.Callback() {
            Bitmap whiteKey, blackKey, orangeKey;
            Paint paint = new Paint();
            public void surfaceCreated(SurfaceHolder holder) {
                int ww = 0, wh = 0, bw = 0, bh = 0, ow = 0, oh = 0;
                Canvas canvas = holder.lockCanvas();
                if (whiteKey == null) {
                    whiteKey = BitmapFactory.decodeResource(getResources(), R.drawable.whitekey);
                    Double dww = whiteKey.getWidth() * 1.47;
                    ww = dww.intValue();
                    wh = whiteKey.getHeight();
                }
                if (blackKey == null) {
                    blackKey = BitmapFactory.decodeResource(getResources(), R.drawable.blackkey);
                    Double dbw = blackKey.getWidth() * 0.5;
                    bw = dbw.intValue();
                    bh = blackKey.getHeight();
                }
                if (orangeKey == null) {
                    orangeKey = BitmapFactory.decodeResource(getResources(), R.drawable.orangekey);
                    Double dow = orangeKey.getWidth() * 1.47;
                    ow = dow.intValue();
                    oh = orangeKey.getHeight();
                }
                for (int i = 0; i < 8; i++) {
                    Rect dst = new Rect(8 + i * ww, 0, 8 + (i + 1) * ww, wh + 193);
                    canvas.drawBitmap(whiteKey, null, dst, paint);
                }
                for (int i = 0; i < 8; i++) {
                    if (i != 2 && i != 6) {
                        Rect dst = new Rect(95 + i * ww, 0, 45 + (i + 1) * ww, bh + 80);
                        canvas.drawBitmap(blackKey, null, dst, paint);
                    }
                }
                holder.unlockCanvasAndPost(canvas);
            }

            public void surfaceDestroyed(SurfaceHolder holder) {

            }

            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }
        });
    }

    /*
     * 블루투스 검색 및 선택을 위한 다이얼로그를 표시
     */
    private void showScanDialog() {
        dismissScanDialog();
        clearDevices();

        mScanDialog = new Dialog(this, R.style.lightbox_dialog);
        mScanDialog.setContentView(R.layout.view_scan_dialog);

        mDialogScanningLabel = (LinearLayout)mScanDialog.findViewById(R.id.llDialogScanning);
        mScannedDeviceList = (ListView)mScanDialog.findViewById(R.id.lvScannedDeviceList);
        mScannedDeviceListAdapter = new ScannedDeviceListAdapter(this, mDevicesScanned);

        mDialogScanEnable = (TextView)mScanDialog.findViewById(R.id.tvDialogScanEnable);
        mDialogScanEnable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDialogScanEnable.getText().equals("중지")) {
                    doDeviceScanning(false);
                } else {
                    doDeviceScanning(true);
                }
            }
        });

        mScannedDeviceList.setAdapter(mScannedDeviceListAdapter);

        // 블루투스 장치 검색 다이얼로그에서 검색된 블루투스 장치에 대한 클릭 이벤트 설정
        mScannedDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ScannedDevice item = mDevicesScanned.get(position);
                if (item.getState() == ScannedDevice.DEVICE_CONNECTED) {
                    item.setState(ScannedDevice.DEVICE_DISCONNECT);
                    mBluetoothService.disconnect(item.getAddress());
                    mScannedDeviceListAdapter.changeItemState(view, item.getState());

                } else if (item.getState() == ScannedDevice.DEVICE_WAITING) {
                    item.setState(ScannedDevice.DEVICE_CONNECT);
                    mBluetoothService.connect(item.getAddress());
                    mScannedDeviceListAdapter.changeItemState(view, item.getState());
                }
            }
        });

        mScanDialog.show();

        bluetoothInitialize();
    }

    /**
     * 블루투스 검색 및 선택을 위한 다이얼로그를 삭제
     */
    public void dismissScanDialog() {
        mBluetoothService.stopScan();
        if (mScanDialog != null) {
            mScanDialog.dismiss();
        }
        mScannedDeviceList = null;
        mScannedDeviceListAdapter = null;
        mScanDialog = null;
    }


    /**
     * 연결되지 않은 블루투스를 검색리스트에서 삭제한다. (리스트 갱신 목적)
     */
    public void clearDevices() {
        for (int i = mDevicesScanned.size() - 1 ; i >= 0 ; i--) {
            ScannedDevice device = mDevicesScanned.get(i);
            if (!mBluetoothService.isConnected(device.getAddress())) {
                mDevicesScanned.remove(i);
                mBluetoothService.delDeviceScanned(device.getAddress());
            }
        }
    }

    /**
     * 블루투스 장치 연결시 처리
     * @param position
     */
    public void deviceConnected(final int position) {
        final ScannedDevice item = mDevicesScanned.get(position);
        item.setState(ScannedDevice.DEVICE_CONNECTED);
        displayLocalMessage(item.getAddress(), "Connected.");
        mSelectedDeviceAddress = item.getAddress();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mScannedDeviceListAdapter.changeItemState(getView(position), item.getState());
                dismissScanDialog();
            }
        });
    }

    /**
     * 블루투스 장치 연결해지시 처리
     * @param position
     */
    public void deviceDisconnected(final int position) {
        final ScannedDevice item = mDevicesScanned.get(position);
        item.setState(ScannedDevice.DEVICE_WAITING);
        displayLocalMessage(item.getAddress(), "Disconnected.");
        mSelectedDeviceAddress = null;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mScannedDeviceListAdapter.changeItemState(getView(position), item.getState());
            }
        });
    }

    /**
     * 사용자 메시지를 표시한다.
     * @param txt
     */
    private void showMessage(String txt) {
        Toast.makeText(this, txt, Toast.LENGTH_SHORT).show();
    }

    /**
     * 선택된 블루투스 장치에 대한 뷰를 조회한다.
     * @param position
     * @return
     */
    private View getView(int position) {
        View v = null;
        int firstListItemPosition = mScannedDeviceList.getFirstVisiblePosition();
        int lastListItemPosition = firstListItemPosition + mScannedDeviceList.getChildCount() - 1;
        if (position < firstListItemPosition || position > lastListItemPosition ) {
            v = mScannedDeviceList.getAdapter().getView(position, null, mScannedDeviceList);
        } else {
            final int childIndex = position - firstListItemPosition;
            v = mScannedDeviceList.getChildAt(childIndex);
        }
        return v;
    }

    @Override
    protected void onResume() {
        mBluetoothService.setServiceCallback(this);
        clearDevices();
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mBluetoothService != null) {
            mBluetoothService.stopScan();
        }
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        if (mBluetoothService != null) {
            mBluetoothService.release();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        // 연결 버튼 클릭 시
        if(v.getId() == R.id.tvActionbarBtnRight) {
            showScanDialog();
        }
    }

    @Override
    public void onBackPressed() {
        if (mScanDialog != null) {
            dismissScanDialog();
        }
        super.onBackPressed();
    }

    /**
     * 블루투스 기능이 Off되어 있다면 On 시킨다.
     */
    private void bluetoothInitialize() {
        if (!mBluetoothService.initialize(this)) {
            dismissScanDialog();
            Intent enableBLEIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBLEIntent, REQUEST_BT_ENABLE);

        } else {
            doDeviceScanning(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 블루투스 기능 On되었다면, 장치를 검색한다.
        if (requestCode == REQUEST_BT_ENABLE) {
            if (resultCode == RESULT_OK) {
                showScanDialog();
            }
        }
    }

    /**
     * 블루투스 장치를 검색하거나 중단한다.
     * @param
     */
    public void doDeviceScanning(boolean b) {
        if (b) {
            clearDevices();
            mBluetoothService.startScan();
            mDialogScanEnable.setText("중지");
            mDialogScanningLabel.setVisibility(View.VISIBLE);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothService.stopScan();
                    mDialogScanningLabel.setVisibility(View.GONE);
                    mDialogScanEnable.setText("검색");
                }
            }, SCAN_PERIOD);

        } else {
            mBluetoothService.stopScan();
            mDialogScanningLabel.setVisibility(View.GONE);
            mDialogScanEnable.setText("검색");
        }
    }

    /*
     * 검색된 블루투스 장치에 대한 콜백 메소드
     * @param address
     */
    @Override
    public void onScanResult(String address) {
        if (mScanDialog != null) {
            mDevicesScanned.add(new ScannedDevice(address, mBluetoothService.getDeviceName(address)));
            mScannedDeviceListAdapter.notifyDataSetInvalidated();
        } else {
            mBluetoothService.stopScan();
        }
    }

    /**
     * 검색된 장치의 연결 상태에 대한 콜백 메소드
     */
    @Override
    public void onConnectionStateChange(String address, boolean isConnected) {
        for (int i = 0 ; i < mDevicesScanned.size() ; i++) {
            ScannedDevice device = mDevicesScanned.get(i);
            Log.d(TAG, "compare " + device.getAddress() + " vs " + address);
            if (device.getAddress().equals(address)) {
                if (isConnected) {
                    if (i != 0) {
                        mDevicesScanned.remove(i);
                        mDevicesScanned.add(0, device);
                    }
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mScannedDeviceListAdapter.notifyDataSetInvalidated();
                            deviceConnected(0);
                        }
                    });
                    Log.d(TAG, "connected " + device.getName());

                } else {
                    if (i != mDevicesScanned.size() -1) {
                        mDevicesScanned.remove(i);
                        mDevicesScanned.add(device);
                    }
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mScanDialog != null) {
                                mScannedDeviceListAdapter.notifyDataSetInvalidated();
                                deviceDisconnected(mDevicesScanned.size() - 1);
                            }
                        }
                    });
                    Log.d(TAG, "disconnected " + device.getName());
                }
                break;
            }
        }
    }

    /**
     * 사용자 지정 메시지를 표시한다.
     * @param address
     * @param msg
     */
    public void displayLocalMessage(String address, String msg) {
        onDataRead(address, msg.getBytes());
    }

    @Override
    public void onDataRead(String address, byte[] data) {
        Message msg = new Message();
        msg.setType(Message.MSG_IN);
        msg.setData(new String(data).trim());
        msg.setFrom(mBluetoothService.getDeviceName(address));
        messageUpdateToListView(msg);
    }

    /**
     * 수신된 메시지를 채팅 메시지 리스트 뷰에 반영한다.
     * @param msg
     */
    private void messageUpdateToListView(Message msg) {
        Log.e("msg",msg.getData().toCharArray()[0]+"");
        if(msg.getData().charAt(0) == 'X'){
            m_soundpool.play(m_sound_idc, 1, 1, 1, 0, 1);
            clicked = 0;
        }
        if(msg.getData().charAt(0) == 'R'){
            m_soundpool.play(m_sound_idd, 1, 1, 1, 0, 1);
            clicked = 1;
        }
        if(msg.getData().charAt(0) == 'M'){
            m_soundpool.play(m_sound_ide, 1, 1, 1, 0, 1);
            clicked = 2;
        }
        if(msg.getData().charAt(0) == 'F'){
            m_soundpool.play(m_sound_idf, 1, 1, 1, 0, 1);
            clicked =3;
        }
        if(msg.getData().charAt(0) == 'S'){
            m_soundpool.play(m_sound_idg, 1, 1, 1, 0, 1);
            clicked =4;
        }
        if(msg.getData().charAt(0) == 'L'){
            m_soundpool.play(m_sound_ida, 1, 1, 1, 0, 1);
            clicked =5;
        }
        if(msg.getData().charAt(0) == 'T'){
            m_soundpool.play(m_sound_idb, 1, 1, 1, 0, 1);
            clicked =6;
        }
        if(msg.getData().charAt(0) == 'Z') {
            m_soundpool.play(m_sound_idc6, 1, 1, 1, 0, 1);
            clicked =7;
        }
    }
    /*
    class Piano extends View{
        public Piano(Context context){
            super(context);
        }

        Bitmap whitekey, blackkey;
        Paint paint = new Paint();

        public void draw(Canvas canvas){
            if (whitekey == null) {
                whitekey = BitmapFactory.decodeResource(getResources(), R.drawable.whitekey);
            }
            if (blackkey == null) {
                blackkey = BitmapFactory.decodeResource(getResources(), R.drawable.blackkey);
            }
            int keys = 8;

            for(int i = 0; i<keys; i++){
                canvas.drawBitmap(whitekey, i*whitekey.getWidth(), 0, paint);
            }
            for(int i = 0; i<keys; i++){
                if(i != 3 && i != 7){
                    canvas.drawBitmap(blackkey, i*blackkey.getWidth()+blackkey.getWidth()*0.5f, 0, paint);
                }
            }
        }
    }
    */
    /*
    public class ViewThread extends Thread {
        SurfaceHolder mHolder;                  // SurfaceHolder를 저장할 변수
        private boolean shouldStop = false;
        private int clicked = -1;
        Bitmap whiteKey, blackKey, orangeKey;
        Paint paint = new Paint();

        public ViewThread(SurfaceHolder holder, int click){
            mHolder = holder;
            clicked = click;
        }

        public void stopSafely(){
            shouldStop = true;
        }
        @Override
        public void run() {
            Log.e("Test", "DDDD");
            Canvas canvas = null;
            while(!shouldStop){
                Log.e("Test", "CCCC");
                canvas = mHolder.lockCanvas();
                try {
                    Log.e("Test", "AAAA");
                    synchronized (mHolder) {
                        Log.e("Test", "BBBB");
                    }

                } finally {
                    if(canvas != null){
                        Log.e("Test", "EEEE");
                        mHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }
    */
}

