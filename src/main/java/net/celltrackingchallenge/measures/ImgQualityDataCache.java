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
package net.celltrackingchallenge.measures;

import net.celltrackingchallenge.measures.util.MutualFgDistances;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Vertices;
import net.imagej.ops.OpService;
import net.imglib2.AbstractInterval;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.roi.geom.real.DefaultWritablePolygon2D;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.type.logic.BitType;
import org.scijava.log.LogService;

import net.imglib2.img.Img;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.RealType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import io.scif.img.ImgIOException;

import java.util.Arrays;
import java.util.Map;
import java.util.Vector;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

public class ImgQualityDataCache
{
	///shortcuts to some Fiji services
	private final LogService log;

	/**
	 * flag to notify ClassifyLabels() if to call extractObjectDistance()
	 * (which will be called in addition to extractFGObjectStats())
	 */
	public boolean doDensityPrecalculation = false;
	///flag to notify extractFGObjectStats() if to bother itself with surface mesh
	public boolean doShapePrecalculation = false;

	///specifies how many digits are to be expected in the input filenames
	public int noOfDigits = 3;

	///a constructor requiring connection to Fiji report/log services
	public ImgQualityDataCache(final LogService _log)
	{
		//check that non-null was given for _log!
		if (_log == null)
			throw new NullPointerException("No log service supplied.");

		log = _log;
	}

	/**
	 * a constructor requiring connection to Fiji report/log services;
	 * this constructor preserves demanded feature flags as they are
	 * given in the foreign \e _cache; \e _cache can be null and then
	 * nothing is preserved
	 */
	public ImgQualityDataCache(final LogService _log, final ImgQualityDataCache _cache)
	{
		//check that non-null was given for _log!
		if (_log == null)
			throw new NullPointerException("No log service supplied.");

		log = _log;

		if (_cache != null)
		{
			//preserve the feature flags
			doDensityPrecalculation = _cache.doDensityPrecalculation;
			doShapePrecalculation   = _cache.doShapePrecalculation;
		}
	}

	///GT and RES paths combination for which this cache is valid, null means invalid
	private String imgPath = null;
	///GT and RES paths combination for which this cache is valid, null means invalid
	private String annPath = null;

	///reference-based-only check if the parameters are those on which this cache was computed
	public boolean validFor(final String _imgPath, final String _annPath)
	{
		return ( imgPath != null &&  annPath != null
		     && _imgPath != null && _annPath != null
		     && imgPath == _imgPath //intentional match on reference (and on the content)
		     && annPath == _annPath);
	}


	// ----------- the common upper stage essentially starts here -----------
	//auxiliary data:

	///representation of resolution, no dimensionality restriction (unlike in GUI)
	private double[] resolution = null;

	public void setResolution(final double[] _res)
	{
		//check if resolution data is sane
		if (_res == null)
			throw new IllegalArgumentException("No pixel resolution data supplied!");
		for (double r : _res)
			if (r <= 0.0)
				throw new IllegalArgumentException("Negative or zero resolution supplied!");

		//copy the supplied resolution to the class structures,
		resolution = new double[_res.length];
		for (int n=0; n < resolution.length; ++n)
			resolution[n] = _res[n];
	}

	/**
	 * This class holds all relevant data that are a) needed for individual
	 * measures to carry on their calculations and b) that are shared between
	 * these measures (so there is no need to scan the raw images all over again
	 * and again, once per every measure) and c) that are valid for one video
	 * (see the this.cachedVideoData).
	 */
	public class videoDataContainer
	{
		public videoDataContainer(final int __v)
		{ video = __v; }

		///number/ID of the video this data belongs to
		public int video;

		/**
		 * Representation of average & std. deviations within individual
		 * foreground masks.
		 * Usage: avgFG[timePoint].get(labelID) = averageIntensityValue
		 */
		public final Vector<HashMap<Integer,Double>> avgFG = new Vector<>(1000,100);
		/// Similar to this.avgFG
		public final Vector<HashMap<Integer,Double>> stdFG = new Vector<>(1000,100);

