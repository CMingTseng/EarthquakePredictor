package com.ghostysoft.earthquakepredictor;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener { //}, SurfaceHolder.Callback {

    // degbug constants
    private final static String TAG = MainActivity.class.getSimpleName();

    // speed constants
    final static int SlowSpeed = SensorManager.SENSOR_DELAY_NORMAL;
    final static int NormalSpeed = SensorManager.SENSOR_DELAY_UI;
    final static int FastSpeed = SensorManager.SENSOR_DELAY_GAME;
    final static int HighSpeed = SensorManager.SENSOR_DELAY_FASTEST;

    // other  constants
    private  final int MY_PERMISSIONS_REQUEST = 168; //a randomly assigned integer

    // Sensor Manager
    SensorManager mSensorManager;
    Sensor mMagneticSensor;

    // running parameters
    public static int updateSpeed = SlowSpeed;           //extern
    public static int sensitivityValue = 10;                //extern
    int senseTick=0;
    int quakeCount=0;   //count for quake

    // UI elements
    TextView sensorView, reportView, messageView;
    private ImageView compassView;
    Button buttonQuake, buttonScreenCapture;
    Menu mOptionsMenu;

    // The Scope
    SensorScope sensorScope;
    int sensorType = Sensor.TYPE_MAGNETIC_FIELD;
    private float currentDegree = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // main view
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Keep the screen always on

        //subviews
        sensorView = (TextView) findViewById(R.id.sensorView);
        reportView = (TextView) findViewById(R.id.reportView);
        messageView = (TextView) findViewById(R.id.messageView);
        compassView = (ImageView) findViewById(R.id.compassView);

        // buttons
        buttonQuake = (Button) findViewById(R.id.buttonQuake);
        buttonQuake.setVisibility(View.INVISIBLE);
        buttonQuake.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonQuake.setVisibility(View.INVISIBLE);
            }
        });

        buttonScreenCapture = (Button) findViewById(R.id.buttonScreenCapture);
        buttonScreenCapture.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeScreenShot();
            }
        });

        // scope
        sensorScope = new SensorScope(this,findViewById(android.R.id.content));

        // sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(sensorType) != null) {
            mMagneticSensor = mSensorManager.getDefaultSensor(sensorType);
            mSensorManager.registerListener(this, mMagneticSensor, updateSpeed);
        } else {
            // fai! we dont have an magnetic sensor!
            showNoSensorDialog();
            finish();
        }

        // request external storage access premission on Android 6.0 (SDKv version>23)
        myRequestPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mMagneticSensor, updateSpeed);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        mOptionsMenu = menu;
        mOptionsMenu.findItem(R.id.menuSpeedRun).setTitle(R.string.stop);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        switch (updateSpeed) {
            case SlowSpeed:
                        menu.findItem(R.id.menuSpeedSlow).setChecked(true);
                        break;
            case NormalSpeed:
                        menu.findItem(R.id.menuSpeedNormal).setChecked(true);
                        break;
            case FastSpeed:
                        menu.findItem(R.id.menuSpeedFast).setChecked(true);
                        break;
            case HighSpeed:
                        menu.findItem(R.id.menuSpeedHigh).setChecked(true);
                        break;
        }

        switch (sensitivityValue) {
            case 5:
                menu.findItem(R.id.menuSense5).setChecked(true);
                break;
            case 10:
                menu.findItem(R.id.menuSense10).setChecked(true);
                break;
            case 25:
                menu.findItem(R.id.menuSense25).setChecked(true);
                break;
        }

        switch (sensorScope.displayStyle) {
            case DisplayXYZV:
                menu.findItem(R.id.menuDisplayXYZV).setChecked(true);
                break;
            case DisplayV:
                menu.findItem(R.id.menuDisplayV).setChecked(true);
                break;
            case DisplayX:
                menu.findItem(R.id.menuDisplayX).setChecked(true);
                break;
            case DisplayY:
                menu.findItem(R.id.menuDisplayY).setChecked(true);
                break;
            case DisplayZ:
                menu.findItem(R.id.menuDisplayZ).setChecked(true);
                break;
        }

        switch ((int)(SensorScope.scaleY*100)) {
            case 100:
                menu.findItem(R.id.menuDisplayY100).setChecked(true);
                break;
            case 125:
                menu.findItem(R.id.menuDisplayY125).setChecked(true);
                break;
            case 150:
                menu.findItem(R.id.menuDisplayY150).setChecked(true);
                break;
            case 175:
                menu.findItem(R.id.menuDisplayY175).setChecked(true);
                break;
            case 200:
                menu.findItem(R.id.menuDisplayY200).setChecked(true);
                break;
        }

        switch (sensorScope.maxPlotLength) {
            case 100:
                menu.findItem(R.id.menuDisplayX100).setChecked(true);
                break;
            case 200:
                menu.findItem(R.id.menuDisplayX200).setChecked(true);
                break;
            case 300:
                menu.findItem(R.id.menuDisplayX300).setChecked(true);
                break;
            case 400:
                menu.findItem(R.id.menuDisplayX400).setChecked(true);
                break;
            case 500:
                menu.findItem(R.id.menuDisplayX500).setChecked(true);
                break;
            case 1000:
                menu.findItem(R.id.menuDisplayX1000).setChecked(true);
                break;
            case -1:
                menu.findItem(R.id.menuDisplayXall).setChecked(true);
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MenuItem runMenuItem = mOptionsMenu.findItem(R.id.menuSpeedRun);
        switch (item.getItemId()) {

            //Speed Group
            case R.id.menuSpeedSlow: //Slow Speed
                updateSpeed = SlowSpeed;
                mSensorManager.unregisterListener(this);
                mSensorManager.registerListener(this, mMagneticSensor, updateSpeed);
                showMessage("Detection is running slowly");
                runMenuItem.setTitle(R.string.stop);
                return true;
            case R.id.menuSpeedNormal: //Normal Speed
                updateSpeed = NormalSpeed;
                mSensorManager.unregisterListener(this);
                mSensorManager.registerListener(this, mMagneticSensor, updateSpeed);
                showMessage("Detection is running normally");
                runMenuItem.setTitle(R.string.stop);
                return true;
            case R.id.menuSpeedFast: //Fast Speed
                updateSpeed = FastSpeed;
                mSensorManager.unregisterListener(this);
                mSensorManager.registerListener(this, mMagneticSensor, updateSpeed);
                showMessage("Detection is running fast");
                runMenuItem.setTitle(R.string.stop);
                return true;
            case R.id.menuSpeedHigh: //High Speed
                updateSpeed = HighSpeed;
                mSensorManager.unregisterListener(this);
                mSensorManager.registerListener(this, mMagneticSensor, updateSpeed);
                showMessage("Detection is running in high speed");
                runMenuItem.setTitle(R.string.stop);
                return true;
            case R.id.menuSpeedRun: //Stop
                if (runMenuItem.getTitle().equals(getText(R.string.stop))) {
                    mSensorManager.unregisterListener(this);
                    runMenuItem.setTitle(R.string.run);
                    showMessage("Detection is stopped");
                } else if (runMenuItem.getTitle().equals(getText(R.string.run))) {
                    mSensorManager.registerListener(this, mMagneticSensor, updateSpeed);
                    showMessage("Detection is running");
                    runMenuItem.setTitle(R.string.stop);
                } else {
                    Log.d(TAG, "bad run=" + runMenuItem.getTitle());
                }
                return true;

            //Sensitivyty Group
            case R.id.menuSense5: //Low Sensitivity
                sensitivityValue = 5;
                return true;
            case R.id.menuSense10: //Middle Sensitivity
                sensitivityValue = 10;
                return true;
            case R.id.menuSense25: //High Sensitivity
                sensitivityValue = 25;
                return true;

            // Display Group
            case R.id.menuDisplayXYZV:
                sensorScope.displayStyle = SensorScope.DisplayConst.DisplayXYZV;
                break;
            case R.id.menuDisplayV:
                sensorScope.displayStyle = SensorScope.DisplayConst.DisplayV;
                break;

            case R.id.menuDisplayX:
                sensorScope.displayStyle = SensorScope.DisplayConst.DisplayX;
                break;

            case R.id.menuDisplayY:
                sensorScope.displayStyle = SensorScope.DisplayConst.DisplayY;
                break;

            case R.id.menuDisplayZ:
                sensorScope.displayStyle = SensorScope.DisplayConst.DisplayZ;
                break;

            // XScale group
            case R.id.menuDisplayX100:
                sensorScope.maxPlotLength = 100;
                break;
            case R.id.menuDisplayX200:
                sensorScope.maxPlotLength = 200;
                break;
            case R.id.menuDisplayX300:
                sensorScope.maxPlotLength = 300;
                break;
            case R.id.menuDisplayX400:
                sensorScope.maxPlotLength = 400;
                break;
            case R.id.menuDisplayX500:
                sensorScope.maxPlotLength = 500;
                break;
            case R.id.menuDisplayX1000:
                sensorScope.maxPlotLength = 1000;
                break;
            case R.id.menuDisplayXall:
                sensorScope.maxPlotLength = -1;
                break;

            //YScale group
            case R.id.menuDisplayY100:
                SensorScope.scaleY = 1.00;
                break;
            case R.id.menuDisplayY125:
                SensorScope.scaleY = 1.25;
                break;
            case R.id.menuDisplayY150:
                SensorScope.scaleY = 1.50;
                break;
            case R.id.menuDisplayY175:
                SensorScope.scaleY = 1.75;
                break;
            case R.id.menuDisplayY200:
                SensorScope.scaleY = 2.00;
                break;

            //Tools
            case R.id.menuScreenShot: //take screen shot
                takeScreenShot();
                break;
            case R.id.menuDeleteAllFiles:
                deleteAllFiles();
                break;
            case R.id.menuHelp:
                showHelp();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    // ------------------------------------------------------------------------
    //  Sensor events
    // ------------------------------------------------------------------------
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        double v = (float) Math.sqrt(
                event.values[0] * event.values[0] +
                        event.values[1] * event.values[1] +
                        event.values[2] * event.values[2]);
        sensorScope.addData(senseTick, event.values[0], event.values[1], event.values[2], v);

        sensorView.setText(String.format(" X = %8.4f\n Y = %8.4f\n Z = %8.4f\n V = %8.4f",
                event.values[0], event.values[1], event.values[2], v));

        reportView.setText(String.format(" time tick = %d\n record # = %d\n quake = %8.4f\n sense = %d",
                senseTick,
                sensorScope.dataManager.writeRecordCount,
                sensorScope.diffSenseValue,
                sensitivityValue));
        // create a rotation animation (reverse turn degree degrees)
        float degree = (float)(Math.atan2((double)event.values[1],(double)event.values[0])/Math.PI*180);
        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);
        ra.setDuration(100); // how long the animation will take place
        ra.setFillAfter(true); // set the animation after the end of the reservation status
        compassView.startAnimation(ra);	// Start the animation
        currentDegree = -degree;

        if (sensorScope.newQuakeValue > sensitivityValue ) {
        //if ((SensorScope.maxSenseValue-SensorScope.minSenseValue) > sensitivityValue ) {
            if (buttonQuake.getVisibility() == View.INVISIBLE) { //only mark 1st time
                buttonQuake.setVisibility(View.VISIBLE);
            }

            quakeCount++; // count up

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis()); // 獲取當前時間
            String timeStr = formatter.format(curDate);
            buttonQuake.setText("Earthquake " + quakeCount + "\n" + timeStr);

            writeLogFile(String.format("%s : v=(%f, %f, %f), diff=%f, max=%f, min=%f, dev=%f\n",
                    timeStr,
                    event.values[0], event.values[1], event.values[2], v,
                    sensorScope.maxSenseValue, sensorScope.minSenseValue, sensorScope.diffSenseValue));
        }

        if (sensorScope.snapQuakeValue > sensitivityValue ) {
            takeScreenShot();
        }

        senseTick++;
    }

    private void showNoSensorDialog() {
        AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(this);
        MyAlertDialog.setTitle("Error");
        MyAlertDialog.setMessage("No Magnetic Sensor in this device");
        DialogInterface.OnClickListener OkClick = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //nothing to do
                //finish();
            }
        };
        MyAlertDialog.setNeutralButton("OK", OkClick);
        MyAlertDialog.show();
    }
    
    private void showMessage(String message) {
        messageView.setText(message+"\n"+
                sensorScope.dataManager.msgFormatter.format(new Date(System.currentTimeMillis())));
    }

    private void showHelp()
    {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Help")
                .setMessage("The captured data is stored in\n/Documents/Quake/")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //nothing
                    }
                })
                .show();
    }
    //
    // writeLogFile()
    //
    private void writeLogFile(String message)
    {
        showMessage(sensorScope.dataManager.writeLogFile(message));
    }

    private void deleteAllFiles()
    {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Warning")
                .setMessage("Do you want to delete all files ?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showMessage(sensorScope.dataManager.deleteAllFiles());
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //nothing
                    }
                })
                .show();
    }

    // ------------------------------------------------------------------------
    //  takeScreenShot()
    // ------------------------------------------------------------------------
    private void takeScreenShot() {

        if (!isExternalStorageWritable()) {
            showMessage("External Storage is not Writable");
            return;
        } else {
            //Toast.makeText(this,"External Storage is Writable",Toast.LENGTH_LONG).show();
        }

        String strResult=sensorScope.dataManager.screenCapture(getWindow().getDecorView().getRootView());
        showMessage(strResult);
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------------
    //  used-permissions must be requested when SDKv version>23
    // ------------------------------------------------------------------------
    public void myRequestPermission()
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // Get the Permission from Android OS
                // MY_PERMISSIONS_REQUEST is an constant
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                   && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                   // permission was granted, yay! Do the task you need to do.
                } else {
                    // permission denied, boo! Disable the functionality that depends on this permission.
                    finish();
                }
                return;
            }
            // other cases
        }
    }
}
