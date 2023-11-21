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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.Collection;
import java.util.TreeSet;

import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.view.Views;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.algorithm.morphology.Erosion;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;

import org.scijava.log.Logger;
import sc.fiji.simplifiedio.SimplifiedIO;
import net.celltrackingchallenge.measures.TrackDataCache;

public class BgMaskCreator {
	//params - data specs (in CTC context):
	//  folder with TRA/man_trackTTT.tif
	//  (to populate folder BG/maskTTT.tif)
	//  no. of digits
	//  time span (selection)
	private final String inputFilesPattern;
	private final String outputFilesPattern;
	private final Set<Integer> timepoints;

	//params - operation mode:
	//  boolean: own mask per TP, or one universal mask
	//  post processing: how many pixels-wide erosion
	private final boolean doMaskValidForAllTPs;
	private final Shape postProcessingSE;

	//params - reporting facility
	private final Logger logger;

	private BgMaskCreator(final String inFiles, final String outFiles,
	                      final Set<Integer> tps, final boolean doOneMask,
	                      final int noOfErosions, final Logger log) throws IOException {
		inputFilesPattern = inFiles;
		outputFilesPattern = outFiles;
		timepoints = tps;
		doMaskValidForAllTPs = doOneMask;
		postProcessingSE = noOfErosions > 0 ? new HyperSphereShape(noOfErosions) : null;
		logger = log;

		int sepIdx = outputFilesPattern.lastIndexOf(File.separator);
		if (sepIdx > 0) {
			//there is some folder path in the pattern
			final Path outFolder = Paths.get(outputFilesPattern.substring(0, sepIdx));
			if (!Files.exists(outFolder)) {
				logger.info("Creating output folder:  "+outFolder);
				Files.createDirectory(outFolder);
			} else if (!Files.isDirectory(outFolder)) {
				throw new IllegalStateException("Output folder '"+outFolder+"' exists but is not a directory!");
			}
		}
	}

	public static class Builder {
		public String inputFilesPattern = "man_track%03d.tif";
		public String outputFilesPattern = "mask%03d.tif";
		public Set<Integer> timepoints = new TreeSet<>();
		public boolean doMaskValidForAllTPs = false;
		public int widthOfPostProcessingErosion = 0;
		public Logger logger = null;

		public BgMaskCreator build() throws IOException {
			if (logger == null)
				throw new IllegalStateException("Some logger must be set beforehand.");

			if (timepoints.isEmpty())
				logger.warn("Creating BG mask for no timepoints!");

			return new BgMaskCreator(inputFilesPattern,outputFilesPattern,timepoints,
					doMaskValidForAllTPs,widthOfPostProcessingErosion,logger);
		}

		public Builder setInputFilesPattern(final String prefix, final int numberOfDigits, final String postfix) {
			if (numberOfDigits < 1)
				throw new IllegalArgumentException("The timepoint index must include at least one digit (requested " +numberOfDigits+")!");
			inputFilesPattern = prefix+"%0"+numberOfDigits+"d"+postfix;
			return this;
		}
		public Builder setOutputFilesPattern(final String prefix, final int numberOfDigits, final String postfix) {
			if (numberOfDigits < 1)
				throw new IllegalArgumentException("The timepoint index must include at least one digit (requested " +numberOfDigits+")!");
			outputFilesPattern = prefix+"%0"+numberOfDigits+"d"+postfix;
			return this;
		}

		public Builder setupForCTC(final Path videoFolder, final int usedNumberOfDigits) {
			if (usedNumberOfDigits < 1)
				throw new IllegalArgumentException("The timepoint index should be 3 or 4 digits, cannot be " +usedNumberOfDigits+"!");
			final String digitsSubstr = "%0"+usedNumberOfDigits+"d";

			final String commonPath = videoFolder.toString();
			inputFilesPattern = commonPath+File.separator+"TRA"+File.separator+"man_track"+digitsSubstr+".tif";
			outputFilesPattern = commonPath+File.separator+"BG"+File.separator+"mask"+digitsSubstr+".tif";
			doMaskValidForAllTPs = true;
			widthOfPostProcessingErosion = 2;
			return this;
		}
		public Builder setupForCTC(final Path videoFolder, final int usedNumberOfDigits, final int widthOfPostProcessingErosion) {
			this.setupForCTC(videoFolder, usedNumberOfDigits);
			this.postProcessMasksByErosionOfWidth(widthOfPostProcessingErosion);
			return this;
		}

		public Builder forTheseTimepointsOnly(final Collection<Integer> TPs) {
			timepoints.clear();
			timepoints.addAll(TPs);
			return this;
		}
		public Builder addAlsoTheseTimepoints(final Collection<Integer> TPs) {
			timepoints.addAll(TPs);
			return this;
		}
		public Builder alsoForThisTimepoint(final int TP) {
			timepoints.add(TP);
			return this;
		}

