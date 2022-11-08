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

public class CR extends AbstractDSmeasure
{
	///a constructor requiring connection to Fiji report/log services
	public CR(final Logger _log)
	{ super(_log); }


	//---------------------------------------------------------------------/
	/// This is the main CR calculator.
	@Override
	protected double calculateBottomStage()
	{
		//do the bottom stage
		//DEBUG//log.info("Computing the CR bottom part...");
		double cr = 0.0;
		long videoCnt = 0; //how many videos were processed

		//go over all encountered videos and calc
		//their respective avg. CRs and average them
		for (ImgQualityDataCache.videoDataContainer data : cache.cachedVideoData)
		{
			//shadows of the/short-cuts to the cache data
			final Vector<HashMap<Integer,Double>> avgFG = data.avgFG;
			final Vector<Double> avgBG = data.avgBG;

			//go over all FG objects and calc their CRs
			long noFGs = 0;
			double l_cr = 0.0;
			//over all time points
			for (int time=0; time < avgFG.size(); ++time)
			{
				//skip this frame if we cannot compute anything on it
				if (avgBG.get(time) == 0.0) continue;

				//over all objects, in fact use their avg intensities
				for (Map.Entry<Integer,Double> aCellAndItsParam : avgFG.get(time).entrySet())
				{
					final double one_cr = aCellAndItsParam.getValue() / avgBG.get(time);
					data.getTableRowFor(time, aCellAndItsParam.getKey()).cr = one_cr;
					l_cr += one_cr;
					++noFGs;
				}
			}

			//finish the calculation of the average
			if (noFGs > 0)
			{
				l_cr /= (double)noFGs;
				log.info("CR for video "+data.video+": "+l_cr);

				cr += l_cr;
				++videoCnt;
			}
			else
				log.info("CR for video "+data.video+": Couldn't calculate average CR because there are missing labels.");
		}

		//summarize over all datasets:
		if (videoCnt > 0)
		{
			cr /= (double)videoCnt;
			log.info("CR for dataset: "+cr);
		}
		else
			log.info("CR for dataset: Couldn't calculate average CR because there are missing labels.");

		return (cr);
	}
}
