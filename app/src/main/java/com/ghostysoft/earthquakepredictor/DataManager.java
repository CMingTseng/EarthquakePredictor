package com.ghostysoft.earthquakepredictor;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by ghosty on 2016/8/3.
 */
public class DataManager {

    // constants
    private final static String TAG = MainActivity.class.getSimpleName();
    final int maxDataLength=1000; //keep limited memory
    final SimpleDateFormat fileFormatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
    final SimpleDateFormat logFormatter = new SimpleDateFormat("yyyyMMdd");
    final SimpleDateFormat recordFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    final SimpleDateFormat msgFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    // sensor data
    XYSeries seriesX, seriesY, seriesZ, seriesV;
    ArrayList<Date> seriesTime;
    XYMultipleSeriesDataset dataset;

    // data file
    final String strRootPath="Quake/";
    final File dataDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), strRootPath);
    File dataFile, logFile;
    int writeRecordCount;

    public DataManager()
    {
        buildDataset();
        buildDataDirectory();
        buildNewDataFile();
        buildLogFile();
    }

    private void buildDataset()
    {
        seriesX = new XYSeries("X");
        seriesY = new XYSeries("Y");
        seriesZ = new XYSeries("Z");
        seriesV = new XYSeries("V");
        seriesTime = new ArrayList<Date>();
        dataset = new XYMultipleSeriesDataset();

        dataset.addSeries(seriesX);
        dataset.addSeries(seriesY);
        dataset.addSeries(seriesZ);
        dataset.addSeries(seriesV);
    }

    private void buildDataDirectory()
    {
        if (!dataDirectory.exists()) {
            Log.d(TAG,"create data directory");
            dataDirectory.mkdirs();
        }
    }

    private void buildNewDataFile()
    {
        String dataFileName =  strRootPath + fileFormatter.format(new Date(System.currentTimeMillis()))+".txt";
        dataFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), dataFileName);
        writeRecordCount=0;
    }

    private void buildLogFile()
    {
        String logFileName =  strRootPath + logFormatter.format(new Date(System.currentTimeMillis()))+".log";
        logFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), logFileName);
    }

    public  void addData( int tick, double x, double y, double z, double v) {
        Date nowTime=new Date(System.currentTimeMillis());
        if (seriesX.getItemCount()>=maxDataLength) { //shift plot data
            seriesX.remove(0);
            seriesY.remove(0);
            seriesZ.remove(0);
            seriesV.remove(0);
            seriesTime.remove(0);
        }
        seriesX.add(tick, x);
        seriesY.add(tick, y);
        seriesZ.add(tick, z);
        seriesV.add(tick, v);
        seriesTime.add(nowTime);
        writeRecord(recordFormatter.format(nowTime), x, y, z);
    }

    public boolean writeRecord(String time, double x, double y, double z)
    {
        try {
            if (writeRecordCount==0) buildNewDataFile();
            FileOutputStream outputStream = new FileOutputStream(dataFile.getAbsolutePath(),true);
            outputStream.write(String.format("%s, %10.6f, %10.6f, %10.6f\n",
                        time , x, y, z).getBytes());
            outputStream.flush();
            outputStream.close();
            if (++writeRecordCount>9999) writeRecordCount=0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    //
    // writeLogFile()
    //
   public String writeLogFile(String message)
    {
        try {
            FileOutputStream outputStream = new FileOutputStream(logFile.getAbsolutePath(),true); //appen end
            outputStream.write(message.getBytes());
            outputStream.flush();
            outputStream.close();
            return "Write Logfile OK";
        } catch (Exception e) {
            e.printStackTrace();
            return "Write Logfile failed";
        }
    }

    public String deleteAllFiles()
    {
        if (dataDirectory.isDirectory())
        {
            String[] children = dataDirectory.list();
            for (int i = 0; i < children.length; i++)
            {
                try {
                    new File(dataDirectory, children[i]).delete();
                } catch (Exception e) {
                    e.printStackTrace();
                    return "Delete files failed";
                }
            }
        }
        return "Delete files OK";
    }

    String screenCapture(View rootView)
    {
        Date curDate = new Date(System.currentTimeMillis());
        String fileName = fileFormatter.format(curDate);
        String strNow = recordFormatter.format(curDate);

        try {
            rootView.setDrawingCacheEnabled(true);
            Bitmap capturedBitmap = Bitmap.createBitmap(rootView.getDrawingCache());
            rootView.setDrawingCacheEnabled(false);

            // save to image file
            File imageFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Screenshots/Quake" + fileName +".jpg");
            Log.d(TAG,"file: "+imageFile.getAbsolutePath());
            OutputStream outputStream = new FileOutputStream(imageFile); //FileNotFoundException
            capturedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "File Not Found";
        } catch (IOException e) {
            e.printStackTrace();
            return "I/O Exception";
        } catch(Throwable e) {
            e.printStackTrace();
            return "Screen Snapshot Fail";
        }
        return "Screen Captured";
    }
}