		/// Stores NUMBER OF VOXELS (not a real volume) of the FG masks at time points.
		public final Vector<HashMap<Integer,Long>> volumeFG = new Vector<>(1000,100);

		/// Converts this.volumeFG values (no. of voxels) into a real volume (in cubic micrometers)
		public double getRealVolume(final long vxlCnt)
		{
			double v = (double)vxlCnt;
			for (double r : resolution) v *= r;
			return (v);
		}

		/// Stores the circularity (for 2D data) or sphericity (for 3D data)
		public final Vector<HashMap<Integer,Double>> shaValuesFG = new Vector<>(1000,100);

		/**
		 * Stores how many voxels are there in the intersection of masks of the same
		 * marker at time point and previous time point.
		 */
		public final Vector<HashMap<Integer,Long>> overlapFG = new Vector<>(1000,100);

		/**
		 * Stores how many voxels are there in between the marker and its nearest
		 * neighboring (other) marker at time points. The distance is measured with
		 * Chamfer distance (which considers diagonals in voxels) and thus the value
		 * is not necessarily an integer anymore. The resolution (size of voxels)
		 * of the image is not taken into account.
		 */
		public final Vector<HashMap<Integer,Float>> nearDistFG = new Vector<>(1000,100);

		/**
		 * Stores axis-aligned, 3D bounding box around every discovered FG marker (per each timepoint,
		 * just like it is the case with most of the attributes around). Pixel coordinates are used.
		 */
		public final Vector<Map<Integer,int[]>> boundingBoxesFG = new Vector<>(1000,100);

		/**
		 * Representation of average & std. deviations of background region.
		 * There is only one background marker expected in the images.
		 */
		public final Vector<Double> avgBG = new Vector<>(1000,100);
		/// Similar to this.avgBG
		public final Vector<Double> stdBG = new Vector<>(1000,100);
	}

	/// this list holds relevant data for every discovered video
	List<videoDataContainer> cachedVideoData = new LinkedList<>();

	//---------------------------------------------------------------------/
	//aux data fillers -- merely markers' properties calculator

	/**
	 * The cursor \e imgPosition points into the raw image at voxel whose counterparting voxel
	 * in the \e imgFGcurrent image stores the first (in the sense of \e imgPosition internal
	 * sweeping order) occurence of the marker that is to be processed with this function.
	 *
	 * This function pushes into global data at the specific \e time .
	 */
	private <T extends RealType<T>>
	void extractFGObjectStats(final Cursor<T> imgPosition, final int time, //who: "object" @ time
		final RandomAccessibleInterval<UnsignedShortType> imgFGcurrent,     //where: input masks
		final RandomAccessibleInterval<UnsignedShortType> imgFGprevious,
		final videoDataContainer data)
	{
		//working pointers into the mask images
		final RandomAccess<UnsignedShortType> fgCursor = imgFGcurrent.randomAccess();

		//obtain the ID of the processed object
		//NB: imgPosition points already at sure existing voxel
		fgCursor.setPosition(imgPosition);
		final int marker = fgCursor.get().getInteger();

		//init aux variables:
		double intSum = 0.; //for single-pass calculation of mean and variance
		double int2Sum = 0.;
		//according to: https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Computing_shifted_data
		//to fight against numerical issues we introduce a "value shifter",
		//which we can already initiate with an "estimate of mean" which we
		//derive from the object's first spotted voxel value
		//NB: imgPosition points already at sure existing voxel
		final double valShift=imgPosition.get().getRealDouble();

		//the voxel counter (for volume)
		long vxlCnt = 0L;

		//working copy of the input cursor, this one drives the image sweeping
		//sweep the image and search for this object/marker
		final Cursor<T> rawCursor = imgPosition.copyCursor();
		rawCursor.reset();
		while (rawCursor.hasNext())
		{
			rawCursor.next();
			fgCursor.setPosition(rawCursor);

			if (fgCursor.get().getInteger() == marker)
			{
				//processing voxel that belongs to the current FG object:
				//increase current volume
				++vxlCnt;

				final double val = rawCursor.get().getRealDouble();
				intSum += (val-valShift);
				int2Sum += (val-valShift) * (val-valShift);
			}
		}
		//must hold: vxlCnt > 1 (otherwise ClassifyLabels wouldn't call this function)

		//finish processing of the FG objects stats:
		//mean intensity
		data.avgFG.get(time).put(marker, (intSum / (double)vxlCnt) + valShift );

		//variance
		int2Sum -= (intSum*intSum/(double)vxlCnt);
		int2Sum /= (double)vxlCnt;
		//
		//std. dev.
		data.stdFG.get(time).put(marker, Math.sqrt(int2Sum) );

		//voxel count
		data.volumeFG.get(time).put(marker, vxlCnt );

		//also process the "overlap feature" (if the object was found in the previous frame)
		if (time > 0 && data.volumeFG.get(time-1).get(marker) != null)
			data.overlapFG.get(time).put(marker,
				measureObjectsOverlap(imgPosition,imgFGcurrent, marker,imgFGprevious) );
	}


