package statefulsharding.graph.algorithms;

import statefulsharding.Pair;

import java.util.Collection;

/**
 * Created by abubakar on 23/10/17.
 */
public class StatAlgorithms {

    public static double Mean(Collection<Double> values){

        double sum = 0.0;

        for(Double d : values){
            sum+=d;
        }

        return sum/values.size();
    }

    public static Pair<Double, Double> ConfIntervals(Collection<Double> values, double level){

        double num = 0.0;

        double mean = Mean(values);

        for(Double d : values){
            double numi = Math.pow(d-mean,2);
            num+=numi;
        }

        double dev = Math.sqrt(num/values.size());

        double multiplier=1.96;

        if(level==95){
            //do nothing
        }
        else if(level==90){
            multiplier = 1.645;
        }
        else if(level==98){
            multiplier = 2.326;
        }
        else if(level==99){
            multiplier = 2.576;
        }


        return new Pair<>(mean, (dev*multiplier)/Math.sqrt(values.size()));

    }

}
