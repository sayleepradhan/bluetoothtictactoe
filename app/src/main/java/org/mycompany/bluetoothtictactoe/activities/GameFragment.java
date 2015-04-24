package org.mycompany.bluetoothtictactoe.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.mycompany.bluetoothtictactoe.R;
import org.mycompany.bluetoothtictactoe.model.TTTBoard;
import org.mycompany.bluetoothtictactoe.logger.Log;

/**
 * Created by Saylee Pradhan (sap140530) on 4/20/2015.
 * Course: CS6301.001
 *
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class GameFragment extends Fragment {

    private static final String TAG = "GameFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    //private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BUTTON = 3;

    // Layout Views
   
    private Button startGameButton;
    private Button selectZeroButton;
    private Button selectCrossButton;
    private ImageButton imageButtons[];
    private String selfSymbol;
    private String readMessage;
    private String oppSymbol;
    private boolean turn;
    private boolean flag = false;
    TTTBoard board;
    TextView gameStatus;
    private boolean[] clicked;
    /**
     * Name of the connected device
     */
    private String connectedDeviceName = null;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer outStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter bluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothService bluetoothService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (bluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        //bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If BT is not on, request that it be enabled.
        // setupGame() will then be called during onActivityResult
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BUTTON);
            // Otherwise, setup the chat session
        } else if (bluetoothService == null) {
            setupGame();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) {
            bluetoothService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (bluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (bluetoothService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                bluetoothService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        startGameButton = (Button) view.findViewById(R.id.button_start);
        selectCrossButton = (Button) view.findViewById(R.id.select_cross_btn);
        selectZeroButton = (Button) view.findViewById(R.id.select_zero_btn);
        imageButtons = new ImageButton[9];
        imageButtons[0] = (ImageButton) view.findViewById(R.id.btn_0_0);
        imageButtons[1] = (ImageButton) view.findViewById(R.id.btn_0_1);
        imageButtons[2] = (ImageButton) view.findViewById(R.id.btn_0_2);
        imageButtons[3] = (ImageButton) view.findViewById(R.id.btn_1_0);
        imageButtons[4] = (ImageButton) view.findViewById(R.id.btn_1_1);
        imageButtons[5] = (ImageButton) view.findViewById(R.id.btn_1_2);
        imageButtons[6] = (ImageButton) view.findViewById(R.id.btn_2_0);
        imageButtons[7] = (ImageButton) view.findViewById(R.id.btn_2_1);
        imageButtons[8] = (ImageButton) view.findViewById(R.id.btn_2_2);
        selfSymbol = "";
        oppSymbol ="";
        clicked = new boolean[9];
        resetClickedButtons();
        setupBoard();

    }

    /**
     * Set up the UI and background operations for chat.
     */


    private void setupGame() {
        Log.debug(TAG, "setupGame()");

        // Initialize the start button with a listener that for click events
        gameStatus = (TextView) getView().findViewById(R.id.game_status_icon);
        startGameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                resetGame();
                enableButtons();
                flag = true;
                resetClickedButtons();
                sendData("newgame");
                Context context = getActivity().getApplicationContext();
                int id = BluetoothService.STATE_CONNECTED;
                if (bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                    Toast toast = Toast.makeText(context, "Devices not paired", Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    setupBoard();
                }
            }
        });


        // Initialize the BluetoothService to perform bluetooth connections
        bluetoothService = new BluetoothService(getActivity(), handler);

        // Initialize the buffer for outgoing messages
        outStringBuffer = new StringBuffer("");
    }

    public void setupBoard() {

            board = new TTTBoard();
//            gameStatus.setText("Hello!");
            selectCrossButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Context context = getActivity().getApplicationContext();
                    if (flag) {
                        if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
                            if (selfSymbol.equals("")) {
                                turn = true;
                                selfSymbol = "X";
                                oppSymbol = "O";
                                sendData("O");
                                selectZeroButton.setBackgroundColor(Color.LTGRAY);
                            } else {
                                Toast toast = Toast.makeText(context, "Symbol already selected", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        } else {
                            Toast toast = Toast.makeText(context, "Devices not paired", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    } else {
                        Toast.makeText(context, "Select New Game", Toast.LENGTH_LONG).show();
                    }
                }
            });
            selectZeroButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Context context = getActivity().getApplicationContext();
                    if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
                        if (selfSymbol.equals("")) {
                            turn = true;
                            selfSymbol = "O";
                            oppSymbol = "X";
                            sendData("X");
                            selectCrossButton.setBackgroundColor(Color.LTGRAY);
                        } else {
                            Toast toast = Toast.makeText(context, "Symbol already selected", Toast.LENGTH_SHORT);
                            toast.show();
                        }

                    } else {
                        Toast toast = Toast.makeText(context, "Devices not paired", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            });
        imageButtons[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!clicked[0]){
                    if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED && selfSymbol != "" && turn == true && board.getSymbol(0) == ' ') {
                        clicked[0]=true;
                        if (selfSymbol.equals("X"))
                            imageButtons[0].setImageResource(R.drawable.cross_image);
                        else
                            imageButtons[0].setImageResource(R.drawable.zero_image);
                        turn = false;
                        char moveResult = board.setMove(selfSymbol.charAt(0), 0);
                        if (moveResult != ' ') {
                            displayResult(v, moveResult);
                        } else
                            gameStatus.setText("Opponent's Turn");
                        sendData("0:move");
                    } else if (bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                        displayNotPaired();
                    }
                    else if (!turn) {
                        displayWhenNotYourTurn();
                    } else if (selfSymbol == "") {
                        selectSymbolPrompt();
                    }
                    if (board.noWinner()){
                        disableButtons();
                        sendData("disable");
                        gameStatus.setText("Nobody Won!");
                    }
                }
                else {
                    displayClicked();
                }
            }
        });

        imageButtons[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!clicked[1]){
                    if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED && selfSymbol != "" && turn == true && board.getSymbol(1) == ' ') {
                        clicked[1]=true;
                        if (selfSymbol.equals("X"))
                            imageButtons[1].setImageResource(R.drawable.cross_image);
                        else
                            imageButtons[1].setImageResource(R.drawable.zero_image);
                        turn = false;
                        char moveResult = board.setMove(selfSymbol.charAt(0), 1);
                        if (moveResult != ' ') {
                            displayResult(v, moveResult);
                        } else
                            gameStatus.setText("Opponent's Turn");
                        sendData("1:move");
                    } else if (bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                        displayNotPaired();
                    }
                    else if (!turn) {
                        displayWhenNotYourTurn();
                    } else if (selfSymbol == "") {
                        selectSymbolPrompt();
                    }
                    if (board.noWinner()){
                        disableButtons();
                        sendData("disable");
                        gameStatus.setText("Nobody Won!");
                    }
                }
                else {
                    displayClicked();
                }
            }
        });
        imageButtons[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!clicked[2]){
                    if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED && selfSymbol != "" && turn == true && board.getSymbol(2) == ' ') {
                        clicked[2]=true;
                        if (selfSymbol.equals("X"))
                            imageButtons[2].setImageResource(R.drawable.cross_image);
                        else
                            imageButtons[2].setImageResource(R.drawable.zero_image);
                        turn = false;
                        char moveResult = board.setMove(selfSymbol.charAt(0), 2);
                        if (moveResult != ' ') {
                            displayResult(v, moveResult);
                        } else
                            gameStatus.setText("Opponent's Turn");
                        sendData("2:move");
                    } else if (bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                        displayNotPaired();
                    }
                    else if (!turn) {
                        displayWhenNotYourTurn();
                    } else if (selfSymbol == "") {
                        selectSymbolPrompt();
                    }
                    if (board.noWinner()){
                        disableButtons();
                        sendData("disable");
                        gameStatus.setText("Nobody Won!");
                    }
                }
                else {
                    displayClicked();
                }
            }
        });

        imageButtons[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!clicked[3]){
                    if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED && selfSymbol != "" && turn == true && board.getSymbol(3) == ' ') {
                        clicked[3]=true;
                        if (selfSymbol.equals("X"))
                            imageButtons[3].setImageResource(R.drawable.cross_image);
                        else
                            imageButtons[3].setImageResource(R.drawable.zero_image);
                        turn = false;
                        char moveResult = board.setMove(selfSymbol.charAt(0), 3);
                        if (moveResult != ' ') {
                            displayResult(v, moveResult);
                        } else
                            gameStatus.setText("Opponent's Turn");
                        sendData("3:move");
                    } else if (bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                        displayNotPaired();
                    }
                    else if (!turn) {
                        displayWhenNotYourTurn();
                    } else if (selfSymbol == "") {
                        selectSymbolPrompt();
                    }
                    if (board.noWinner()){
                        disableButtons();
                        sendData("disable");
                        gameStatus.setText("Nobody Won!");
                    }
                }
                else {
                    displayClicked();
                }
            }
        });
        imageButtons[4].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!clicked[4]){
                    if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED && selfSymbol != "" && turn == true && board.getSymbol(4) == ' ') {
                        clicked[4]=true;
                        if (selfSymbol.equals("X"))
                            imageButtons[4].setImageResource(R.drawable.cross_image);
                        else
                            imageButtons[4].setImageResource(R.drawable.zero_image);
                        turn = false;
                        char moveResult = board.setMove(selfSymbol.charAt(0), 4);
                        if (moveResult != ' ') {
                            displayResult(v, moveResult);
                        } else
                            gameStatus.setText("Opponent's Turn");
                        sendData("4:move");
                    } else if (bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                        displayNotPaired();
                    }
                    else if (!turn) {
                        displayWhenNotYourTurn();
                    } else if (selfSymbol == "") {
                        selectSymbolPrompt();
                    }
                    if (board.noWinner()){
                        disableButtons();
                        sendData("disable");
                        gameStatus.setText("Nobody Won!");
                    }
                }
                else {
                    displayClicked();
                }
            }
        });
        imageButtons[5].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!clicked[5]){
                    if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED && selfSymbol != "" && turn == true && board.getSymbol(5) == ' ') {
                        clicked[5]=true;
                        if (selfSymbol.equals("X"))
                            imageButtons[5].setImageResource(R.drawable.cross_image);
                        else
                            imageButtons[5].setImageResource(R.drawable.zero_image);
                        turn = false;
                        char moveResult = board.setMove(selfSymbol.charAt(0), 5);
                        if (moveResult != ' ') {
                            displayResult(v, moveResult);
                        } else
                            gameStatus.setText("Opponent's Turn");
                        sendData("5:move");
                    } else if (bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                        displayNotPaired();
                    }
                    else if (!turn) {
                        displayWhenNotYourTurn();
                    } else if (selfSymbol == "") {
                        selectSymbolPrompt();
                    }
                    if (board.noWinner()){
                        disableButtons();
                        sendData("disable");
                        gameStatus.setText("Nobody Won!");
                    }
                }
                else {
                    displayClicked();
                }
            }
        });
        imageButtons[6].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!clicked[6]){
                    if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED && selfSymbol != "" && turn == true && board.getSymbol(6) == ' ') {
                        clicked[6]=true;
                        if (selfSymbol.equals("X"))
                            imageButtons[6].setImageResource(R.drawable.cross_image);
                        else
                            imageButtons[6].setImageResource(R.drawable.zero_image);
                        turn = false;
                        char moveResult = board.setMove(selfSymbol.charAt(0), 6);
                        if (moveResult != ' ') {
                            displayResult(v, moveResult);
                        } else
                            gameStatus.setText("Opponent's Turn");
                        sendData("6:move");
                    } else if (bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                        displayNotPaired();
                    }
                    else if (!turn) {
                        displayWhenNotYourTurn();
                    } else if (selfSymbol == "") {
                        selectSymbolPrompt();
                    }
                    if (board.noWinner()){
                        disableButtons();
                        sendData("disable");
                        gameStatus.setText("Nobody Won!");
                    }
                }
                else {
                    displayClicked();
                }
            }
        });
        imageButtons[7].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!clicked[7]){
                    if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED && selfSymbol != "" && turn == true && board.getSymbol(7) == ' ') {
                        clicked[7]=true;
                        if (selfSymbol.equals("X"))
                            imageButtons[7].setImageResource(R.drawable.cross_image);
                        else
                            imageButtons[7].setImageResource(R.drawable.zero_image);
                        turn = false;
                        char moveResult = board.setMove(selfSymbol.charAt(0), 7);
                        if (moveResult != ' ') {
                            displayResult(v, moveResult);
                        } else
                            gameStatus.setText("Opponent's Turn");
                        sendData("7:move");
                    } else if (bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                        displayNotPaired();
                    }
                    else if (!turn) {
                        displayWhenNotYourTurn();
                    } else if (selfSymbol == "") {
                        selectSymbolPrompt();
                    }
                    if (board.noWinner()){
                        gameStatus.setText("Nobody Won!");
                        sendData("disable");
                        disableButtons();
                    }
                }
                else {
                    displayClicked();
                }
            }
        });
        imageButtons[8].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!clicked[8]){
                    if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED && selfSymbol != "" && turn == true && board.getSymbol(8) == ' ') {
                        clicked[8]=true;
                        if (selfSymbol.equals("X"))
                            imageButtons[8].setImageResource(R.drawable.cross_image);
                        else
                            imageButtons[8].setImageResource(R.drawable.zero_image);
                        turn = false;
                        char moveResult = board.setMove(selfSymbol.charAt(0), 8);
                        if (moveResult != ' ') {
                            displayResult(v, moveResult);
                        } else
                            gameStatus.setText("Opponent's Turn");
                        sendData("8:move");
                    } else if (bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                        displayNotPaired();
                    }
                    else if (!turn) {
                        displayWhenNotYourTurn();
                    } else if (selfSymbol == "") {
                        selectSymbolPrompt();
                    }
                    if (board.noWinner()){
                        disableButtons();
                        sendData("disable");
                        gameStatus.setText("Nobody Won!");
                    }
                }
                else {
                    displayClicked();
                }
            }
        });
    }

    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (bluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */

    private void sendData(String message) {
        // Check that we're actually connected before trying anything
        if (!bluetoothService.isConnected()) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothService to write
            byte[] send = message.getBytes();
            bluetoothService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            outStringBuffer.setLength(0);
//            mOutEditText.setText(outStringBuffer);
        }
    }
    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothService
     */
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, connectedDeviceName));
                            //mConversationArrayAdapter.clear();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                     readMessage = new String(readBuf, 0, msg.arg1);
                     getMessage(readMessage);
                     readMessage = "";
                    //mConversationArrayAdapter.add(connectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    connectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + connectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void getMessage(String message){
        if (message.equals("X")){
            Context context = getActivity().getApplicationContext();
            Toast toast = Toast.makeText(context,"Symbol Assigned: X",Toast.LENGTH_SHORT);
            toast.show();
            selfSymbol = "X";
            oppSymbol ="O";
            selectZeroButton.setBackgroundColor(Color.LTGRAY);
        }
        else if (message.equals("O")){
            Context context = getActivity().getApplicationContext();
            Toast toast = Toast.makeText(context,"Symbol Assigned: O",Toast.LENGTH_SHORT);
            toast.show();
            selfSymbol = "O";
            oppSymbol ="X";
            selectCrossButton.setBackgroundColor(Color.LTGRAY);
        }
        else if (message.contains(":move")){
            int pos = Integer.parseInt(String.valueOf(message.charAt(0)));

            if (oppSymbol.equals("X")){
                markCross(pos);
                if (board.checkWinner('X')!=' ')
                    displayResult(getView(),'X');
                else{
                   gameStatus.setText("Your Turn");
                    turn = true;
                }

            }
            else{
                markZero(pos);
                if (board.checkWinner('O')!=' ')
                    displayResult(getView(),'O');
                else{
                    gameStatus.setText("Your Turn");
                    turn = true;
                }
            }

        }
        else if (message.contains("newgame")){
            displayNewGameMsg();
            resetGame();
        }
        else if (message.contains("disable")){
            disableButtons();
        }
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BUTTON:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a  session
                    setupGame();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.debug(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other divice
     *
     * @param data   An {@link Intent} with {@link org.mycompany.bluetoothtictactoe.activities.DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     */
    private void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        bluetoothService.connect(device);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_game, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }
    public void displayNotPaired(){
        Toast toast = Toast.makeText(getActivity().getApplicationContext(),"Devices not paired",Toast.LENGTH_SHORT);
        toast.show();
    }
    public void selectSymbolPrompt(){
        Toast toast = Toast.makeText(getActivity().getApplicationContext(),"Select a Symbol",Toast.LENGTH_SHORT);
        toast.show();
    }
    public void displayWhenNotYourTurn(){
        Toast toast = Toast.makeText(getActivity().getApplicationContext(),"Opponent's Turn",Toast.LENGTH_SHORT);
        toast.show();
    }
    public void displayNewGameMsg(){
        Toast toast = Toast.makeText(getActivity().getApplicationContext(),"New Game Started",Toast.LENGTH_SHORT);
        toast.show();
    }
    public void displayClicked(){
        Toast toast = Toast.makeText(getActivity().getApplicationContext(),"Cell already marked",Toast.LENGTH_SHORT);
        toast.show();
    }
    public void displayResult(View v,char moveResult){
        //gameStatus = (TextView) verbose.findViewById(R.id.game_status_icon);
        if (moveResult== selfSymbol.charAt(0)){
            gameStatus.setText("You Won!\n " +
                    "Click 'New Game' to restart");
        }
        else {
            gameStatus.setText("You Lost\n" +
                    " Click 'New Game' to restart");
        }
        disableButtons();
        setupBoard();
    }

    public void markCross(int position){
        if (board.getSymbol(position)==' '){
            board.setMove('X',position);
            imageButtons[position].setImageResource(R.drawable.cross_image);
        }
    }
    public void markZero(int position){
        if (board.getSymbol(position)==' '){
            board.setMove('O',position);
            imageButtons[position].setImageResource(R.drawable.zero_image);
        }
    }

    public void resetClickedButtons(){
        for (int i =0;i <9;i++){
            clicked[i] = false;
        }
    }
    public void resetGame(){

        Fragment frg = null;
        frg = getFragmentManager().findFragmentByTag("GameFragment");
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.detach(frg);
        ft.attach(frg);
        ft.commit();
    }
    public void disableButtons(){
        for (int i=0; i<9;i++){
            imageButtons[i].setEnabled(false);
        }
    }

    public void enableButtons(){
        for (int i=0; i<9;i++){
            imageButtons[i].setEnabled(true);
        }
    }


}
