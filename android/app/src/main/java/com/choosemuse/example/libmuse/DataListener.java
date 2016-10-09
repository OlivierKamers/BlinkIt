package com.choosemuse.example.libmuse;

import android.util.Log;

import com.choosemuse.libmuse.Accelerometer;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;

public class DataListener extends MuseDataListener {

    private boolean isBlinking = false;
    private long startedBlinking = 0L;
    private MotionListener listener;


    private boolean isLeft = false;
    private boolean isRight = false;

    DataListener(MotionListener listener) {
        this.listener = listener;
    }

    /**
     * You will receive a callback to this method each time the headband sends a MuseDataPacket
     * that you have registered.  You can use different listeners for different packet types or
     * a single listener for all packet types as we have done here.
     * @param p     The data packet containing the data from the headband (eg. EEG data)
     * @param muse  The headband that sent the information.
     */
    @Override
    public synchronized void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
        final long n = p.valuesSize();
        switch (p.packetType()) {
//            case EEG:
//                getEegChannelValues(eegBuffer,p);
//                eegStale = true;
//                break;
            case ACCELEROMETER:
                double value = p.getAccelerometerValue(Accelerometer.LEFT_RIGHT);
                if(value > 0.3) {
                    if(!isLeft)
                        listener.onMotion(Motion.LEFT, 0L);
                    isLeft = true;
                } else {
                    isLeft = false;
                }
                if(value < -0.3) {
                    if(!isRight)
                        Log.d("foobar", "1");
                        listener.onMotion(Motion.RIGHT, 0L);
                    isRight = true;
                } else {
                    isRight = false;
                }
        }
       // Log.d("foobar", "0");
    }

    /**
     * You will receive a callback to this method each time an artifact packet is generated if you
     * have registered for the ARTIFACTS data type.  MuseArtifactPackets are generated when
     * eye blinks are detected, the jaw is clenched and when the headband is put on or removed.
     * @param p     The artifact packet with the data from the headband.
     * @param muse  The headband that sent the information.
     */
    @Override
    public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
        if(isBlinking != p.getBlink()) {
            if(isBlinking){
                //einde
                long duration = System.currentTimeMillis() - startedBlinking;
                listener.onMotion(Motion.BLINK, duration);
            } else {
                // begin
                startedBlinking = System.currentTimeMillis();
            }
            isBlinking = !isBlinking;
        }
    }
}
