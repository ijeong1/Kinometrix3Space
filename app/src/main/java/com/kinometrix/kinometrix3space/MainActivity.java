package com.kinometrix.kinometrix3space;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.LimitLine.LimitLabelPosition;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.Utils;

public class MainActivity extends Activity {
    private boolean mLoop = true;
    private boolean isRunning = false;
    private Button btnRun, btnStop, btnSensorStart, btnSensorStop, btnTareSensor, btnGoogleDrive;
    private EditText etFirstName, etLastName;
    private Spinner spinBdPart, spinExer, spinSide;
    private Thread thread;
    private boolean threadlock = false;
    public static ArrayList<byte[]> streamdata = new ArrayList<>();

    // constant
    public static final long NOTIFY_INTERVAL = 1 * 100; // 0.1 seconds

    // run on another Thread to avoid crash
    private Handler mHandler = new Handler();
    // timer handling
    private Timer mTimer = null;

    //Log Data
    File LogFile;
    BufferedWriter out;

    //MPAndroidPlotChart
    private LineChart mChart;
    protected Typeface mTfLight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove Title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Screen Orientation Landscape
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        initializeViews();
    }

    //Use it for UI Initialization
    public void initializeViews() {
        etFirstName = findViewById(R.id.etPatientFirstName);
        etLastName = findViewById(R.id.etPatientLastName);
        spinBdPart = findViewById(R.id.spinnerBodyPart);
        spinExer = findViewById(R.id.spinnerExerciseType);
        spinSide = findViewById(R.id.spinnerSide);

        btnRun = findViewById(R.id.btnExamStart);
        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(threadlock != true && etFirstName.getText() != null && etLastName.getText() != null)
                {
                    mLoop = true;
                    thread = new Thread(null, doBackgroundThreadProcessing, "Background");
                    threadlock = true;
                    String sdcard = Environment.getExternalStorageDirectory().getPath();
                    File appDirectory = new File(sdcard, "Kinometrix");
                    if(!appDirectory.exists()){
                        boolean status = appDirectory.mkdirs();
                        Log.e("LOG_TAG", "appDirectory created: " + status );
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy_HHmmss");
                    String currentDateandTime = sdf.format(new Date());

                    String fileName = etFirstName.getText() + "_" + etLastName.getText() + "_" + currentDateandTime + "_" + spinBdPart.getSelectedItem().toString() + "_"
                            + spinExer.getSelectedItem().toString() + "_" + spinSide.getSelectedItem().toString() + ".txt";

                    //String fileName = etFirstName.getText() + "_" + etLastName.getText() + "_" + currentDateandTime + ".txt";
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
                    isRunning = true;
                    thread.start();
                }
                else{
                    Toast.makeText(getApplicationContext(), "The process is running.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnStop = findViewById(R.id.btnExamStop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRunning) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mLoop = false;
                    thread.interrupt();
                    threadlock = false;
                    isRunning = false;
                }
            }
        });

        btnSensorStart = findViewById(R.id.btnBtConnect);
        btnSensorStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    //TSSBTSensor.getInstance().setTareCurrentOrient();
                    TSSBTSensor.getInstance().setFilterMode(1); //Filter Mode
                    TSSBTSensor.getInstance().setupStreaming();
                    Toast.makeText(getApplicationContext(), "Acquired Bluetooth Sensor", Toast.LENGTH_SHORT).show();
                    TSSBTSensor.getInstance().startStreaming();

                    if(mTimer != null) {
                        mTimer.cancel();
                    } else {
                        // recreate new
                        mTimer = new Timer();
                    }
                    mTimer.scheduleAtFixedRate(new TimeDisplayTimerTask(), 0, NOTIFY_INTERVAL);

                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        btnSensorStop = findViewById(R.id.btnBtDisconnect);
        btnSensorStop.setVisibility(View.GONE);
        btnSensorStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //
            }
        });

        btnTareSensor = findViewById(R.id.btnTareSensor);
        btnTareSensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    TSSBTSensor.getInstance().stopStreaming();
                    TSSBTSensor.getInstance().setTareCurrentOrient();
                    TSSBTSensor.getInstance().startStreaming();
                    Toast.makeText(getApplicationContext(), "Tared Current Orientation", Toast.LENGTH_SHORT).show();
                }
                catch(Exception e){
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        btnGoogleDrive = findViewById(R.id.btnGoogleDrive);
        btnGoogleDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SendLogsToGoogleDriveActivity.class));
            }
        });
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }

    // GUI Handler Processing
    private Handler handler = new Handler();
    // This method is called from the UI thread


    //Background process Runnable
    private Runnable doBackgroundThreadProcessing = new Runnable(){
        @Override
        public void run() {
            backgroundThreadProcessing();
        }
    };

    int dataLength = TSSBTSensor.TSS_QUAT_LEN +
            TSSBTSensor.TSS_NORMAL_GYRO_LEN +
            TSSBTSensor.TSS_EULERANGLES_LEN+
            TSSBTSensor.TSS_NORMAL_ACCEL;

    // Background Method Implementation
    @SuppressLint("LongLogTag")
    private void backgroundThreadProcessing(){
        int i = 0;
        while (mLoop && !thread.isInterrupted())
        {
            try{
                //ORDER OF STREAMING DATA
                //TSS_GET_TARED_ORIENTATION_AS_QUATERNION, TSS_GET_NORMAL_GYRO, TSS_GET_EULERANGLES, TSS_GET_NORMAL_ACCEL
                //get path from sdcard
                ByteBuffer bb = null;

                /*
                //ChartData
                LineData data = mChart.getData();

                ILineDataSet set1 = data.getDataSetByIndex(0);
                // set.addEntry(...); // can be called as well
                ILineDataSet set2 = data.getDataSetByIndex(1);
                ILineDataSet set3 = data.getDataSetByIndex(2);
                if (set1 == null) {
                    set1 = createSet(0);
                    data.addDataSet(set1);
                }

                if (set2 == null) {
                    set2 = createSet(1);
                    data.addDataSet(set2);
                }

                if (set3 == null) {
                    set3 = createSet(2);
                    data.addDataSet(set3);
                }*/

                bb = ByteBuffer.wrap(streamdata.get(i));
                streamdata.remove(i);
                byte[] quat_bytes, gyro_bytes, euler_bytes, accel_bytes;

                bb.order(ByteOrder.nativeOrder());
                quat_bytes = new byte[TSSBTSensor.TSS_QUAT_LEN];
                bb.get(quat_bytes, 0, TSSBTSensor.TSS_QUAT_LEN);

                gyro_bytes = new byte[TSSBTSensor.TSS_NORMAL_GYRO_LEN];
                bb.get(gyro_bytes, 0, TSSBTSensor.TSS_NORMAL_GYRO_LEN);

                euler_bytes = new byte[TSSBTSensor.TSS_EULERANGLES_LEN];
                bb.get(euler_bytes, 0, TSSBTSensor.TSS_EULERANGLES_LEN);

                accel_bytes = new byte[TSSBTSensor.TSS_NORMAL_ACCEL];
                bb.get(accel_bytes, 0, TSSBTSensor.TSS_NORMAL_ACCEL);

                //quat = binToFloat(quat_bytes);
                long unixTime = System.currentTimeMillis();
                float [] gyrodata = binToFloat(gyro_bytes);
                /*
                data.addEntry(new Entry(set1.getEntryCount(), gyrodata[0]), 0);
                data.addEntry(new Entry(set2.getEntryCount(), gyrodata[1]), 1);
                data.addEntry(new Entry(set3.getEntryCount(), gyrodata[2]), 2);
                data.notifyDataChanged();
                mChart.notifyDataSetChanged();
                mChart.setVisibleXRangeMaximum(30);
                mChart.moveViewToX(data.getEntryCount());
                */
                float [] eulerdata = binToFloat(euler_bytes);
                float [] acceldata = binToFloat(accel_bytes);

                Log.d("backgroundThreadProcessing()", "Sensor Data wrote" );
                out.append( unixTime + ", " + Float.toString(gyrodata[0]) +  ", " + Float.toString(gyrodata[1]) +  ", " + Float.toString(gyrodata[2]) + ", "
                        + Float.toString(eulerdata[0]) + ", " + Float.toString(eulerdata[1]) + ", " + Float.toString(eulerdata[2]) + ", "
                        + Float.toString(acceldata[0]) + ", " + Float.toString(acceldata[1]) + ", " + Float.toString(acceldata[2]));
            }catch(Exception e){

            }
        }
    }

    // Call GUI update method Runnable.
    private Runnable doUpdateGUI = new Runnable(){
        @Override
        public void run() {
            updateGUI();
        }
    };

    private void updateGUI() {
        Log.d("Thread",  "Thread is done");
    }

    public float[] binToFloat(byte[] b)
    {
        if (b.length % 4 != 0)
        {
            return new float[0];
        }
        float[] return_array = new float[b.length / 4];
        for (int i = 0; i < b.length; i += 4)
        {
            //We account for endieness here
            int asInt = (b[i + 3] & 0xFF)
                    | ((b[i + 2] & 0xFF) << 8)
                    | ((b[i + 1] & 0xFF) << 16)
                    | ((b[i] & 0xFF) << 24);

            return_array[i / 4] = Float.intBitsToFloat(asInt);
        }
        return return_array;
    }

    class TimeDisplayTimerTask extends TimerTask {

        @Override
        public void run() {
            // run on another thread
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    try {
                        streamdata.add(TSSBTSensor.getInstance().read(dataLength));
                        Log.d("StreamData", "data added");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            });
        }
    }
}
