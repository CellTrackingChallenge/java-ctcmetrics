package net.celltrackingchallenge.measures.util;

public class TestMutualFgDistancesMatrices {
	public static void main(String[] args) {
		final MutualFgDistances d = new MutualFgDistances(2);

		d.setDistance(10,20,100);
		d.setDistance(20,10,110);
		d.setDistance(50,60,200);

		System.out.println( d.getDistance(10,20) );
		System.out.println( d.getDistance(20,10) );
		System.out.println( d.getDistance(15,20) );
		System.out.println( d.getDistance(60,50) );
		System.out.println( d.getDistance(50,60) );
		System.out.println( d.printAllDistances() );
	}
}
