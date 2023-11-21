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
