package com.ghostysoft.earthquakepredictor;


import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import org.achartengine.ChartFactory;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

/**
 * Created by ghosty on 2016/7/31.
 */

public class SensorScope {
    private final static String TAG = MainActivity.class.getSimpleName();

    enum DisplayConst {DisplayXYZV, DisplayV, DisplayX, DisplayY, DisplayZ };  // display constants
    final int numberOfLines = 4;

    private View rootView;
    private View chartView;
    Context context;

    //
    DataManager dataManager;

    // extern reference
    public double minSenseValue = Float.MAX_VALUE;
    public double maxSenseValue = 0;
    public double diffSenseValue, newQuakeValue=0, snapQuakeValue=0;
    public int maxPlotLength = 200, plotLength=0, dataLength=0;
    public DisplayConst displayStyle = DisplayConst.DisplayXYZV;

    // plot parameters
    final int posDistance = 20;    //position distance of quake calculation
    XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
    static double scaleY = 1.5; //extern
    boolean drawing=false;

    public SensorScope(Context context, View view) {
        this.context = context;
        this.rootView = view;

        dataManager = new DataManager();

        buildRender();
        buildChartView();
    }

    private void buildRender() // render builder
    {
        int[] colors = new int[] { Color.RED, Color.GREEN, Color.BLUE, Color.BLACK };// line colors
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
    }

    private void buildChartView()
    {
        // add chart view to layout
        chartView = ChartFactory.getLineChartView(context, dataManager.dataset, renderer);
        LinearLayout chartLayout = (LinearLayout) rootView.findViewById(R.id.chartLayout);
        try{
            chartLayout.addView(chartView);
            //chartLayout.addView(chartView,, new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public  void addData(int tick, double x, double y, double z, double v)
    {
        dataManager.addData(tick, x, y, z, v);
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
        dataLength = dataManager.seriesV.getItemCount();
        minSenseValue = dataManager.seriesV.getMinY();
        maxSenseValue = dataManager.seriesV.getMaxY();
        diffSenseValue = maxSenseValue - minSenseValue;

        // calculate horizontal display parameters
        if (maxPlotLength<0) {
            // all data
            xMin = 0;
            xMax = dataManager.seriesV.getMaxX();
        } else {
            plotLength = Math.min(dataManager.seriesV.getItemCount(), maxPlotLength);
            if (plotLength < maxPlotLength) { // first plot
                xMax = maxPlotLength;
                xMin = 0;
            } else {
                xMax = dataManager.seriesX.getMaxX();
                xMin = xMax - maxPlotLength;
            }
            if (plotLength > posDistance) {
                newQuakeValue = Math.abs(dataManager.seriesV.getY(dataLength - 1) - dataManager.seriesV.getY(dataLength -posDistance)); //5 points distance
            }
            if (plotLength > maxPlotLength/2 + posDistance) {
                snapQuakeValue = Math.abs(dataManager.seriesV.getY(dataLength - maxPlotLength/2) - dataManager.seriesV.getY(dataLength - maxPlotLength/2 - posDistance));
            }
        }

        // calculate vertical display parameters
        if (displayStyle==DisplayConst.DisplayXYZV) {
            if (yMin>(tempY=dataManager.seriesX.getMinY())) yMin=tempY;
            if (yMin>(tempY=dataManager.seriesY.getMinY())) yMin=tempY;
            if (yMin>(tempY=dataManager.seriesZ.getMinY())) yMin=tempY;
            if (yMin>(tempY=dataManager.seriesV.getMinY())) yMin=tempY;
            if (yMax<(tempY=dataManager.seriesX.getMaxY())) yMax=tempY;
            if (yMax<(tempY=dataManager.seriesY.getMaxY())) yMax=tempY;
            if (yMax<(tempY=dataManager.seriesZ.getMaxY())) yMax=tempY;
            if (yMax<(tempY=dataManager.seriesV.getMaxY())) yMax=tempY;
        } else if (displayStyle==DisplayConst.DisplayV) {
            yMin=dataManager.seriesV.getMinY();
            yMax=dataManager.seriesV.getMaxY();
        } else if (displayStyle==DisplayConst.DisplayX) {
            yMin=dataManager.seriesX.getMinY();
            yMax=dataManager.seriesX.getMaxY();
        } else if (displayStyle==DisplayConst.DisplayY) {
            yMin=dataManager.seriesY.getMinY();
            yMax=dataManager.seriesY.getMaxY();
        } else if (displayStyle==DisplayConst.DisplayZ) {
            yMin=dataManager.seriesZ.getMinY();
            yMax=dataManager.seriesZ.getMaxY();
        } else {
            Log.d(TAG,"bad displayStyle code ");
        }
        double yMid = (yMax+yMin)/2;
       // double yAmp= Math.max((yMax-yMin)/2, MainActivity.sensitivityValue/2);
        double yAmp= (yMax-yMin)/2;
        yMax = yMid + yAmp*scaleY;
        yMin = yMid - yAmp*scaleY;

        // set plot range
        renderer.setXAxisMin(xMin);
        renderer.setXAxisMax(xMax);
        renderer.setYAxisMin(yMin);
        renderer.setYAxisMax(yMax);
        chartView.invalidate();  //or repaint() ?
    }
}