	private
	double computeSphericity(final int fgValue,
	                         final RandomAccessibleInterval<UnsignedShortType> imgFGcurrent, //FG mask
	                         final RandomAccessibleInterval<BitType> imgTmpSharedStorage)
	{
		OpService ops = log.getContext().getService(OpService.class);
		if (ops == null)
			throw new RuntimeException("computeSphericity() is missing the Ops service in its context, sorry.");

		//extract the FG mask into separate binary image
		LoopBuilder.setImages(imgFGcurrent, imgTmpSharedStorage)
				.forEachPixel((s, t) -> {
					if (s.getInteger() == fgValue) t.setOne();
					else t.setZero();
				});

		final Mesh m = ops.geom().marchingCubes(imgTmpSharedStorage);

		//apply resolution correction
		final Vertices mv = m.vertices();
		for (int cnt = 0; cnt < mv.size(); ++cnt) {
			mv.setPosition(cnt, resolution[0]*mv.x(cnt), resolution[1]*mv.y(cnt), resolution[2]*mv.z(cnt) );
		}

		//System.out.println("      size: "+ops.geom().size(m));
		//System.out.println("   surface: "+ops.geom().boundarySize(m));
		return ops.geom().sphericity(m).getRealDouble();
	}

	private
	double computeCircularity(final int fgValue,
	                          final RandomAccessibleInterval<UnsignedShortType> imgFGcurrent, //FG mask
	                          final RandomAccessibleInterval<BitType> imgTmpSharedStorage)
	{
		OpService ops = log.getContext().getService(OpService.class);
		if (ops == null)
			throw new RuntimeException("computeCircularity() is missing the Ops service in its context, sorry.");

		//extract the FG mask into separate binary image
		LoopBuilder.setImages(imgFGcurrent, imgTmpSharedStorage)
				.forEachPixel((s, t) -> {
					if (s.getInteger() == fgValue) t.setOne();
					else t.setZero();
				});

		Polygon2D p = ops.geom().contour(imgTmpSharedStorage, true);
		if (p.numDimensions() != 2)
			throw new RuntimeException("computeCircularity() failed extracting 2D polygon, sorry.");

		//apply resolution correction
		if (resolution[0] != 1.0 || resolution[1] != 1.0) {
			log.info("Applying 2D resolution correction for SHA measure: "+ Arrays.toString(resolution));
			final double[] x = new double[p.numVertices()];
			final double[] y = new double[p.numVertices()];
			for (int cnt = 0; cnt < p.numVertices(); ++cnt) {
				x[cnt] = resolution[0] * p.vertices().get(cnt).getDoublePosition(0);
				y[cnt] = resolution[1] * p.vertices().get(cnt).getDoublePosition(1);
			}
			p = new DefaultWritablePolygon2D(x,y);
		}

		return ops.geom().circularity(p).getRealDouble();
	}


