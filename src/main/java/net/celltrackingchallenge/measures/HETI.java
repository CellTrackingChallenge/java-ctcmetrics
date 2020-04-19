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

public class HETI extends AbstractDSmeasure
{
	///a constructor requiring connection to Fiji report/log services
	public HETI(final LogService _log)
	{ super(_log); }


	//---------------------------------------------------------------------/
	/// This is the main HETI calculator.
	@Override
	protected double calculateBottomStage()
	{
		//do the bottom stage
		//DEBUG//log.info("Computing the HETI bottom part...");
		double heti = 0.0;
		long videoCnt = 0; //how many videos were processed

		//go over all encountered videos and calc
		//their respective avg. HETIs and average them
		for (ImgQualityDataCache.videoDataContainer data : cache.cachedVideoData)
		{
			//shadows of the/short-cuts to the cache data
			final Vector<HashMap<Integer,Double>> avgFG = data.avgFG;
			final Vector<HashMap<Integer,Double>> stdFG = data.stdFG;
			final Vector<Double> avgBG = data.avgBG;

			//go over all FG objects and calc their CRs
			long noFGs = 0;
			double l_heti = 0.0;
			//over all time points
			for (int time=0; time < avgFG.size(); ++time)
			{
				//over all objects
				for (Integer fgID : avgFG.get(time).keySet())
				{
					double denom = Math.abs(avgFG.get(time).get(fgID) - avgBG.get(time));
					//exclude close-to-zero denominators (that otherwise escalate/outlay the average)
					if (denom > 0.01)
					{
						l_heti += stdFG.get(time).get(fgID) / denom;
						++noFGs;
					}
				}
			}

			//finish the calculation of the average
			if (noFGs > 0)
			{
				l_heti /= (double)noFGs;
				log.info("HETI for video "+data.video+": "+l_heti);

				heti += l_heti;
				++videoCnt;
			}
			else
				log.info("HETI for video "+data.video+": Couldn't calculate average HETI because there are no cells labelled.");
		}

		//summarize over all datasets:
		if (videoCnt > 0)
		{
			heti /= (double)videoCnt;
			log.info("HETI for dataset: "+heti);
		}
		else
			log.info("HETI for dataset: Couldn't calculate average HETI because there are missing labels.");

		return (heti);
	}
}
