package net.celltrackingchallenge.measures.util;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.view.Views;
import net.imglib2.view.IntervalView;
import net.imglib2.Interval;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Objects;

public class MutualFgDistances {

	public MutualFgDistances(final int forThisDimensionality) {
		if (forThisDimensionality != 2 && forThisDimensionality != 3)
			throw new IllegalArgumentException("Sorry, MutualFgDistances is supporting only 2 and 3 dimensional images.");

		dimCnt = forThisDimensionality;
		_location = new int[dimCnt];
		pxsNeigsPattern = dimCnt == 2 ? PXS_2D_NEIGS_PATTERN : PXS_3D_NEIGS_PATTERN;
	}

	//technically-oriented params:
	final int dimCnt;
	private final int[] _location;

	static public final long[][] PXS_2D_NEIGS_PATTERN = { //dx,dy
			{0,-1}, {-1,0}, {+1,0}, {0,+1} };
	static public final long[][] PXS_3D_NEIGS_PATTERN = { //dx,dy,dz
			{0,0,-1},   {0,-1,0}, {-1,0,0}, {+1,0,0}, {0,+1,0},   {0,0,+1} };

	//data-oriented params:
	private final long[][] pxsNeigsPattern; //one of the two constants above...

	private final Map<Integer, List<Integer>> surfaceCoordsPerLabel = new HashMap<>(2000);
	private final Map<SymmetricPair, Float> distMatrixBetweenLabels = new HashMap<>(2000);

	public
	void resetForSurfaces() {
		surfaceCoordsPerLabel.clear();
		distMatrixBetweenLabels.clear();
	}

	public
	List<Integer> getSurfacePixels(final int ofThisMarker) {
		return Collections.unmodifiableList( surfaceCoordsPerLabel.getOrDefault(ofThisMarker, Collections.emptyList()) );
	}

	public
	void setDistance(final int firstMarker, final int secondMarker,
	                 final float dist) {
		distMatrixBetweenLabels.put(new SymmetricPair(firstMarker,secondMarker), dist);
	}

	public
	float getDistance(final int firstMarker, final int secondMarker) {
		_pair.set(firstMarker,secondMarker);
		return distMatrixBetweenLabels.getOrDefault(_pair, Float.MAX_VALUE);
	}
	private final SymmetricPair _pair = new SymmetricPair(0,0); //only aux variable

	public
	String printAllDistances() {
		final StringBuilder sb = new StringBuilder();
		for (Map.Entry<SymmetricPair,Float> d : distMatrixBetweenLabels.entrySet())
			sb.append(d.getKey().a)
					.append(" <-> ")
					.append(d.getKey().b)
					.append(" = ")
					.append(d.getValue())
					.append(" pixels  [")
					.append(d.getKey().toString())
					.append("]\n");
		return sb.toString();
	}

	public
	int getClosestNeighbor(final int ofThisMarker) {
		int bestMarker = -1;
		float bestDist = Float.MAX_VALUE;
		for (Map.Entry<SymmetricPair,Float> c : distMatrixBetweenLabels.entrySet()) {
			if (c.getKey().a == ofThisMarker && c.getValue() < bestDist) {
				bestMarker = c.getKey().b;
				bestDist = c.getValue();
			}
			if (c.getKey().b == ofThisMarker && c.getValue() < bestDist) {
				bestMarker = c.getKey().a;
				bestDist = c.getValue();
			}
		}
		return bestMarker;
	}

	static class SymmetricPair {
		int a,b;
		SymmetricPair(int _a, int _b) { set(_a,_b); }
		void set(int _a, int _b) { a = _a; b = _b; }
		//
		@Override
		public boolean equals(Object o) {
			if (o == this) return true;
			if (!(o instanceof SymmetricPair)) return false;

			SymmetricPair other = (SymmetricPair)o;
			if (a == other.a && b == other.b) return true;
			if (a == other.b && b == other.a) return true;
			return false;
		}
		//
		@Override
		public int hashCode() {
			return Objects.hashCode(a*b);
		}
	}

	public <T extends IntegerType<T>>
	void findAndSaveSurface(final int ofThisMarker,
	                        final RandomAccessibleInterval<T> inThisMask,
	                        final Interval withinThisROI) {
		//prepare (potentially) reduced image of the original data...
		final IntervalView<T> pixels = Views.interval(inThisMask, withinThisROI);
		//...and an image of zero-extended original data (to shift and reduce it later)
		final IntervalView<T> expandedMask = Views.expandZero(inThisMask, 5, 5, 5);

		surfaceCoordsPerLabel.put(ofThisMarker, new ArrayList<>(600000));
		for (long[] dir : pxsNeigsPattern)
			findAndSaveSurface(ofThisMarker,pixels, expandedMask,dir);
	}

	public <T extends IntegerType<T>>
	void findAndSaveSurface(final int ofThisMarker,
	                        final RandomAccessibleInterval<T> inThisMask) {
		findAndSaveSurface(ofThisMarker, inThisMask, inThisMask);
	}

	private <T extends IntegerType<T>>
	void findAndSaveSurface(final int ofThisMarker,
	                        final IntervalView<T> pixels,
	                        final IntervalView<T> expandedMask,
	                        final long[] broDirection) {
		//shortcut...
		final List<Integer> boundary = surfaceCoordsPerLabel.get(ofThisMarker);

		final Cursor<T> bro = Views.interval(Views.translate(expandedMask,broDirection), pixels).cursor();
		final Cursor<T> ref = pixels.localizingCursor();
		int refVal,broVal;

		while (ref.hasNext()) {
			refVal = ref.next().getInteger();
			broVal = bro.next().getInteger();
			if (refVal == ofThisMarker && broVal != ofThisMarker) {
				ref.localize(_location);
				for (int l : _location) boundary.add(l);
				//System.out.println(Arrays.toString(_location));
			}
		}
	}
}