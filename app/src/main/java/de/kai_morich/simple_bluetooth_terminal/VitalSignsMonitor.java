package de.kai_morich.simple_bluetooth_terminal;


//ploteo 5 segundos

import android.widget.TextView;

import com.jjoe64.graphview.GraphView;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VitalSignsMonitor {
    //DEFINEs
    private static final int TIME_TO_PLOT = 5; //in seconds
    private static final int ECG_FS = 200; //Hz
    public static final int PPG_FS = 25; //Hz

    private static List<Double> ECG_samples = new ArrayList<Double>() {{
        add(0.0);
    }};
    private static List<Double> PPG_samples = new ArrayList<Double>() {{
        add(0.0);
    }};

    private static double curr_temp=0.0;
    private static int curr_heart_rate=0;
    private static int curr_spo2=0;

    public static double GetCurrentTemp(){ return curr_temp;}
    public static int GetCurrentHeartRate(){ return curr_heart_rate;}
    public static int GetCurrentSpO2(){ return curr_spo2;}



    public static LineGraphSeries<DataPoint> GetECGInitialData() {
        LineGraphSeries<DataPoint> ECG_new_samples = new LineGraphSeries<>(new DataPoint[]{new DataPoint(-5.0, 0.0)});
        ECG_new_samples.setThickness(5);

        return ECG_new_samples;
    }

    public static LineGraphSeries<DataPoint> GetPPGInitialData() {
        LineGraphSeries<DataPoint> PPG_new_samples = new LineGraphSeries<>(new DataPoint[]{new DataPoint(-5.0, 0.0)});
        PPG_new_samples.setThickness(5);
        return PPG_new_samples;
    }

    public static void HeartBeat(byte heart_rate) {
        int bpm = (int) heart_rate & 0xFF;
        if(GetCurrentHeartRate() != bpm) {
            curr_heart_rate = bpm;
            VitalSignsMonitorFragment.text_bpm.setText(String.valueOf(bpm) + " BPM");
        }
    }

    public static void Oxygen(byte oxygen) {
        int spO2 = (int) oxygen & 0xFF;
        if(GetCurrentSpO2() != spO2) {
            curr_spo2 = spO2;
            VitalSignsMonitorFragment.text_spo2.setText(String.valueOf(spO2) + "%");
        }
    }

    public static void Temperature(byte part_entera, byte part_decimal) {
        double temp = (int) part_entera;
        temp += part_decimal /256.0;
        VitalSignsMonitorFragment.text_temp.setText(String.format("%.2f", temp) + "°C");

    }

    public static void UpdateECGGraph(byte[] bytes, int len) {
        double total_points = ECG_FS * TIME_TO_PLOT;

        UpdateDatapoints(bytes, len, total_points, ECG_samples);
        for (int i = 0; i < ECG_samples.size(); i++) {
         //       ECG_samples.set(i, ECG_samples.get(i)*3.3/65536.0);
        };
        //preparo los datapoints para plotear
        DataPoint[] dataPoints = new DataPoint[ECG_samples.size()]; // declare an array of DataPoint objects with the same size as your list
        double step = 1.0 / (ECG_FS);
        for (int j = 0; j < ECG_samples.size(); j++) {
            // add new DataPoint object to the array for each of your list entries
            double time = -ECG_samples.size() * step + step * j;
            dataPoints[j] = new DataPoint(time, ECG_samples.get(j)*3.3/65536.0); // not sure but I think the second argument should be of type double
        }

        VitalSignsMonitorFragment.ECGDataPoints.resetData(dataPoints);


        //VitalSignsMonitorFragment.ECGgraph.getViewport().setMinX(-5.1);
        //VitalSignsMonitorFragment.ECGgraph.getViewport().setMaxX(0.5);
        VitalSignsMonitorFragment.ECGgraph.getViewport().setMinY(VitalSignsMonitorFragment.ECGDataPoints.getLowestValueY());
        VitalSignsMonitorFragment.ECGgraph.getViewport().setMaxY(VitalSignsMonitorFragment.ECGDataPoints.getHighestValueY());
        VitalSignsMonitorFragment.ECGgraph.getViewport().setYAxisBoundsManual(true);
        //VitalSignsMonitorFragment.ECGgraph.getViewport().setXAxisBoundsManual(true);

    }

    public static void UpdatePPGGraph(byte[] bytes, int len) {
        double total_points = PPG_FS * TIME_TO_PLOT;

        UpdateDatapoints(bytes, len, total_points, PPG_samples);

        for (int i = 0; i < PPG_samples.size(); i++) {
            if (PPG_samples.get(i) > 32768) {
                PPG_samples.set(i, PPG_samples.get(i) - 65536);
            }
        }


        //preparo los datapoints para plotear
        DataPoint[] dataPoints = new DataPoint[PPG_samples.size()]; // declare an array of DataPoint objects with the same size as your list
        double step = 1.0 / (PPG_FS);
        for (int j = 0; j < PPG_samples.size(); j++) {
            // add new DataPoint object to the array for each of your list entries
            double time = -PPG_samples.size() * step + step * j;
            dataPoints[j] = new DataPoint(time, PPG_samples.get(j)/1000.0); // not sure but I think the second argument should be of type double
        }

        VitalSignsMonitorFragment.PPGDataPoints.resetData(dataPoints);


        //VitalSignsMonitorFragment.ECGgraph.getViewport().setMinX(-5.1);
        //VitalSignsMonitorFragment.ECGgraph.getViewport().setMaxX(0.5);
        VitalSignsMonitorFragment.PPGgraph.getViewport().setMinY(VitalSignsMonitorFragment.PPGDataPoints.getLowestValueY() );
        VitalSignsMonitorFragment.PPGgraph.getViewport().setMaxY(VitalSignsMonitorFragment.PPGDataPoints.getHighestValueY() * 1.1);
        VitalSignsMonitorFragment.PPGgraph.getViewport().setYAxisBoundsManual(true);
        //VitalSignsMonitorFragment.ECGgraph.getViewport().setXAxisBoundsManual(true);
    }

    private static void UpdateDatapoints(byte[] bytes, int len, double total_points, List<Double> samples) {
        if(bytes.length==len*2) {
            for (int i = 0; i < len; i++) {
                int p_alta = (int) bytes[i * 2] & 0xff;
                int p_baja = (int) bytes[i * 2 + 1] & 0xff;
                double sample = p_alta << 8 | p_baja;

                //agrego la muestra actual y saco el ultimo elemento(que es mas viejo ahora)
                if (samples.size() > 0) {
                    samples.add(samples.size(), sample);
                    if (samples.size() == 2 && samples.get(0) == 0)
                        samples.remove(0);
                    if (samples.size() > total_points)
                        samples.remove(0);
                }
            }
        }
    }
}


