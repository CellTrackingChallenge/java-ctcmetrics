package net.celltrackingchallenge.measures.util;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class TestMutualFgDistances {
	public static void main(String[] args) {
		final Img<UnsignedShortType> img = new ArrayImgFactory<>(new UnsignedShortType()).create(5,5,2);
		RandomAccess<UnsignedShortType> ra = img.randomAccess();
		ra.setPositionAndGet(1,2,1).set(10);
		ra.setPositionAndGet(2,2,1).set(10);
		ra.setPositionAndGet(3,2,1).set(15);
		ra.setPositionAndGet(4,4,1).set(20);
		printImage(img);
		System.out.println("\n-------------");

		final MutualFgDistances m = new MutualFgDistances(3);
		m.findAndSaveSurface(15,img, new FinalInterval(new long[] {2,2,0}, new long[] {4,4,1}));
	}

	static <T extends IntegerType<T>>
	void printImage(final IterableInterval<T> ii) {
		Cursor<T> c = ii.localizingCursor();
		int lastY = -1;
		int lastZ = -1;
		while (c.hasNext()) {
			int val = c.next().getInteger();
			if (lastZ != c.getIntPosition(2)) {
				lastZ = c.getIntPosition(2);
				System.out.print("\nslice "+lastZ+":");
			}
			if (lastY != c.getIntPosition(1)) {
				lastY = c.getIntPosition(1);
				System.out.println();
			}
			System.out.print(val+" ");
		}
	}
}
