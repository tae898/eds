package de.fraunhofer.fit.cochez.decayingsampling;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.TimeUnit;


public class FastAlgorithm {

	/**
	 TODO: use http://docs.oracle.com/javase/6/docs/api/java/math/BigDecimal.html or http://www.apfloat.org/apfloat_java/
	 
	 TODO: split adding and forwarding of clock
	 
	 */
	
	private final double T;
	private final double alpha;
	private final Random r;
	private final PriorityQueue<Element> collection = new PriorityQueue<>(1000, new Comparator<Element>() {

		@Override
		public int compare(Element o1, Element o2) {
			return Double.compare(o1.randomNumber, o2.randomNumber);
		}
	});
	private double X;
	private double r_t;

	public FastAlgorithm(double t, double alpha, Random r) {
		T = t;
		this.alpha = alpha;
		this.r = r;
		this.r_t = 1;
		this.X = T;
	}

	public void addElement(int element) {
		Element newE = new Element(element, r_t * r.nextDouble());
		collection.add(newE);
		Element temp;
		while ((temp = collection.peek()) != null && temp.randomNumber < X) {
			collection.poll();
		}
		X = X / alpha;
		r_t = r_t / alpha;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append('[');
		collection.stream().map(n -> n.element + ",").forEach(s::append);
		s.append(']');
		return s.toString();
	}


	public int getSize() {
	    return this.collection.size();
    }

	private static class Element {
		int element;
		double randomNumber;

		public Element(int element, double randomNumber) {
			super();
			this.element = element;
			this.randomNumber = randomNumber;
		}
	}

	/*
	public static void main(String[] args) {
		for (double T = 0.00; T < 0.01; T += 0.0001) {
			FastAlgorithm s = new FastAlgorithm(T, 0.99999, new Random(79876534L));
			Stopwatch w = Stopwatch.createStarted();
			for (int i = 0; i < 1_000_000; i++) {
				s.addElement(i);
				if (i % 1000 == 0) {
					System.out.println(s.collection.size());
				}
			}
			w.stop();
			System.out.println("T= " + T + " time= " + w.elapsed(TimeUnit.MILLISECONDS) + " ms");
		}
	}
	*/

}
