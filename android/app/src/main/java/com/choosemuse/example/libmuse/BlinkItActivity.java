package com.choosemuse.example.libmuse;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.LibmuseVersion;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseManagerAndroid;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

public class BlinkItActivity extends Activity implements OnClickListener, MotionListener, GameListener {

    /**
     * Tag used for logging purposes.
     */
    private final String TAG = "TestLibMuseAndroid";

    /**
     * The MuseManager is how you detect Muse headbands and receive notifications
     * when the list of available headbands changes.
     */
    private MuseManagerAndroid manager;

    /**
     * A Muse refers to a Muse headband.  Use this to connect/disconnect from the
     * headband, register listeners to receive EEG data and get headband
     * configuration and version information.
     */
    private Muse muse;

    /**
     * The ConnectionListener will be notified whenever there is a change in
     * the connection state of a headband, for example when the headband connects
     * or disconnects.
     * <p>
     * Note that ConnectionListener is an inner class at the bottom of this file
     * that extends MuseConnectionListener.
     */
    private ConnectionListener connectionListener;

    /**
     * The DataListener is how you will receive EEG (and other) data from the
     * headband.
     * <p>
     * Note that DataListener is an inner class at the bottom of this file
     * that extends MuseDataListener.
     */
    private DataListener dataListener;

    private View connectionStatus;
    private View startButton;
    private TextView taskView;
    private TextView scoreView;
    private TextView timerView;

    private Game game;

    private final Handler handler = new Handler();
    private TextToSpeech tts;


    //--------------------------------------
    // Lifecycle / Connection code


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We need to set the context on MuseManagerAndroid before we can do anything.
        // This must come before other LibMuse API calls as it also loads the library.
        manager = MuseManagerAndroid.getInstance();
        manager.setContext(this);

        Log.i(TAG, "LibMuse version=" + LibmuseVersion.instance().getString());

        WeakReference<BlinkItActivity> weakActivity =
                new WeakReference<BlinkItActivity>(this);
        // Register a listener to receive connection state changes.
        connectionListener = new ConnectionListener(weakActivity);
        // Register a listener to receive data from a Muse.
        dataListener = new DataListener(this);

        // Muse 2016 (MU-02) headbands use Bluetooth Low Energy technology to
        // simplify the connection process.  This requires access to the COARSE_LOCATION
        // or FINE_LOCATION permissions.  Make sure we have these permissions before
        // proceeding.
        ensurePermissions();

