package de.fraunhofer.fit.cochez.decayingsampling;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.TimeUnit;


public class SlowAlgorithm {

	private final double T;
	private final double alpha;
	private final Random r;
	private final LinkedList<Element> collection = new LinkedList<>();

	public SlowAlgorithm(double t, double alpha, Random r) {
		T = t;
		this.alpha = alpha;
		this.r = r;
	}

	void addElement(int element) {
		collection.add(new Element(element, r.nextDouble()));
		for (Iterator<Element> it = collection.iterator(); it.hasNext();) {
			Element e = it.next();
			if (e.randomNumber < T) {
				it.remove();
			}
			// prepare for next round
			e.randomNumber *= alpha;
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append('[');
		collection.stream().map(n -> n.element + ",").forEach(s::append);
		s.append(']');
		return s.toString();
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

	public static void main(String[] args) {
		SlowAlgorithm s = new SlowAlgorithm(0.8, 0.99999, new Random(79876534L));
		for (int i = 0; i < 1_000_000; i++) {
			s.addElement(i);
			if (i % 1000 == 0) {
				System.out.println(s.collection.size());
			}
		}
	}

}