	/**
	 * The functions counts how many times the current marker (see below) in the image \e imgFGcurrent
	 * co-localizes with marker \e prevID in the image \e imgFGprevious. This number is returned.
	 *
	 * The cursor \e imgPosition points into the raw image at voxel whose counterparting voxel
	 * in the \e imgFGcurrent image stores the first (in the sense of \e imgPosition internal
	 * sweeping order) occurence of the marker that is to be processed with this function.
	 */
	private <T extends RealType<T>>
	long measureObjectsOverlap(final Cursor<T> imgPosition, //pointer to the raw image -> gives current FG ID
	                           final RandomAccessibleInterval<UnsignedShortType> imgFGcurrent, //FG mask
									   final int prevID, //prev FG ID
	                           final RandomAccessibleInterval<UnsignedShortType> imgFGprevious) //FG mask
	{
		//working copy of the input cursor, this one drives the image sweeping
		final Cursor<T> rawCursor = imgPosition.copyCursor();

		//working pointers into the (current and previous) object masks
		final RandomAccess<UnsignedShortType> fgCursor = imgFGcurrent.randomAccess();
		final RandomAccess<UnsignedShortType> prevFgCursor = imgFGprevious.randomAccess();

		//obtain the ID of the processed object
		//NB: imgPosition points already at sure existing voxel
		//NB: rawCursor is as if newly created cursor, i.e., now it points before the image
		fgCursor.setPosition(imgPosition);
		final int marker = fgCursor.get().getInteger();

		//return value...
		long count = 0;

		rawCursor.reset();
		while (rawCursor.hasNext())
		{
			rawCursor.next();
			fgCursor.setPosition(rawCursor);
			prevFgCursor.setPosition(rawCursor);

			if (fgCursor.get().getInteger() == marker && prevFgCursor.get().getInteger() == prevID)
				++count;
		}

		return(count);
	}


	private int[] _location = new int[3];
	private void assureArrayLengthFor(final int length)
	{
		if (_location.length != length) _location = new int[length];
	}
	//
	private int[] createBox(final Localizable loc)
	{
		final int D = loc.numDimensions();
		assureArrayLengthFor(D);
		//
		loc.localize(_location);
		final int[] bbox = new int[D+D];
		for (int d = 0; d < D; ++d) {
			bbox[d]   = _location[d];
			bbox[d+D] = _location[d];
		}
		return bbox;
	}
	private void extendBox(final int[] bbox, final Localizable loc)
	{
		final int D = loc.numDimensions();
		assureArrayLengthFor(D);
		//
		loc.localize(_location);
		for (int d = 0; d < D; ++d) {
			bbox[d]   = Math.min(bbox[d],   _location[d]);
			bbox[d+D] = Math.max(bbox[d+D], _location[d]);
		}
	}
	//
	private Interval wrapBoxWithInterval(final int[] bbox)
	{
		if (_interval.numDimensions() * 2 != bbox.length)
			_interval = new BboxBackedInterval(bbox);
		_interval.wrapAroundBbox(bbox);
		return _interval;
	}
	private BboxBackedInterval _interval = new BboxBackedInterval(3);
	static class BboxBackedInterval extends AbstractInterval {
		private BboxBackedInterval(final int n) {
			super(n);
		}
		public BboxBackedInterval(final int[] bbox) {
			super(bbox.length / 2);
		}
		public void wrapAroundBbox(final int[] bbox) {
			final int D = bbox.length / 2;
			for (int d = 0; d < D; ++d) {
				min[d] = bbox[d];
				max[d] = bbox[d+D];
			}
		}
	}


