package com.ghostysoft.earthquakepredictor;


import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import org.achartengine.ChartFactory;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by ghosty on 2016/7/31.
 */

public class SensorScope {
    private final static String TAG = MainActivity.class.getSimpleName();

    enum DisplayConst {DisplayXYZV, DisplayV};  // display constants
    final int numberOfLines = 4;

    private View rootView;
    private View chartView;
    Context context;

    // sensor data
    static XYSeries seriesX = new XYSeries("X");
    static XYSeries seriesY = new XYSeries("Y");
    static XYSeries seriesZ = new XYSeries("Z");
    static XYSeries seriesV = new XYSeries("V");
    static ArrayList<Date> seriesTime = new ArrayList<Date>();
    XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

    // extern reference
    static double minSenseValue = Float.MAX_VALUE;
    static double maxSenseValue = 0;
    static double diffSenseValue, newQuakeValue=0, snapQuakeValue=0;
    static int maxPlotLength = 200, plotLength=0, dataLength=0;

    static  DisplayConst displayStyle = DisplayConst.DisplayXYZV;

    // plot parameters
    final int posDistance = 20;    //position distance of quake calculation
    XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
    static double scaleY = 1.5; //extern
    boolean drawing=false;

    public SensorScope(Context context, View view) {
        this.context = context;
        this.rootView = view;

        dataset.addSeries(seriesX);
        dataset.addSeries(seriesY);
        dataset.addSeries(seriesZ);
        dataset.addSeries(seriesV);

        // render builder
        //int[] colors = new int[] { 0xFFFFFFFF, 0xFFFF80FF, 0xFFFFFF80, 0xFF000000 };// 折線的顏色
        int[] colors = new int[] { Color.RED, Color.GREEN, Color.BLUE, Color.BLACK };// 折線的顏色
        for (int i = 0; i < numberOfLines; i++) {
            XYSeriesRenderer r = new XYSeriesRenderer();
            r.setColor(colors[i]);
            r.setPointStyle(org.achartengine.chart.PointStyle.CIRCLE);
            r.setFillPoints(true); // fill
            renderer.addSeriesRenderer(r); //convert series data to a line
        }

        renderer.setChartTitleTextSize(48);
        renderer.setChartTitle( "Sensor Scope");

        renderer.setPanEnabled(true, true);
        renderer.setZoomEnabled(true, true);

        renderer.setAxisTitleTextSize(32);
        renderer.setLabelsTextSize(32);
        renderer.setAxesColor(Color.BLACK);
        renderer.setLabelsColor(Color.BLACK);

        renderer.setMargins(new int[] { 60, 90, 50, 90 }); //top, left, down, right
        renderer.setMarginsColor(Color.WHITE);

        renderer.setLegendTextSize(32);
        //renderer.setLegendHeight(80);
        renderer.setFitLegend(true);
        renderer.setShowLegend(true);

        //renderer.setPointSize(2);

        renderer.setXTitle("Tick");
        //renderer.setXLabels(6); // not correct in aChartEngine?
        renderer.setXLabelsColor(Color.BLACK);
        //renderer.setXLabelsAlign(Paint.Align.CENTER); //default

        renderer.setYTitle("Sensor Values");
        renderer.setYLabels(8);
        renderer.setYLabelsColor(0, Color.BLACK);
        renderer.setYLabelsAlign(Paint.Align.RIGHT);

        renderer.setGridLineWidth(2);
        renderer.setGridColor(Color.GRAY);
        renderer.setShowGridX(true);
        renderer.setShowGridY(true);
        renderer.setShowGrid(true);

        XYSeriesRenderer r = (XYSeriesRenderer) renderer.getSeriesRendererAt(0);
        r.setAnnotationsTextSize(15);

        // add chart view to layout
        chartView = ChartFactory.getLineChartView(context, dataset, renderer);
        LinearLayout chartLayout = (LinearLayout) rootView.findViewById(R.id.chartLayout);
        try{
            chartLayout.addView(chartView); //, new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public  void addData(double x, double y, double z, double v, int tick)
    {
        seriesX.add(tick, x);
        seriesY.add(tick, y);
        seriesZ.add(tick, z);
        seriesV.add(tick, v);
        seriesTime.add(new Date(System.currentTimeMillis()));
        if (!drawing) {
            drawing = true;
            plotChart();
            drawing = false;
        }
    }

    public void plotChart()
    {
        // scope  value range
        double yMin= Double.MAX_VALUE;
        double yMax= Double.MIN_VALUE;
        double xMin, xMax;
        double tempY;

        // calculate statistics
        dataLength = seriesV.getItemCount();
        plotLength = Math.min(seriesV.getItemCount(),maxPlotLength);
        if (plotLength<maxPlotLength) { // first plot
            xMax = maxPlotLength;
            xMin = 0;
        } else {
            xMax = seriesX.getMaxX();
            xMin = xMax-  maxPlotLength;
        }
        minSenseValue = seriesV.getMinY();
        maxSenseValue = seriesV.getMaxY();
        diffSenseValue = maxSenseValue - minSenseValue;
        if (plotLength > posDistance) {
            newQuakeValue = Math.abs(seriesV.getY(dataLength - 1) - seriesV.getY(dataLength -posDistance)); //5 points distance
        }
        if (plotLength > maxPlotLength/2 + posDistance) {
            snapQuakeValue = Math.abs(seriesV.getY(dataLength - maxPlotLength/2) - seriesV.getY(dataLength - maxPlotLength/2 - posDistance));
        }

        if (displayStyle==DisplayConst.DisplayXYZV) {
            if (yMin>(tempY=seriesX.getMinY())) yMin=tempY;
            if (yMin>(tempY=seriesY.getMinY())) yMin=tempY;
            if (yMin>(tempY=seriesZ.getMinY())) yMin=tempY;
            if (yMin>(tempY=seriesV.getMinY())) yMin=tempY;
            if (yMax<(tempY=seriesX.getMaxY())) yMax=tempY;
            if (yMax<(tempY=seriesY.getMaxY())) yMax=tempY;
            if (yMax<(tempY=seriesZ.getMaxY())) yMax=tempY;
            if (yMax<(tempY=seriesV.getMaxY())) yMax=tempY;
        } else if (displayStyle==DisplayConst.DisplayV) {
            if (yMin>(tempY=seriesV.getMinY())) yMin=tempY;
            if (yMax<(tempY=seriesV.getMaxY())) yMax=tempY;
        } else {
            Log.d(TAG,"bad displayStyle code ");
        }

        double yMid = (yMax+yMin)/2;
        double yAmp= Math.max((yMax-yMin)/2, MainActivity.sensitivityValue/2);
        yMax = (yMax+yMin)/2 + yAmp*scaleY;
        yMin = yMid - yAmp*scaleY;

        // set plot range
        renderer.setXAxisMin(xMin);
        renderer.setXAxisMax(xMax);
        renderer.setYAxisMin(yMin);
        renderer.setYAxisMax(yMax);
        chartView.invalidate();  //or repaint() ?
    }
}