        // Load and initialize our UI.
        initUI();

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.UK);
                }
            }
        });
    }

    protected void onPause() {
        super.onPause();
        // It is important to call stopListening when the Activity is paused
        // to avoid a resource leak from the LibMuse library.
        manager.stopListening();
    }

    protected void onStart() {
        super.onStart();

        manager.startListening();
    }

    private void connect() {

        List<Muse> availableMuses = manager.getMuses();

        // Check that we actually have something to connect to.
        if (availableMuses.size() < 1) {
            Log.w(TAG, "There is nothing to connect to");
            connectionStatus.setBackgroundColor(getResources().getColor(R.color.red));
        } else {
            manager.stopListening();
            // Cache the Muse that the user has selected.
            muse = availableMuses.get(0);

            // Unregister all prior listeners and register our data listener to
            // receive the MuseDataPacketTypes we are interested in.  If you do
            // not register a listener for a particular data type, you will not
            // receive data packets of that type.
            muse.unregisterAllListeners();
            muse.registerConnectionListener(connectionListener);
            muse.registerDataListener(dataListener, MuseDataPacketType.EEG);
            muse.registerDataListener(dataListener, MuseDataPacketType.ACCELEROMETER);
            muse.registerDataListener(dataListener, MuseDataPacketType.ARTIFACTS);

            // Initiate a connection to the headband and stream the data asynchronously.
            muse.runAsynchronously();
        }
    }

    private void disconnect() {
        // Disconnect from the selected Muse.
        if (muse != null) {
            muse.disconnect(false);
            muse = null;
        }
        manager.startListening();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.task && (game == null || game.finished)) {
            if (game == null || game.finished) {
                game = new Game(this);
                SharedPreferences preferences = getSharedPreferences("active", Context.MODE_PRIVATE);
                game.setName(preferences.getString("name", ""));
                game.setPhone(preferences.getString("phone", ""));
                taskView.setTextColor(getResources().getColor(android.R.color.black));
            }
            game.start();
            scoreView.setText("" + game.score);
            handler.post(tickUi);
        }
    }

    private final Runnable tickUi = new Runnable() {
        @Override
        public void run() {
            if (game.started && !game.finished) {
                double millis = game.getSpeed() - (System.currentTimeMillis() - game.motionStartTime);
                if(millis < 0) {
                    timerView.setText("0.00");
                    game.setNextMotion();
                }
                timerView.setText(String.format("%.2f", millis / 1000));
            }
            handler.postDelayed(tickUi, 1000 / 60);
        }
    };

    @Override
    public void onNextMove() {
        taskView.setText(game.currentMotion.toString());
        tts.speak(game.currentMotion.toString(), TextToSpeech.QUEUE_FLUSH, null);
        taskView.setTextColor(getResources().getColor(R.color.black));
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                taskView.setTextColor(getResources().getColor(android.R.color.black));
//            }
//        }, 200);
    }

    @Override
    public boolean onMotion(Motion m, long duration) {
//        Log.d(TAG, "Motion received: " + m.toString());
        if (game.started && !game.finished) {
            Log.d("foobar", "1b");
            if (game.onMotion(m, duration))
                return true;
            if (!game.finished) {
                taskView.setTextColor(getResources().getColor(R.color.green));
                scoreView.setText("" + game.score);
                MediaPlayer mp = MediaPlayer.create(this, R.raw.correct);
                mp.setVolume(0.5F, 0.5F);
                mp.start();
            }
        }
        Log.d("foobar", "1c");
        return false;
    }

    @Override
    public void onGameOver() {
        taskView.setText("Game over!");
        taskView.setTextColor(getResources().getColor(R.color.red));
        MediaPlayer.create(this, R.raw.gameover).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                manager.stopListening();
                manager.startListening();
                break;
            case R.id.connect:
                connect();
                break;
            case R.id.disconnect:
                disconnect();
                break;
        }
        return true;
    }

    //--------------------------------------
    // Permissions

    /**
     * The ACCESS_COARSE_LOCATION permission is required to use the
     * Bluetooth Low Energy library and must be requested at runtime for Android 6.0+
     * On an Android 6.0 device, the following code will display 2 dialogs,
     * one to provide context and the second to request the permission.
     * On an Android device running an earlier version, nothing is displayed
     * as the permission is granted from the manifest.
     * <p>
     * If the permission is not granted, then Muse 2016 (MU-02) headbands will
     * not be discovered and a SecurityException will be thrown.
     */
    private void ensurePermissions() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // We don't have the ACCESS_COARSE_LOCATION permission so create the dialogs asking
            // the user to grant us the permission.

            DialogInterface.OnClickListener buttonListener =
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ActivityCompat.requestPermissions(BlinkItActivity.this,
                                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                    0);
                        }
                    };

            // This is the context dialog which explains to the user the reason we are requesting
            // this permission.  When the user presses the positive (I Understand) button, the
            // standard Android permission dialog will be displayed (as defined in the button
            // listener above).
            AlertDialog introDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_dialog_title)
                    .setMessage(R.string.permission_dialog_description)
                    .setPositiveButton(R.string.permission_dialog_understand, buttonListener)
                    .create();
            introDialog.show();
        }
    }


    //--------------------------------------
    // Listeners

    /**
     * You will receive a callback to this method each time there is a change to the
     * connection state of one of the headbands.
     *
     * @param p    A packet containing the current and prior connection states
     * @param muse The headband whose state changed.
     */
    public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
        final ConnectionState current = p.getCurrentConnectionState();

        // Format a message to show the change of connection state in the UI.
        final String status = p.getPreviousConnectionState() + " -> " + current;
        Log.i(TAG, status);

        if (current == ConnectionState.DISCONNECTED) {
            Log.i(TAG, "Muse disconnected:" + muse.getName());
            // We have disconnected from the headband, so set our cached copy to null.
            this.muse = null;

        }
        if (connectionStatus != null)
            connectionStatus.setBackgroundColor(getResources().getColor(colorFromStatus(current)));
        else
            Log.d("foobar", "no connection status view visible");
    }

    private int colorFromStatus(ConnectionState state) {
        switch (state) {
            case CONNECTED:
                return R.color.green;
            case CONNECTING:
                return R.color.yellow;
            case DISCONNECTED:
                return R.color.red;
            case UNKNOWN:
            case NEEDS_UPDATE:
                return R.color.blue;
        }
        return -1;
    }

    //--------------------------------------
    // UI Specific methods

    /**
     * Initializes the UI of the example application.
     */
    private void initUI() {
        setContentView(R.layout.activity_blinkit);

        startButton = findViewById(R.id.task);
        if (startButton != null)
            startButton.setOnClickListener(this);

        connectionStatus = findViewById(R.id.con_status);

        scoreView = (TextView) findViewById(R.id.score);
        taskView = (TextView) findViewById(R.id.task);
        timerView = (TextView) findViewById(R.id.timer);
    }

    class ConnectionListener extends MuseConnectionListener {
        final WeakReference<BlinkItActivity> activityRef;

        ConnectionListener(final WeakReference<BlinkItActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
            activityRef.get().receiveMuseConnectionPacket(p, muse);
        }
    }
}
