package org.louiswilliams.phcontroller;


import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class CarDataSim extends CarData {


    private Timer mTimer;
    private int value = -1000;
    /*
        Update car data every *rate* ms.
     */
    void run(int rate) {

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Map<String, String> uuids =  getUuids();
                for (String v : uuids.values()) {
                    setByUUID(v, value);
                }
                value = (value+10) % 6000;
            }
        }, 0, rate);
    }

    void stop() {
        if (mTimer != null) {
            mTimer.cancel();
        }
    }
}
