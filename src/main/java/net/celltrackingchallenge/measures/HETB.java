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

public class HETB extends AbstractDSmeasure
{
	///a constructor requiring connection to Fiji report/log services
	public HETB(final LogService _log)
	{ super(_log); }


	//---------------------------------------------------------------------/
	/// This is the main HETB calculator.
	@Override
	protected double calculateBottomStage()
	{
		//do the bottom stage
		//DEBUG//log.info("Computing the HETB bottom part...");
		double hetb = 0.0;
		long videoCnt = 0; //how many videos were processed

		//go over all encountered videos and calc
		//their respective avg. HETBs and average them
		for (ImgQualityDataCache.videoDataContainer data : cache.cachedVideoData)
		{
			double intSum = 0.; //for mean and variance
			double int2Sum = 0.;
			//see ImgQualityDataCache.extractFGObjectStats() for explanation of this variable
			double valShift=-1.;

			//shadows of the/short-cuts to the cache data
			final Vector<HashMap<Integer,Double>> avgFG = data.avgFG;
			final Vector<Double> avgBG = data.avgBG;

			//go over all FG objects and calc their HETBs
			long noFGs = 0;
			double l_hetb = 0.0;
			//over all time points
			for (int time=0; time < avgFG.size(); ++time)
			{
				//skip this frame if it is empty
				if (avgFG.get(time).size() == 0) continue;

				//get average signal height from all objects in the given frame
				//NB: the denominator of the HETb_i,t expression
				double frameAvgFGSignal = 0.0;
				for (Double fg : avgFG.get(time).values())
					frameAvgFGSignal += Math.abs(fg - avgBG.get(time));
				frameAvgFGSignal /= (double)avgFG.get(time).size();

				//over all objects, in fact use their avg intensities
				for (Double fg : avgFG.get(time).values())
				{
					//object signal height "normalized" with respect to the
					//usual signal height in this frame, we have to calculate
					//std.dev. from these values
					l_hetb = (fg - avgBG.get(time)) / frameAvgFGSignal;

					if (valShift == -1) valShift = l_hetb;

					intSum  += (l_hetb-valShift);
					int2Sum += (l_hetb-valShift) * (l_hetb-valShift);
					++noFGs;
				}
			}

			//finish the calculation of the average
			if (noFGs > 0)
			{
				//finish calculation of the variance...
				int2Sum -= (intSum*intSum/(double)noFGs);
				int2Sum /= (double)noFGs;

				//...to get the final standard deviation
				l_hetb = Math.sqrt(int2Sum);

				log.info("HETB for video "+data.video+": "+l_hetb);

				hetb += l_hetb;
				++videoCnt;
			}
			else
				log.info("HETB for video "+data.video+": Couldn't calculate average HETB because there are missing labels.");
		}

		//summarize over all datasets:
		if (videoCnt > 0)
		{
			hetb /= (double)videoCnt;
			log.info("HETB for dataset: "+hetb);
		}
		else
			log.info("HETB for dataset: Couldn't calculate average HETB because there are missing labels.");

		return (hetb);
	}
}