	public <T extends RealType<T>>
	void ClassifyLabels(final int time,
	                    IterableInterval<T> imgRaw,
	                    RandomAccessibleInterval<UnsignedByteType> imgBG,
	                    Img<UnsignedShortType> imgFG,
	                    RandomAccessibleInterval<UnsignedShortType> imgFGprev,
	                    final videoDataContainer data)
	{
		//uses resolution from the class internal structures, check it is set already
		if (resolution == null)
			throw new IllegalArgumentException("No pixel resolution data is available!");
		//assume that resolution is sane

		//check we have a resolution data available for every dimension
		if (imgRaw.numDimensions() > resolution.length)
			throw new IllegalArgumentException("Raw image has greater dimensionality"
				+" than the available resolution data.");

		//check the sizes of the images
		if (imgRaw.numDimensions() != imgFG.numDimensions())
			throw new IllegalArgumentException("Raw image and FG label image"
				+" are not of the same dimensionality.");
		if (imgRaw.numDimensions() != imgBG.numDimensions())
			throw new IllegalArgumentException("Raw image and BG label image"
				+" are not of the same dimensionality.");

		for (int n=0; n < imgRaw.numDimensions(); ++n)
			if (imgRaw.dimension(n) != imgFG.dimension(n))
				throw new IllegalArgumentException("Raw image and FG label image"
					+" are not of the same size.");
		for (int n=0; n < imgRaw.numDimensions(); ++n)
			if (imgRaw.dimension(n) != imgBG.dimension(n))
				throw new IllegalArgumentException("Raw image and BG label image"
					+" are not of the same size.");

		//.... populate the internal structures ....
		//first, frame-related stats variables:
		long volBGvoxelCnt = 0L;
		long volFGvoxelCnt = 0L;
		long volFGBGcollisionVoxelCnt = 0L;

		double intSum = 0.; //for mean and variance
		double int2Sum = 0.;
		//see extractFGObjectStats() for explanation of this variable
		double valShift=-1.;

		//bounding boxes
		final Map<Integer,int[]> bboxes = new HashMap<>(1000);
		data.boundingBoxesFG.add(bboxes);

		//sweeping variables:
		final Cursor<T> rawCursor = imgRaw.localizingCursor();
		final RandomAccess<UnsignedByteType> bgCursor = imgBG.randomAccess();
		final RandomAccess<UnsignedShortType> fgCursor = imgFG.randomAccess();

		while (rawCursor.hasNext())
		{
			//update cursors...
			rawCursor.next();
			bgCursor.setPosition(rawCursor);
			fgCursor.setPosition(rawCursor);

			//analyze background voxels
			if (bgCursor.get().getInteger() > 0)
			{
				if (fgCursor.get().getInteger() > 0)
				{
					//found colliding BG voxel, exclude it from BG stats
					++volFGBGcollisionVoxelCnt;
				}
				else
				{
					//found non-colliding BG voxel, include it for BG stats
					++volBGvoxelCnt;

					final double val = rawCursor.get().getRealDouble();
					if (valShift == -1) valShift = val;

					intSum += (val-valShift);
					int2Sum += (val-valShift) * (val-valShift);
				}
			}
			if (fgCursor.get().getInteger() > 0)
			{
				++volFGvoxelCnt; //found FG voxel, update FG stats
				bboxes.putIfAbsent(fgCursor.get().getInteger(), createBox(rawCursor));
				extendBox(bboxes.get(fgCursor.get().getInteger()), rawCursor);
			}
		}

		//report the "occupancy stats"
		log.info("Frame at time "+time+" overview:");
		final long imgSize = imgRaw.size();
		log.info("all FG voxels           : "+volFGvoxelCnt+" ( "+100.0*(double)volFGvoxelCnt/imgSize+" %)");
		log.info("pure BG voxels          : "+volBGvoxelCnt+" ( "+100.0*(double)volBGvoxelCnt/imgSize+" %)");
		log.info("BG&FG overlapping voxels: "+volFGBGcollisionVoxelCnt+" ( "+100.0*(double)volFGBGcollisionVoxelCnt/imgSize+" %)");
		final long untouched = imgSize - volFGvoxelCnt - volBGvoxelCnt;
		log.info("not annotated voxels    : "+untouched+" ( "+100.0*(double)untouched/imgSize+" %)");
		//
		for (int marker : bboxes.keySet())
			log.trace("bbox for marker "+marker+": "+ Arrays.toString(bboxes.get(marker)));

		//finish processing of the BG stats of the current frame
		if (volBGvoxelCnt > 0)
		{
			//great, some pure-background voxels have been found
			data.avgBG.add( (intSum / (double)volBGvoxelCnt) + valShift );

			int2Sum -= (intSum*intSum/(double)volBGvoxelCnt);
			int2Sum /= (double)volBGvoxelCnt;
			data.stdBG.add( Math.sqrt(int2Sum) );
		}
		else
		{
			log.info("Warning: Background annotation has no pure background voxels.");
			data.avgBG.add( 0.0 );
			data.stdBG.add( 0.0 );
		}

		//now, sweep the image, detect all labels and calculate & save their properties
		log.info("Retrieving per object statistics, might take some time...");
		//
		//set to remember already discovered labels
		//(with initial capacity for 1000 labels)
		HashSet<Integer> mDiscovered = new HashSet<>(1000);

		//prepare the per-object data structures
		data.avgFG.add( new HashMap<>() );
		data.stdFG.add( new HashMap<>() );
		data.volumeFG.add( new HashMap<>() );
		data.shaValuesFG.add( new HashMap<>() );
		data.overlapFG.add( new HashMap<>() );
		data.nearDistFG.add( new HashMap<>() );

		final MutualFgDistances fgDists = new MutualFgDistances(imgFG.numDimensions());
		if (doDensityPrecalculation)
		{
			//get all boundary pixels
			for (int marker : bboxes.keySet())
				fgDists.findAndSaveSurface( marker, imgFG,
						wrapBoxWithInterval(bboxes.get(marker)) );

			//fill the distance matrix
			for (int markerA : bboxes.keySet())
				for (int markerB : bboxes.keySet())
					if (markerA != markerB && fgDists.getDistance(markerA,markerB) == Float.MAX_VALUE)
						fgDists.setDistance(markerA,markerB,
								fgDists.computeTwoSurfacesDistance(markerA,markerB, 9) );
		}

		final Img<BitType> fgBinaryTmp = doShapePrecalculation ?
				imgFG.factory().imgFactory(new BitType()).create(imgFG) : null;
		final boolean doSphericity = imgFG.numDimensions() == 3;

		rawCursor.reset();
		while (rawCursor.hasNext())
		{
			//update cursors...
			rawCursor.next();
			fgCursor.setPosition(rawCursor);

			//analyze foreground voxels
			final int curMarker = fgCursor.get().getInteger();
			if ( curMarker > 0 && (!mDiscovered.contains(curMarker)) )
			{
				//found not-yet-processed FG object
				extractFGObjectStats(rawCursor, time, imgFG, imgFGprev, data);

				if (doShapePrecalculation)
					data.shaValuesFG.get(time).put( curMarker,
							doSphericity ? computeSphericity(curMarker,imgFG,fgBinaryTmp)
									: computeCircularity(curMarker,imgFG,fgBinaryTmp) );

				if (doDensityPrecalculation)
					data.nearDistFG.get(time).put( curMarker,
							fgDists.getDistance(curMarker, fgDists.getClosestNeighbor(curMarker)) );

				//mark the object (and all its voxels consequently) as processed
				mDiscovered.add(curMarker);
			}
		}
	}

