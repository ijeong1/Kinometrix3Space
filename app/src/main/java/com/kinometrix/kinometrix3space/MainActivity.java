package com.kinometrix.kinometrix3space;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorRes;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends Activity {
    private boolean isRunning = false;
    private Button btnRun, btnStop, btnSensorStart, btnSensorStop, btnTareSensor, btnGoogleDrive;
    private EditText etFirstName, etLastName;
    private Spinner spinBdPart, spinExer, spinSide;

    private SensorDataAsyncTask sensorDataAsyncTask;

    //Log Data
    public static String fileName;

    //List for Graph
    public LineGraphSeries seriesX = new LineGraphSeries();
    public LineGraphSeries seriesY = new LineGraphSeries();
    public LineGraphSeries seriesZ = new LineGraphSeries();

    private GraphView mGyroGraph;

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
        mGyroGraph = findViewById(R.id.graphView);
        setUpGraphView();
    }

    private void setUpGraphView(){
        mGyroGraph.addSeries(seriesX);
        mGyroGraph.addSeries(seriesY);
        mGyroGraph.addSeries(seriesZ);
        seriesX.setDrawAsPath(true);
        seriesY.setDrawAsPath(true);
        seriesZ.setDrawAsPath(true);
        mGyroGraph.getViewport().setXAxisBoundsManual(true);
        mGyroGraph.getViewport().setMinX(0);
        mGyroGraph.getViewport().setMaxX(10000);

        setSeriesColor(android.R.color.holo_red_dark, seriesX);
        setSeriesColor(android.R.color.holo_green_dark, seriesY);
        setSeriesColor(android.R.color.holo_blue_dark, seriesZ);
    }

    private void setSeriesColor(@ColorRes int colorRes, LineGraphSeries series) {
        int color = getResources().getColor(colorRes);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(color);
        series.setCustomPaint(paint);
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
                if(etFirstName.getText().toString().matches("")) Toast.makeText(getApplicationContext(), "Enter First Name", Toast.LENGTH_SHORT).show();
                if(etLastName.getText().toString().matches("")) Toast.makeText(getApplicationContext(), "Enter Last Name", Toast.LENGTH_SHORT).show();
                if(isRunning == true) Toast.makeText(getApplicationContext(), "Exam is running now", Toast.LENGTH_SHORT).show();
                if(!isRunning && (!etFirstName.getText().toString().matches("")) && (!etLastName.getText().toString().matches("")))
                {
                    isRunning = true;
                    sensorDataAsyncTask.execute(1);
                } else{
                    Toast.makeText(getApplicationContext(), "The process is running.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnStop = findViewById(R.id.btnExamStop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRunning) {
                    sensorDataAsyncTask.cancel(true);
                    Toast.makeText(getApplicationContext(), "Exam is finished", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSensorStart = findViewById(R.id.btnBtConnect);
        btnSensorStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(etFirstName.getText().toString().matches("")) Toast.makeText(getApplicationContext(), "Enter First Name", Toast.LENGTH_SHORT).show();
                if(etLastName.getText().toString().matches("")) Toast.makeText(getApplicationContext(), "Enter Last Name", Toast.LENGTH_SHORT).show();
                if(!isRunning && (!etFirstName.getText().toString().matches("") && (!etLastName.getText().toString().matches("")))) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy_HHmmss");
                    String currentDateandTime = sdf.format(new Date());
                    fileName = etFirstName.getText() + "_" + etLastName.getText() + "_" + currentDateandTime + "_" + spinBdPart.getSelectedItem().toString() + "_"
                            + spinExer.getSelectedItem().toString() + "_" + spinSide.getSelectedItem().toString() + ".txt";

                    sensorDataAsyncTask = new SensorDataAsyncTask(getApplicationContext(), 1);
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
                    Message tare_message = new Message();
                    tare_message.what = 3;
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
                sensorDataAsyncTask.cancel(true);
                startActivity(new Intent(getApplicationContext(), SendLogsToGoogleDriveActivity.class));
            }
        });

    }


    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
        if(isRunning) sensorDataAsyncTask.cancel(true);
    }

}
