package net.celltrackingchallenge.measures.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.Collection;
import java.util.TreeSet;

import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.log.Logger;

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
	private final int widthOfPostProcessingErosion;

	//params - reporting facility
	private final Logger logger;

	private BgMaskCreator(final String inFiles, final String outFiles,
	                      final Set<Integer> tps, final boolean doOneMask,
	                      final int noOfErosions, final Logger log) {
		inputFilesPattern = inFiles;
		outputFilesPattern = outFiles;
		timepoints = tps;
		doMaskValidForAllTPs = doOneMask;
		widthOfPostProcessingErosion = noOfErosions;
		logger = log;
	}

	public static class Builder {
		public String inputFilesPattern = "man_track%03d.tif";
		public String outputFilesPattern = "mask%03d.tif";
		public Set<Integer> timepoints = new TreeSet<>();
		public boolean doMaskValidForAllTPs = false;
		public int widthOfPostProcessingErosion = 0;
		public Logger logger = null;

		public BgMaskCreator build() {
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

		public Builder createIndividualMaskForEachTimepoint() {
			doMaskValidForAllTPs = false;
			return this;
		}
		public Builder findMaskValidForAllTimepoints() {
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
				.setSciJavaLogger(new Context(LogService.class).getService(LogService.class));
		if (args.length == 5) b.findMaskValidForAllTimepoints();
		else b.createIndividualMaskForEachTimepoint();

		b.build().run();
	}


	public void run() {
		logger.info("Getting masks for files: "+inputFilesPattern);
		logger.info("... for timepoints: "+timepoints);
		logger.info("... with One mask for all: "+doMaskValidForAllTPs);
		logger.info("... with erosion width: "+widthOfPostProcessingErosion);
		logger.info("Saving masks as: "+outputFilesPattern);
	}
}
