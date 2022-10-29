package de.rwth.gross;

import de.fraunhofer.fit.cochez.decayingsampling.FastAlgorithm;
import org.javatuples.Pair;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

public class Main {

    private static final String logFile = "/tmp/var.csv";

    public static void main(String[] args) {
        Map<Double, Pair<Double, Double>> observations = new HashMap<Double, Pair<Double, Double>>();
        double avg = 0;

        double t = 0.5;
        int nr = 1_000_000;

        for (double a = 0.99999; a < 0.999999; a += 0.000001) {
            FastAlgorithm s = new FastAlgorithm(t, a, new Random(79876534L));
            EstimateSample est = new EstimateSample(t, a);

            //perform sampling
            for (int i = 1; i < nr; i++) {
                s.addElement(i);

                //compute avg
                avg = ((avg * (nr-1)) + s.getSize()) / nr;
            }

            System.out.println("#### "+a+ " ####");
            System.out.println("AVG: "+avg);

            BigDecimal estAvg = est.Variance();
            System.out.println("EST: "+estAvg);

            observations.put(a, new Pair(avg, estAvg));

            try {
                writeCSVLine(a, avg, estAvg.doubleValue());
            } catch (IOException e) {
                System.err.println(e);
            }


        }

        //store observations
//        try {
//            writeCSV(observations);
//        } catch (FileNotFoundException e){
//            System.err.println(e);
//        }
    }

    public static void writeCSVLine(double a, double avg, double estAvg) throws IOException {
        FileWriter pw = new FileWriter(logFile,true);
        StringBuilder sb = new StringBuilder();
        sb.append(a);
        sb.append(',');
        sb.append(avg);
        sb.append(',');
        sb.append(estAvg);
        sb.append('\n');

        pw.write(sb.toString());
        pw.flush();
        pw.close();
    }

    public static void writeCSV(Map<Double, Pair<Double, Double>> observations) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(new File("observations.csv"));
        StringBuilder sb = new StringBuilder();
        sb.append("alpha");
        sb.append(',');
        sb.append("avg");
        sb.append(',');
        sb.append("est");
        sb.append('\n');


        pw.write(sb.toString());
        pw.close();

        for (Map.Entry<Double, Pair<Double, Double>> entry : observations.entrySet())
        {
            Pair<Double, Double> obs = entry.getValue();

            sb.append(entry.getKey());
            sb.append(',');

            sb.append(obs.getValue0());
            sb.append(',');

            sb.append(obs.getValue1());
            sb.append('\n');
        }

        pw.write(sb.toString());
        pw.close();
    }
}
