package com.kinometrix.kinometrix3space;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.jjoe64.graphview.series.DataPoint;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static com.kinometrix.kinometrix3space.MainActivity.fileName;

/**
 * Created by J on 1/25/2018.
 */

public class SensorDataAsyncTask extends AsyncTask<Integer, Integer, Integer>{
    private boolean keep_going = false;
    public static long tstamp;
    public static float[] sdata;
    Context context;
    long unixTime;

    File LogFile;
    BufferedWriter out;

    private double graph2LastXValue;

    int dataLength = TSSBTSensor.TSS_QUAT_LEN +
            TSSBTSensor.TSS_NORMAL_GYRO_LEN +
            TSSBTSensor.TSS_EULERANGLES_LEN+
            TSSBTSensor.TSS_NORMAL_ACCEL;

    @SuppressLint("LongLogTag")
    public SensorDataAsyncTask(Context context, Integer mode){
        super();
        this.context = context;
        graph2LastXValue = 0;
        try {

            //TSSBTSensor.getInstance().setTareCurrentOrient();
            TSSBTSensor.getInstance().setFilterMode(mode);
            TSSBTSensor.getInstance().setupStreaming();

            keep_going = true;

            Toast.makeText(context, "Acquired Bluetooth Sensor", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String sdcard = Environment.getExternalStorageDirectory().getPath();
        File appDirectory = new File(sdcard, "Kinometrix");
        if(!appDirectory.exists()){
            boolean status = appDirectory.mkdirs();
            Log.e("LOG_TAG", "appDirectory created: " + status );
        }

        LogFile = new File(appDirectory, fileName);

        if(!LogFile.exists()){
            try {
                boolean status = LogFile.createNewFile();
                Log.e("LOG_TAG", "LogFile.createNewFile() created: " + status);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            out = new BufferedWriter(new FileWriter(LogFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Log.d("SensorDataHandler:handleMessage()", "Going to call startStreaming");
            TSSBTSensor.getInstance().startStreaming();
            Log.d("SensorDataHandler:handleMessage()", "Called startStreaming");
            Log.d("SensorDataHandler:handleMessage()","Expected length of data: "+dataLength);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Integer doInBackground(Integer... integers) {
        int num = integers[0];
        switch (num){
            case 1:
                while(keep_going){
                    try {
                        unixTime = System.currentTimeMillis();
                        float[] sensordata = TSSBTSensor.getInstance().binToFloat(TSSBTSensor.getInstance().read(dataLength));
                        tstamp = unixTime;
                        sdata = sensordata;
                        out.append(Float.toString(sensordata[4]) +  ", " + Float.toString(sensordata[5]) +  ", " + Float.toString(sensordata[6]) + ", "
                                + Float.toString(sensordata[7]) + ", " + Float.toString(sensordata[8]) + ", " + Float.toString(sensordata[9]) + ", "
                                + Float.toString(sensordata[10]) + ", " + Float.toString(sensordata[11]) + ", " + Float.toString(sensordata[12]) + "\n");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... params) {

    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
