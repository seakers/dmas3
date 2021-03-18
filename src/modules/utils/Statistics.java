package modules.utils;

import java.util.ArrayList;

public class Statistics {
    public static double getMax(ArrayList<Double> v){
        double max = Double.NEGATIVE_INFINITY;
        for(Double x : v){
            if(x > max) max = x;
        }

        if(max == Double.NEGATIVE_INFINITY) return Double.NaN;
        return max;
    }

    public static double getMin(ArrayList<Double> v){
        double min = Double.POSITIVE_INFINITY;
        for(Double x : v){
            if(x < min) min = x;
        }
        if(min == Double.POSITIVE_INFINITY) return Double.NaN;
        return min;
    }

    public static double getMean(ArrayList<Double> v){
        double mean = 0.0;
        for(Double x : v){
            mean += x;
        }
        return mean/v.size();
    }

    public static double getStd(ArrayList<Double> v){
        double mean = getMean(v);

        double sum = 0.0;
        for(Double x : v){
            sum += Math.pow( x - mean ,2);
        }

        return Math.sqrt( sum/v.size() );
    }
}