		public Builder setupToCreateIndividualMaskForEachTimepoint() {
			doMaskValidForAllTPs = false;
			return this;
		}
		public Builder setupToFindOneMaskValidForAllTimepoints() {
			doMaskValidForAllTPs = true;
			return this;
		}

		public Builder doNoPostprocessing() {
			widthOfPostProcessingErosion = 0;
			return this;
		}
		public Builder postProcessMasksByErosionOfWidth(final int width) {
			if (width < 0)
				throw new IllegalArgumentException("Width (given "+width+") cannot be negative!");
			widthOfPostProcessingErosion = width;
			return this;
		}

		public Builder setSciJavaLogger(final Logger log) {
			if (log == null)
				throw new IllegalArgumentException("Some logger must be given!");
			logger = log;
			return this;
		}
	}


	public static void main(String[] args) {
		if (args.length != 4 && args.length != 5) {
			System.out.println("Expecting args: CTCfolder noOfDigits erosionWidth timepointsRange [onwMaskForAll]");
			return;
		}

		String mainFolder = args[0];
		int digits = Integer.parseInt(args[1]);
		int erosion = Integer.parseInt(args[2]);
		Collection<Integer> tps = NumberSequenceHandler.toSet(args[3]);

		final Builder b = new Builder()
				.setupForCTC(Paths.get(mainFolder),digits,erosion)
				.forTheseTimepointsOnly(tps)
				.setSciJavaLogger(new SimpleConsoleLogger());
		if (args.length == 5) b.setupToFindOneMaskValidForAllTimepoints();
		else b.setupToCreateIndividualMaskForEachTimepoint();

		try {
			b.build().run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public void run() throws IOException {
		logger.info("Getting masks for files: "+inputFilesPattern);
		logger.info("... for timepoints: "+timepoints);
		logger.info("... with One mask for all: "+doMaskValidForAllTPs);
		logger.info("Saving masks as: "+outputFilesPattern);
		logger.info("-------------");

		final TrackDataCache loader = new TrackDataCache(logger);
		Img<UnsignedByteType> bgImg = null;
		Img<UnsignedByteType> bgImgPostProcessed = null;
		ExtendedRandomAccessibleInterval<UnsignedByteType, Img<UnsignedByteType>> extViewBgImage = null;

		for (int tp : timepoints) {
			//load input
			Img<UnsignedShortType> fgImg = loader.ReadImageG16(String.format(inputFilesPattern, tp));

			//setup output
			if (bgImg == null) {
				//first timepoint -> get memory to hold aux and output images
				bgImg = fgImg.factory().imgFactory(new UnsignedByteType()).create(fgImg);
				if (postProcessingSE != null) {
					//view on the same data, just with a broader coord domain...
					extViewBgImage = Views.extendValue(bgImg, 255);
					//the same (reusable) output image
					bgImgPostProcessed = bgImg.factory().create(bgImg);
				} else {
					bgImgPostProcessed = bgImg;
				}

				LoopBuilder.setImages(bgImg).forEachPixel(UnsignedByteType::setOne);
				//NB: we gonna be clearing pixels where fg has some label
			}
			LoopBuilder.setImages(fgImg,bgImg).forEachPixel((fg,bg) -> { if (fg.getInteger() > 0) bg.setZero(); });

			if (!doMaskValidForAllTPs) {
				postProcessMask(extViewBgImage,bgImgPostProcessed);
				saveMask(bgImgPostProcessed,tp);
				LoopBuilder.setImages(bgImg).forEachPixel(UnsignedByteType::setOne);
			}
		}

		if (doMaskValidForAllTPs) {
			postProcessMask(extViewBgImage,bgImgPostProcessed);
			//
			//saves the first time point
			saveMask(bgImgPostProcessed, timepoints.iterator().next());
			//or
			//saves all the timepoints
			//for (int tp : timepoints) saveMask(bgImgPostProcessed, tp);
		}
	}

	<T extends IntegerType<T>>
	void postProcessMask(final ExtendedRandomAccessibleInterval<T, Img<T>> inImg, final Img<T> outImg) {
		if (postProcessingSE == null) return;
		logger.info("Going to post process...");
		Erosion.erode(inImg,outImg,postProcessingSE,10);
	}

	void saveMask(final RandomAccessibleInterval<? extends IntegerType<?>> img, final int tp) {
		final String filename = String.format(outputFilesPattern,tp);
		SimplifiedIO.saveImage(img, filename);
		logger.info("Saved BGmask: "+filename);

		bgCounter[0] = 0;
		LoopBuilder.setImages(img).forEachPixel(p -> { if (p.getInteger() > 0) bgCounter[0]++; });

		long pxSize = 1;
		for (int d = 0; d < img.numDimensions(); ++d) pxSize *= img.dimension(d);
		logger.info("... with "+bgCounter[0]+ " voxels in the mask -> "
				+(100*bgCounter[0]/pxSize)+"% coverage");
	}

	final long[] bgCounter = new long[1];
}
