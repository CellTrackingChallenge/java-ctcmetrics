package net.celltrackingchallenge.measures.util;

import net.imagej.ImgPlus;
import net.imglib2.RandomAccess;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.RealType;
import sc.fiji.simplifiedio.SimplifiedIO;
import java.util.Iterator;

public class TestMutualFgDistancesOnRealImages {
	static void testOneCase() {
		final ImgPlus<? extends RealType<?>> img = SimplifiedIO.openImage("/temp/fg_dist_input.tif");
		final int markerOfInterest = 6;

		final int dim = img.numDimensions();
		final MutualFgDistances m = new MutualFgDistances(dim);

		m.resetForSurfaces(); //not necessary
		m.findAndSaveSurface(markerOfInterest,(ImgPlus)img);
		//m.findAndSaveSurface(markerOfInterest,(ImgPlus)img, new FinalInterval(new long[] {250,200,5}, new long[] {270,320,8}));
		final Iterator<Integer> pxs = m.getSurfacePixels(markerOfInterest).iterator();

		final int[] pos = new int[dim];
		LoopBuilder.setImages(img).forEachPixel(RealType::setZero);
		final RandomAccess<? extends RealType<?>> ra = img.randomAccess();
		while (pxs.hasNext()) {
			for (int i = 0; i < dim; ++i) pos[i] = pxs.next();
			ra.setPositionAndGet(pos).setReal(255.f);
		}

		SimplifiedIO.saveImage(img,"/temp/fg_dist_output.tif");
	}

	public static void main(String[] args) {
		testOneCase();
	}
}
