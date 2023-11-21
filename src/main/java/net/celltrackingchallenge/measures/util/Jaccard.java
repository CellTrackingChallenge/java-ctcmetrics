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

import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.Views;
import net.imglib2.loops.LoopBuilder;

public class Jaccard {
	/** label-free, aka binary, variant of the Jaccard similarity coefficient */
	static public <T extends RealType<T>>
	double Jaccard( final RandomAccessibleInterval<T> imgA, final RandomAccessibleInterval<T> imgB )
	{
		//sanity checks first: images must be of the same size
		if (imgA.numDimensions() != imgB.numDimensions())
			throw new IllegalArgumentException("Both input images have to be of the same dimension.");

		final int n = imgA.numDimensions();
		int i = 0;
		while ( i < n && imgA.dimension(i) ==  imgB.dimension(i) ) ++i;
		if (i < n)
			throw new IllegalArgumentException("Images differ in size in dimension "+i+".");

		//Jaccard is size of an intersection over size of an union,
		//and we consider binary images...
		long interSize = 0;
		long unionSize = 0;

		//sweeping stuff
		final Cursor<T> cA = Views.iterable(imgA).localizingCursor();
		final RandomAccess<T> cB = imgB.randomAccess();

		while (cA.hasNext())
		{
			final boolean isFGA = cA.next().getRealFloat() > 0;

			cB.setPosition( cA );
			final boolean isFGB = cB.get().getRealFloat() > 0;

			interSize += isFGA && isFGB ? 1 : 0;
			unionSize += isFGA || isFGB ? 1 : 0;
		}

		return ( (double)interSize / (double)unionSize );
	}


	static public <TA extends RealType<TA>, TB extends RealType<TB>>
	double Jaccard( final RandomAccessibleInterval<TA> imgA, final double labelA,
	                final RandomAccessibleInterval<TB> imgB, final double labelB )
	{
		//sanity checks first: images must be of the same size
		if (imgA.numDimensions() != imgB.numDimensions())
			throw new IllegalArgumentException("Both input images have to be of the same dimension.");

		final int n = imgA.numDimensions();
		int i = 0;
		while ( i < n && imgA.dimension(i) ==  imgB.dimension(i) ) ++i;
		if (i < n)
			throw new IllegalArgumentException("Images differ in size in dimension "+i+".");

		//Jaccard is size of an intersection over size of an union,
		//and we consider binary images...
		long interSize = 0;
		long unionSize = 0;

		//sweeping stuff
		final Cursor<TA> cA = Views.iterable(imgA).localizingCursor();
		final RandomAccess<TB> cB = imgB.randomAccess();

		while (cA.hasNext())
		{
			final boolean isFGA = cA.next().getRealDouble() == labelA;

			cB.setPosition( cA );
			final boolean isFGB = cB.get().getRealDouble() == labelB;

			interSize += isFGA && isFGB ? 1 : 0;
			unionSize += isFGA || isFGB ? 1 : 0;
		}

		//System.out.println("intersection = " + interSize + ", union = " + unionSize);
		return ( (double)interSize / (double)unionSize );
	}


	static public <T extends IntegerType<T>>
	double Jaccard( final RandomAccessibleInterval<T> imgA, final int labelA,
	                final RandomAccessibleInterval<T> imgB, final int labelB )
	{
		//sanity checks first: images must be of the same size
		if (imgA.numDimensions() != imgB.numDimensions())
			throw new IllegalArgumentException("Both input images have to be of the same dimension.");

		final int n = imgA.numDimensions();
		int i = 0;
		while ( i < n && imgA.dimension(i) ==  imgB.dimension(i) ) ++i;
		if (i < n)
			throw new IllegalArgumentException("Images differ in size in dimension "+i+".");

		//Jaccard is size of an intersection over size of an union,
		//and we consider binary images...
		long interSize = 0;
		long unionSize = 0;

		//sweeping stuff
		final Cursor<T> cA = Views.iterable(imgA).localizingCursor();
		final RandomAccess<T> cB = imgB.randomAccess();

		while (cA.hasNext())
		{
			final boolean isFGA = cA.next().getInteger() == labelA;

			cB.setPosition( cA );
			final boolean isFGB = cB.get().getInteger() == labelB;

			interSize += isFGA && isFGB ? 1 : 0;
			unionSize += isFGA || isFGB ? 1 : 0;
		}

		//System.out.println("intersection = " + interSize + ", union = " + unionSize);
		return ( (double)interSize / (double)unionSize );
	}


	/** Jaccard similarity coefficient for the two labels
	    (e.g. for all pixels with value of 'labelA' in the image 'imgA'),
	    implemented via the LoopBuilder */
	static public <T extends IntegerType<T> >
	double JaccardLB( final RandomAccessibleInterval<T> imgA, final int labelA,
	                  final RandomAccessibleInterval<T> imgB, final int labelB )
	{
		//NB: LoopBuilder checks the dimensions of imgA and imgB,
		//    so we don't do it here...

		//Jaccard is size of an intersection over size of an union
		//
		//here we need 'final' references in order to be able to use them inside
		//the lambda function below (inside the LoopBuilder),
		final long[] sizes = new long[2];
		final int interCntIdx = 0;
		final int unionCntIdx = 1;

		LoopBuilder.setImages(imgA,imgB).forEachPixel( (A, B) ->
		{
			final boolean isFGA = A.getInteger() == labelA;
			final boolean isFGB = B.getInteger() == labelB;

			if (isFGA || isFGB)
			{
				//NB: LoopBuilder is not multithreaded -> no race issues on 'sizes'
				sizes[interCntIdx] += isFGA && isFGB ? 1 : 0;
				sizes[unionCntIdx]++;
			}
		} );

		//System.out.println("intersection = " + sizes[interCntIdx] + ", union = " + sizes[unionCntIdx]);
		return ( (double)sizes[interCntIdx] / (double)sizes[unionCntIdx] );
	}
}
