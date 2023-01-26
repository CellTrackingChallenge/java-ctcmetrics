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

import org.scijava.log.Logger;

import java.util.Map;
import java.util.Vector;
import java.util.HashMap;

public class SPA extends AbstractDSmeasure
{
	///a constructor requiring connection to Fiji report/log services
	public SPA(final Logger _log)
	{ super(_log); }


	//---------------------------------------------------------------------/
	/// This is the main SPA calculator.
	@Override
	protected double calculateBottomStage()
	{
		//do the bottom stage
		//DEBUG//log.info("Computing the SPA bottom part...");
		double spa = 0.0;
		long fgCnt = 0; //how many videos were processed

		//go over all encountered videos and calc
		//their respective avg. SPAs and average them
		for (ImgQualityDataCache.videoDataContainer data : cache.cachedVideoData)
		{
			//shadows of the/short-cuts to the cache data
			final Vector<HashMap<Integer,Float>> nearDistFG = data.nearDistFG;

			//how many frames contain less than two cells (and are excluded from the stats)
			long noOfBoringFrames = 0;

			//number of objects whose neighbors were not found (within the distance)
			long noIsolatedFGs = 0;

			//go over all FG objects and calc their SPAs
			long noFGs = 0;
			double l_spa = 0.;

			//over all time points
			for (int time=0; time < nearDistFG.size(); ++time)
			{
				//over all objects, in fact use their avg intensities
				for (Map.Entry<Integer,Float> aCellAndItsParam : nearDistFG.get(time).entrySet())
				{
					final double dist = aCellAndItsParam.getValue();
					data.getTableRowFor(time, aCellAndItsParam.getKey()).spa = dist;
					l_spa += dist;
					++noFGs;
					if (dist >= 50.0) ++noIsolatedFGs;
				}

				if (nearDistFG.get(time).isEmpty()) ++noOfBoringFrames;
			}

			//finish the calculation of the average
			if (noFGs > 0)
			{
				log.info("SPA for video "+data.video+": There is "+noIsolatedFGs+" ( "+100.0*noIsolatedFGs/(double)noFGs
					+" %) cells with no neighbor in the range of 50 voxels.");
				log.info("SPA for video "+data.video+": There is "+noOfBoringFrames+" ( "+100.0*noOfBoringFrames/(double)nearDistFG.size()
					+" %) frames with zero or one cell.");
				log.info("SPA for video "+data.video+": "+l_spa/(double)noFGs);

				spa += l_spa;
				fgCnt += noFGs;
			}
			else
				log.info("SPA for video "+data.video+": Couldn't calculate average SPA because there are no cells labelled.");
		}

		//summarize over all datasets:
		if (fgCnt > 0)
		{
			spa /= (double)fgCnt;
			log.info("SPA for dataset: "+spa);
		}
		else
			log.info("SPA for dataset: Couldn't calculate average SPA because there are missing labels.");

		return (spa);
	}
}
