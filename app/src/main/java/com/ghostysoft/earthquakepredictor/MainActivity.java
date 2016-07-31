package com.ghostysoft.earthquakepredictor;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
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
    // sensitivity  constants
    final static int LowSensitivity = 25;
    final static int NormalSensitivify = 10;
    final static int HighSensitivity = 5;
    // speed constants
    final static int SlowSpeed = SensorManager.SENSOR_DELAY_NORMAL;
    final static int NormalSpeed = SensorManager.SENSOR_DELAY_UI;
    final static int FastSpeed = SensorManager.SENSOR_DELAY_GAME;
    final static int HighSpeed = SensorManager.SENSOR_DELAY_FASTEST;
    // other  constants
    private  final static int MY_PERMISSIONS_REQUEST = 168; //a randomly assigned integer
    final String logFileName = "EarthQuakeLog.txt";

    // Sensor Manager
    SensorManager mSensorManager;
    Sensor mMagneticSensor;

    // running parameters
    int updateSpeed = SlowSpeed;
    int sensitivityValue = NormalSensitivify;
    int senseTick=0;
    int quakeCount=0;   //count for quake

    // UI elements
    TextView sensorView, reportView, messageView;
    Button buttonQuake, buttonScreenCapture;
    Menu mOptionsMenu;

    // The Scope
    SensorScope sensorScope;

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

        // buttons
        buttonQuake = (Button) findViewById(R.id.buttonQuake);
        buttonQuake.setVisibility(View.INVISIBLE);
        buttonQuake.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"Reset");
                buttonQuake.setVisibility(View.INVISIBLE);
            }
        });

        buttonScreenCapture = (Button) findViewById(R.id.buttonScreenCapture);
        buttonScreenCapture.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"Screen Shot");
                takeScreenShot();
            }
        });

        // scope
        sensorScope = new SensorScope(this,findViewById(android.R.id.content));

        // sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            mMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
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
        mOptionsMenu.findItem(R.id.speedRun).setTitle(R.string.stop);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        switch (updateSpeed) {
            case SlowSpeed:
                        menu.findItem(R.id.speedSlow).setChecked(true);
                        break;
            case NormalSpeed:
                        menu.findItem(R.id.speedNormal).setChecked(true);
                        break;
            case FastSpeed:
                        menu.findItem(R.id.speedFast).setChecked(true);
                        break;
            case HighSpeed:
                        menu.findItem(R.id.speedHigh).setChecked(true);
                        break;
        }

        switch (sensitivityValue) {
            case HighSensitivity:
                menu.findItem(R.id.senseHigh).setChecked(true);
                break;
            case NormalSensitivify:
                menu.findItem(R.id.senseNormal).setChecked(true);
                break;
            case LowSensitivity:
                menu.findItem(R.id.senseLow).setChecked(true);
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MenuItem runMenuItem = mOptionsMenu.findItem(R.id.speedRun);
        switch (item.getItemId()) {
            case R.id.speedSlow: //Slow Speed
                updateSpeed = SlowSpeed;
                mSensorManager.unregisterListener(this);
                mSensorManager.registerListener(this, mMagneticSensor, updateSpeed);
                messageView.setText("Detection is running slowly");
                runMenuItem.setTitle(R.string.stop);
                item.setChecked(true);
                return true;
            case R.id.speedNormal: //Normal Speed
                updateSpeed = NormalSpeed;
                mSensorManager.unregisterListener(this);
                mSensorManager.registerListener(this, mMagneticSensor, updateSpeed);
                messageView.setText("Detection is running normally");
                runMenuItem.setTitle(R.string.stop);
                item.setChecked(true);
                return true;
            case R.id.speedFast: //Fast Speed
                updateSpeed = FastSpeed;
                mSensorManager.unregisterListener(this);
                mSensorManager.registerListener(this, mMagneticSensor, updateSpeed);
                messageView.setText("Detection is running fast");
                runMenuItem.setTitle(R.string.stop);
                item.setChecked(true);
                return true;
            case R.id.speedHigh: //High Speed
                updateSpeed = HighSpeed;
                mSensorManager.unregisterListener(this);
                mSensorManager.registerListener(this, mMagneticSensor, updateSpeed);
                messageView.setText("Detection is running in high speed");
                runMenuItem.setTitle(R.string.stop);
                item.setChecked(true);
                return true;
            case R.id.speedRun: //Stop
                if (runMenuItem.getTitle().equals(getText(R.string.stop))) {
                    mSensorManager.unregisterListener(this);
                    runMenuItem.setTitle(R.string.run);
                    messageView.setText("Detection is stopped");
                } else if (runMenuItem.getTitle().equals(getText(R.string.run))) {
                    mSensorManager.registerListener(this, mMagneticSensor, updateSpeed);
                    messageView.setText("Detection is running");
                    runMenuItem.setTitle(R.string.stop);
                } else {
                    Log.d(TAG, "run=" + runMenuItem.getTitle());
                }
                return true;
            case R.id.senseLow: //Low Sensitivity
                sensitivityValue = LowSensitivity;
                item.setChecked(true);
                return true;
            case R.id.senseNormal: //Middle Sensitivity
                sensitivityValue = NormalSensitivify;
                item.setChecked(true);
                return true;
            case R.id.senseHigh: //High Sensitivity
                sensitivityValue = HighSensitivity;
                item.setChecked(true);
                return true;
            case R.id.screenShot: //take screen shot
                takeScreenShot();
                return true;
            case R.id.deleteLogfile: //take screen shot
                deleteLogFile();
                return true;
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
        sensorScope.addData(event.values[0], event.values[1], event.values[2], v, senseTick);

        sensorView.setText(String.format(" X = %10.6f\n Y = %10.6f\n Z = %10.6f\n V = %10.6f",
                event.values[0], event.values[1], event.values[2], v));

        reportView.setText(String.format(" max = %10.6f\n min = %10.6f\n dev = %10.6f\n tick = %d",
                SensorScope.maxSenseValue, SensorScope.minSenseValue, SensorScope.diffSenseValue, senseTick));

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
                    SensorScope.maxSenseValue, SensorScope.minSenseValue, SensorScope.diffSenseValue));
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

    //
    // writeLogFile()
    //
    private void writeLogFile(String message)
    {

        try {
            File logFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), logFileName);
            FileOutputStream outputStream = new FileOutputStream(logFile.getAbsolutePath(),true); //appen end
            outputStream.write(message.getBytes());
            outputStream.flush();
            outputStream.close();
            messageView.setText("Write Logfile OK");
        } catch (Exception e) {
            messageView.setText("Write Logfile failed");
            e.printStackTrace();
        }
    }

    private void deleteLogFile()
    {
        try {
            File logFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), logFileName);
            logFile.delete();
            messageView.setText("Delete Logfile OK");
        } catch (Exception e) {
            messageView.setText("Delete Logfile failed");
            e.printStackTrace();
        }

    }

    // ------------------------------------------------------------------------
    //  takeScreenShot()
    // ------------------------------------------------------------------------
    private void takeScreenShot() {

        if (!isExternalStorageWritable()) {
            Toast.makeText(this, "External Storage is not Writable", Toast.LENGTH_LONG).show();
            return;
        } else {
            //Toast.makeText(this,"External Storage is Writable",Toast.LENGTH_LONG).show();
        }

        Process sh;

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
        SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());
        String fileName = formatter.format(curDate);
        String strNow = formatter2.format(curDate);

        try {
            // create bitmap screen capture
            View rootView = getWindow().getDecorView().getRootView(); // get full screen
            //View rootView = getWindow().getDecorView().getRootView(); // get activity screen
            rootView.setDrawingCacheEnabled(true);
            Bitmap capturedBitmap = Bitmap.createBitmap(rootView.getDrawingCache());
            rootView.setDrawingCacheEnabled(false);

            // save to image file
            File imageFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Screenshots/" + fileName +".jpg");
            Log.d(TAG,"file: "+imageFile.getAbsolutePath());
            OutputStream outputStream = new FileOutputStream(imageFile); //FileNotFoundException
            capturedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            messageView.setText("File Not Found");
            return;
        } catch (IOException e) {
            e.printStackTrace();
            messageView.setText("I/O Exception");
            return;
        } catch(Throwable e) {
            e.printStackTrace();
            messageView.setText("Screen Snapshot Fail");
            return;
        }

        //Toast.makeText(this, "File Saved", Toast.LENGTH_SHORT).show(); //do not do this
        messageView.setText("Screen Captured\n"+strNow);
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