	//---------------------------------------------------------------------/
	/**
	 * Measure calculation happens in two stages. The first/upper stage does
	 * data pre-fetch and calculations to populate the ImgQualityDataCache.
	 * ImgQualityDataCache.calculate() actually does this job. The sort of
	 * calculations is such that the other measures could benefit from
	 * it and re-use it (instead of loading images again and doing same
	 * calculations again), and the results are stored in the cache.
	 * The second/bottom stage is measure-specific. It basically finishes
	 * the measure calculation procedure, possible using data from the cache.
	 *
	 * This function computes the common upper stage of measures.
	 */
	public void calculate(final String imgPath, final double[] resolution,
	                      final String annPath)
	throws IOException, ImgIOException
	{
		//this functions actually only iterates over video folders
		//and calls this.calculateVideo() for every folder

		//test and save the given resolution
		setResolution(resolution);

		//single or multiple (does it contain a "01" subfolder) video situation?
		if (Files.isDirectory( Paths.get(imgPath,"01") ))
		{
			//multiple video situation: paths point on a dataset
			int video = 1;
			while (Files.isDirectory( Paths.get(imgPath,(video > 9 ? String.valueOf(video) : "0"+video)) ))
			{
				final videoDataContainer data = new videoDataContainer(video);
				calculateVideo(String.format("%s/%02d",imgPath,video),
				               String.format("%s/%02d_GT",annPath,video), data);
				this.cachedVideoData.add(data);
				++video;
			}
		}
		else
		{
			//single video situation
			final videoDataContainer data = new videoDataContainer(1);
			calculateVideo(imgPath,annPath,data);
			this.cachedVideoData.add(data);
		}

		//now that we got here, note for what data
		//this cache is valid, see validFor() above
		this.imgPath = imgPath;
		this.annPath = annPath;
	}

