/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2017, Vladimír Ulman
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.celltrackingchallenge.measures.util;

import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;

public class testJaccard
{
	public static void main(final String... args)
	{
		final String fileImgA = "/home/ulman/data/MAC20/02_bestIndiv_CVUT-CZ/mask035__3D.tif";
		final String fileImgB = "/home/ulman/data/MAC20/02_manAnnot/man_seg035__3D.tif";

		Img<?> imgA = ImageJFunctions.wrap( new ImagePlus( fileImgA ));
		Img<?> imgB = ImageJFunctions.wrap( new ImagePlus( fileImgB ));

		// ------ next test ------
		long a = System.currentTimeMillis();
		System.out.println("Jaccard   is " + Jaccard.Jaccard(
			(RandomAccessibleInterval)imgA,881,
			(RandomAccessibleInterval)imgB,17) );

		long b = System.currentTimeMillis();
		System.out.println("JaccardLB is " + Jaccard.JaccardLB(
			(RandomAccessibleInterval)imgA,881,
			(RandomAccessibleInterval)imgB,17) );

		long c = System.currentTimeMillis();
		System.out.println("timings: "+(b-a)+", "+(c-b)+" millis");

		// ------ next test ------
		a = System.currentTimeMillis();
		System.out.println("Jaccard   is " + Jaccard.Jaccard(
			(RandomAccessibleInterval)imgA,373,
			(RandomAccessibleInterval)imgB,3) );

		b = System.currentTimeMillis();
		System.out.println("JaccardLB is " + Jaccard.JaccardLB(
			(RandomAccessibleInterval)imgA,373,
			(RandomAccessibleInterval)imgB,3) );

		c = System.currentTimeMillis();
		System.out.println("timings: "+(b-a)+", "+(c-b)+" millis");
	}
}
