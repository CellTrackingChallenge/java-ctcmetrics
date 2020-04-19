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

import org.scijava.log.LogService;

import java.util.Vector;
import java.util.HashMap;

/*
 * ====================================================================================
 *       THIS CLASS IS NOT PROPERLY IMPLEMENTED! DO NOT USE! (and sorry for that)
 * ====================================================================================
 */

public class SHA extends AbstractDSmeasure
{
	///a constructor requiring connection to Fiji report/log services
	public SHA(final LogService _log)
	{ super(_log); }


	//---------------------------------------------------------------------/
	/// This is the main SHA calculator.
	@Override
	protected double calculateBottomStage()
	{
		//do the bottom stage
		//DEBUG//log.info("Computing the SHA bottom part...");
		double sha = 0.0;
		long videoCnt = 0; //how many videos were processed

		//go over all encountered videos and calc
		//their respective avg. SHAs and average them
		for (ImgQualityDataCache.videoDataContainer data : cache.cachedVideoData)
		{
			//shadows of the/short-cuts to the cache data
			final Vector<HashMap<Integer,Long>> volumeFG = data.volumeFG;

			//go over all FG objects and calc their RESs
			long noFGs = 0;
			double l_sha = 0.0;
			//over all time points
			for (int time=0; time < volumeFG.size(); ++time)
			{
				//over all objects
				for (Long vol : volumeFG.get(time).values())
				{
					l_sha += (double)vol;
					++noFGs;
				}
			}

			//finish the calculation of the average
			if (noFGs > 0)
			{
				l_sha /= (double)noFGs;
				log.info("RES for video "+data.video+": "+l_sha);

				sha += l_sha;
				++videoCnt;
			}
			else
				log.info("RES for video "+data.video+": Couldn't calculate average RES because there are no cells labelled.");
		}



		//NOTES:
		//use imagej-ops to convert RAI to DefaultMesh
		//via: src/main/java/net/imagej/ops/geom/geom3d/DefaultMarchingCubes.java
		// (need to figure out how binarization is achieved -- see .isolevel and interpolatorClass)
		//
		//That seems to be an implementation of http://paulbourke.net/geometry/polygonise/
		//(which is likely a defacto standard approach, 1st hit on Google at least)
		//
		//once image is meshified, go through all facets of the mesh and stretch vertices
		//according to the current resolution, call mesh.getSurfaceArea() afterwards
		//
		//(alternatively, make a copy of the MC class, and change it by
		// incorporating the resolution directly, see L178-L186)
		//
		//some MC is in 3D_Viewer by Ulrik, some MC is done Kyle...
		//see: https://gitter.im/fiji/fiji/archives/2016/01/22


		//summarize over all datasets:
		if (videoCnt > 0)
		{
			sha /= (double)videoCnt;
			log.info("SHA for dataset: "+sha);
		}
		else
			log.info("SHA for dataset: Couldn't calculate average SHA because there are missing labels.");

		return (sha);
	}
}
