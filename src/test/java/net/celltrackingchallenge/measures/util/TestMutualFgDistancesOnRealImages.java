/*-
 * #%L
 * CTC-measures
 * %%
 * Copyright (C) 2017 - 2023 Vladimír Ulman
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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

	static void testThreeCases() {
		final ImgPlus<? extends RealType<?>> img = SimplifiedIO.openImage("/temp/fg_dist_input.tif");

		final int dim = img.numDimensions();
		final MutualFgDistances m = new MutualFgDistances(dim);

		m.findAndSaveSurface(6, (ImgPlus)img);
		m.findAndSaveSurface(8, (ImgPlus)img);
		m.findAndSaveSurface(24, (ImgPlus)img);
		m.findAndSaveSurface(10, (ImgPlus)img);
		m.findAndSaveSurface(12, (ImgPlus)img);

		System.out.println("solving a distance between 10 and 24:");
		m.setDistance(10,24, m.computeTwoSurfacesDistance(10,24,9) );

		System.out.println("solving a distance between 12 and 24:");
		m.setDistance(12,24, m.computeTwoSurfacesDistance(12,24,9) );

		System.out.println("best distance 10->24: "+m.getDistance(10,24));
		System.out.println("best distance 12->24: "+m.getDistance(12,24));
	}

	public static void main(String[] args) {
		//testOneCase();
		testThreeCases();
	}
}