	/// this functions processes given video folders and outputs to \e data
	@SuppressWarnings({"unchecked","rawtypes"})
	public void calculateVideo(final String imgPath,
	                           final String annPath,
	                           final videoDataContainer data)
	throws IOException, ImgIOException
	{
		log.info("IMG path: "+imgPath);
		log.info("ANN path: "+annPath);
		//DEBUG//log.info("Computing the common upper part...");

		//we gonna re-use image loading functions...
		final TrackDataCache tCache = new TrackDataCache(log);

		//iterate through the RAW images folder and read files, one by one,
		//find the appropriate file in the annotations folders,
		//and call ClassifyLabels() for every such tripple,
		//
		//check also previous frame for overlap size
		Img<UnsignedShortType> imgFGprev = null;
		//
		int time = 0;
		while (Files.isReadable(
			new File(String.format("%s/t%0"+noOfDigits+"d.tif",imgPath,time)).toPath()))
		{
			//read the image triple (raw image, FG labels, BG label)
			Img<?> img
				= tCache.ReadImage(String.format("%s/t%0"+noOfDigits+"d.tif",imgPath,time));

			Img<UnsignedShortType> imgFG
				= tCache.ReadImageG16(String.format("%s/TRA/man_track%0"+noOfDigits+"d.tif",annPath,time));

			Img<UnsignedByteType> imgBG
				= tCache.ReadImageG8(String.format("%s/BG/mask%0"+noOfDigits+"d.tif",annPath,time));

			ClassifyLabels(time, (IterableInterval)img, imgBG, imgFG, imgFGprev, data);

			imgFGprev = null; //be explicit that we do not want this in memory anymore
			imgFGprev = imgFG;
			++time;

			//to be on safe side (with memory)
			img = null;
			imgFG = null;
			imgBG = null;
		}
		imgFGprev = null;

		if (time == 0)
			throw new IllegalArgumentException("No raw image was found!");

		if (data.volumeFG.size() != time)
			throw new IllegalArgumentException("Internal consistency problem with FG data!");

		if (data.avgBG.size() != time)
			throw new IllegalArgumentException("Internal consistency problem with BG data!");
	}
}
