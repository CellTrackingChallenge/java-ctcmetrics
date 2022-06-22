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

import java.util.Vector;
import java.util.HashMap;

public class DEN extends AbstractDSmeasure
{
	///a constructor requiring connection to Fiji report/log services
	public DEN(final Logger _log)
	{ super(_log); }


	//---------------------------------------------------------------------/
	/// This is the main DEN calculator.
	@Override
	protected double calculateBottomStage()
	{
		//do the bottom stage
		//DEBUG//log.info("Computing the DEN bottom part...");
		double den = 0.0;
		long fgCnt = 0; //how many videos were processed

		//go over all encountered videos and calc
		//their respective avg. DENs and average them
		for (ImgQualityDataCache.videoDataContainer data : cache.cachedVideoData)
		{
			//shadows of the/short-cuts to the cache data
			final Vector<HashMap<Integer,Float>> nearDistFG = data.nearDistFG;

			//number of objects whose neighbors were not found (within the distance)
			long noIsolatedFGs = 0;

			//go over all FG objects and calc their DENs
			long noFGs = 0;
			double l_den = 0.;

			//over all time points
			for (int time=0; time < nearDistFG.size(); ++time)
			{
				//over all objects, in fact use their avg intensities
				for (Float dist : nearDistFG.get(time).values())
				{
					l_den += (double)dist;
					++noFGs;
					if (dist == 50.0) ++noIsolatedFGs;
				}
			}

			//finish the calculation of the average
			if (noFGs > 0)
			{
				log.info("DEN for video "+data.video+": There is "+noIsolatedFGs+" ( "+100.0*noIsolatedFGs/(double)noFGs
					+" %) cells with no neighbor in the range of 50 voxels.");
				log.info("DEN for video "+data.video+": "+l_den/(double)noFGs);

				den += l_den;
				fgCnt += noFGs;
			}
			else
				log.info("DEN for video "+data.video+": Couldn't calculate average DEN because there are no cells labelled.");
		}

		//summarize over all datasets:
		if (fgCnt > 0)
		{
			den /= (double)fgCnt;
			log.info("DEN for dataset: "+den);
		}
		else
			log.info("DEN for dataset: Couldn't calculate average DEN because there are missing labels.");

		return (den);
	}
}
