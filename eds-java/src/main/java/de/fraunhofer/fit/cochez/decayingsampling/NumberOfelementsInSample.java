package de.fraunhofer.fit.cochez.decayingsampling;

import java.math.BigDecimal;
import java.math.RoundingMode;

import arithmetic.BigDecimalMath;

public class NumberOfelementsInSample {

	private static final BigDecimal c = BigDecimal.valueOf(0.01);
	private static final BigDecimal alpha = BigDecimal.ONE.subtract(c);
	private static final BigDecimal oneOveralpha = BigDecimal.ONE.divide(alpha, 100, RoundingMode.DOWN);
	private static final BigDecimal T = BigDecimal.valueOf(0.7);
	private static final BigDecimal alphaLogT;

	static {
		BigDecimal natLogT = BigDecimalMath.log(T);
		BigDecimal natLogAlpha = BigDecimalMath.log(alpha);
		alphaLogT = natLogT.divide(natLogAlpha);
	}

	public static void main(String[] args) {

		for (int t_int = 1; t_int < 1000; t_int++) {
			BigDecimal t = BigDecimal.valueOf(t_int);
			BigDecimal onoveralphaPowt = oneOveralpha.pow(t_int);

			BigDecimal amountUncorrected = t.subtract(
					T.multiply(BigDecimal.ONE.subtract(onoveralphaPowt).divide(BigDecimal.ONE.subtract(oneOveralpha))));
//FIXME remove the correction for elements with negative probability
			
		}
	}
}
