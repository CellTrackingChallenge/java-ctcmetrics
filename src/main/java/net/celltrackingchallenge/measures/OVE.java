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

public class OVE extends AbstractDSmeasure
{
	///a constructor requiring connection to Fiji report/log services
	public OVE(final LogService _log)
	{ super(_log); }


	//---------------------------------------------------------------------/
	/// This is the main OVE calculator.
	@Override
	protected double calculateBottomStage()
	{
		//do the bottom stage
		//DEBUG//log.info("Computing the OVE bottom part...");
		double ove = 0.0;
		long videoCnt = 0; //how many videos were processed

		//go over all encountered videos and calc
		//their respective avg. OVEs and average them
		for (ImgQualityDataCache.videoDataContainer data : cache.cachedVideoData)
		{
			//shadows of the/short-cuts to the cache data
			final Vector<HashMap<Integer,Long>> volumeFG = data.volumeFG;
			final Vector<HashMap<Integer,Long>> overlapFG = data.overlapFG;

			//go over all FG objects and calc their OVEs
			long noFGs = 0;
			double l_ove = 0.0;

			//over all time points (NB: no overlap possible for time==0)
			for (int time=1; time < overlapFG.size(); ++time)
			{
				//over all objects
				for (Integer fgID : overlapFG.get(time).keySet())
				{
					l_ove += (double)overlapFG.get(time).get(fgID) / (double)volumeFG.get(time).get(fgID);
					++noFGs;
				}
			}

			//finish the calculation of the average
			if (noFGs > 0)
			{
				l_ove /= (double)noFGs;
				log.info("OVE for video "+data.video+": "+l_ove);

				ove += l_ove;
				++videoCnt;
			}
			else
				log.info("OVE for video "+data.video+": Couldn't calculate average OVE because there are no cells labelled.");
		}

		//summarize over all datasets:
		if (videoCnt > 0)
		{
			ove /= (double)videoCnt;
			log.info("OVE for dataset: "+ove);
		}
		else
			log.info("OVE for dataset: Couldn't calculate average OVE because there are missing labels.");

		return (ove);
	}
}
