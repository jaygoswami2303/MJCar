package com.ambush.mjcar;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;


import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainActivity extends AppCompatActivity {

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;


    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private Button mConnectButton;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    public int counter = 0;

    private Button bt_left;
    private Button bt_right;
    private int count = 0;

    private TextView mTextViewAngleLeft;
    private TextView mTextViewStrengthLeft;
    private JoystickView mJoystickViewLeft;

    private TextView mTextViewAngleRight;
    private TextView mTextViewStrengthRight;
    private JoystickView mJoystickViewRight;

    private ProgressBar progressBarLeftUp;
    private ProgressBar progressBarLeftDown;

    private ProgressBar progressBarRightUp;
    private ProgressBar progressBarRightDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //getSupportActionBar().hide();

        bt_left = (Button) findViewById(R.id.bt_left);
        bt_left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    move('L',270,100);
                    move('R',90,100);
                }
                if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    move('L',270,0);
                    move('R',90,0);
                }
                return true;
            }
        });
        bt_right = (Button) findViewById(R.id.bt_right);
        bt_right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    move('L',90,100);
                    move('R',270,100);
                }
                if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    move('L',90,0);
                    move('R',270,0);
                }
                return true;
            }
        });

        progressBarLeftUp = (ProgressBar) findViewById(R.id.progressBar_leftUp);
        progressBarLeftDown = (ProgressBar) findViewById(R.id.progressBar_leftDown);
        progressBarLeftDown.setRotation(180);

        progressBarRightUp = (ProgressBar) findViewById(R.id.progressBar_rightUp);
        progressBarRightDown = (ProgressBar) findViewById(R.id.progressBar_rightDown);
        progressBarRightDown.setRotation(180);

        mTextViewAngleLeft = (TextView) findViewById(R.id.textView_angle_left);
        mTextViewStrengthLeft = (TextView) findViewById(R.id.textView_strength_left);

        mJoystickViewLeft = (JoystickView) findViewById(R.id.joystickView_left);
        //mJoystickViewLeft.setEnabled(false);
        mJoystickViewLeft.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                move('L', angle, strength);
            }
        });

        mTextViewAngleRight = (TextView) findViewById(R.id.textView_angle_right);
        mTextViewStrengthRight = (TextView) findViewById(R.id.textView_strength_right);

        mJoystickViewRight = (JoystickView) findViewById(R.id.joystickView_right);
        //mJoystickViewRight.setEnabled(false);
        mJoystickViewRight.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                move('R',angle,strength);
            }
        });

        mConnectButton = (Button) findViewById(R.id.bt_connect);
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            }
        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

    }

    private void move(char direction, int angle, int strength) {
        if(direction=='R') {
            mTextViewAngleRight.setText(angle + "°");
            mTextViewStrengthRight.setText(strength + "%");
            if (angle < 180) {
                progressBarRightUp.setProgress(strength);
                progressBarRightDown.setProgress(0);
                sendMessage("<RF" + strength + ">");
            } else {
                progressBarRightDown.setProgress(strength);
                progressBarRightUp.setProgress(0);
                sendMessage("<RR" + strength + ">");
            }
        }
        else if(direction=='L') {
            mTextViewAngleLeft.setText(angle + "°");
            mTextViewStrengthLeft.setText(strength + "%");
            if(angle<180) {
                progressBarLeftUp.setProgress(strength);
                progressBarLeftDown.setProgress(0);
                sendMessage("<LF"+strength+">");
            }
            else {
                progressBarLeftDown.setProgress(strength);
                progressBarLeftUp.setProgress(0);
                sendMessage("<LR"+strength+">");
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                mChatService.start();
            }
        }
    }

    private void setupChat() {

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();

    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }


    private void sendMessage(String message) {

        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            //Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);
            // Reset out string buffer to zero and clear the edit text field
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    // If the action is a key-up event on the return key, send the message
                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                        String message = view.getText().toString();
                        sendMessage(message);
                    }
                    return true;
                }
            };

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    /*String writeMessage = new String(writeBuf);
                    mAdapter.notifyDataSetChanged();
                    linearLayoutManager.scrollToPosition(mAdapter.getItemCount());
                    messageList.add(new androidRecyclerView.Message(counter++, writeMessage, "Me"));*/
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    /*mAdapter.notifyDataSetChanged();
                    linearLayoutManager.scrollToPosition(mAdapter.getItemCount());
                    Log.e("Message read","*****************"+readMessage);
                    messageList.add(new androidRecyclerView.Message(counter++, readMessage, mConnectedDeviceName));*/
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    mJoystickViewLeft.setEnabled(true);
                    mJoystickViewRight.setEnabled(true);
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mChatService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    public void discoverable(View v) {
        ensureDiscoverable();
    }
}