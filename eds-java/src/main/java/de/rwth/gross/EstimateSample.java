package de.rwth.gross;

import ch.obermuhlner.math.big.BigDecimalMath;
import ch.obermuhlner.math.big.BigFloat;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class EstimateSample {
    private BigDecimal x;
    private BigDecimal p = new BigDecimal(1);
    private BigDecimal alpha;

    private BigDecimal frac;
    private MathContext mc = new MathContext(32);

    /**
     * Creates a new estimator, init the scenario
     * @param t
     * @param alpha
     */
    public EstimateSample(double t, double alpha) {
        this.x = new BigDecimal(t);
        this.alpha = new BigDecimal(alpha);

        this.frac = this.x.divide(this.p, 8, RoundingMode.HALF_UP);
    }

    /**
     * Calculates maximum size of given scenario
     * @return max size
     */
    public BigDecimal Max() {
        //return new BigDecimal(1).add(this.frac.divide(BigDecimalMath.log(this.alpha), 2, RoundingMode.HALF_UP));
        return new BigDecimal(1).add(BigDecimalMath.log(this.frac, this.mc).divide(BigDecimalMath.log(alpha, this.mc), 32, RoundingMode.HALF_DOWN));
    }

    /**
     * Calculates the expected size of the given scenario
     * @return expected size
     */
    public BigDecimal Expected() {
        double max = this.Max().doubleValue();
        int maxRound = (int) Math.floor(max);

        System.out.println("[MAX: "+maxRound+"]");

        BigDecimal e = new BigDecimal(0);
        for (int i = 1; i<= maxRound; i++) {

            int exponent = i-maxRound;
            BigDecimal prob = this.frac.divide(alpha.pow(Math.abs(exponent)), 32, RoundingMode.HALF_DOWN);

            //add to exp
            BigDecimal d = BigDecimal.ONE.subtract(prob);
            e = e.add(d);

            //print current status of calculation
            if (i % 100 == 0) {
                System.out.println( ( (((double) i)/maxRound)*100) +"% - e:"+ e+  " - d: "+ d);
            }
        }
        return e;
    }


    public BigDecimal Variance() {
        double max = this.Max().doubleValue();
        int maxRound = (int) Math.floor(max);

        System.out.println("[MAX: "+maxRound+"]");

        BigDecimal e = new BigDecimal(0);
        for (int i = 1; i<= maxRound; i++) {

            int exponent = i-maxRound;
            BigDecimal prob = this.frac.divide(alpha.pow(Math.abs(exponent)), 32, RoundingMode.HALF_DOWN);
            BigDecimal iprob = BigDecimal.ONE.subtract(prob);

            //add to exp
            BigDecimal d = prob.multiply(iprob);
            e = e.add(d);

            //print current status of calculation
            if (i % 100 == 0) {
                System.out.println( ( (((double) i)/maxRound)*100) +"% - e:"+ e+  " - d: "+ d);
            }
        }
        return e;
    }
}
