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

public class CHA extends AbstractDSmeasure
{
	///a constructor requiring connection to Fiji report/log services
	public CHA(final Logger _log)
	{ super(_log); }


	//---------------------------------------------------------------------/
	/**
	 * The function returns average FG intensity over all objects found that
	 * are present at time points (inclusive) \e from till \e to.
	 * Returns -1 if no object has been found at all.
	 */
	private double avgFGfromTimeSpan(final int from, final int to,
		final Vector<HashMap<Integer,Double>> avgFG)
	{
		if (from < 0 || from >= avgFG.size()) return (-1.0);
		if ( to  < 0 ||  to  >= avgFG.size()) return (-1.0);

		double avg = 0.0;
		int cnt = 0;

		for (int time = from; time <= to; ++time)
		{
			for (Double fg : avgFG.get(time).values())
			{
				avg += fg;
				++cnt;
			}
		}

		return (cnt > 0 ? avg/(double)cnt : -1.0);
	}


	//---------------------------------------------------------------------/
	/// This is the main CHA calculator.
	@Override
	protected double calculateBottomStage()
	{
		//do the bottom stage
		//DEBUG//log.info("Computing the CHA bottom part...");
		double cha = 0.0;
		long videoCnt = 0; //how many videos were processed

		//go over all encountered videos and calc
		//their respective avg. CHAs and average them
		for (ImgQualityDataCache.videoDataContainer data : cache.cachedVideoData)
		{
			//shadows of the/short-cuts to the cache data
			final Vector<HashMap<Integer,Double>> avgFG = data.avgFG;

			double a = -1.0, b = -1.0;
			double l_cha = 0.0;

			if (avgFG.size() < 2)
			{
				throw new IllegalArgumentException("Cannot calculate CHA from less than two images.");
			}
			else
			if (avgFG.size() == 2)
			{
				a = avgFGfromTimeSpan(0,0,avgFG);
				b = avgFGfromTimeSpan(1,1,avgFG);
				l_cha = b - a;
			}
			else
			{
				//use largest possible (possibly overlapping, though) window
				//windows size = 2 time points
				final int last = avgFG.size() - 1;
				a = avgFGfromTimeSpan(0,1,avgFG);
				b = avgFGfromTimeSpan(last-1,last,avgFG);
				l_cha = b - a;
				l_cha /= (double)last;
			}

			if (a < 0.0 || b < 0.0)
				throw new IllegalArgumentException("CHA for video "+data.video
					+": Current implementation cannot deal with images with no FG labels.");

			log.info("CHA_debug: avg. int. "+a+" -> "+b+", over "+avgFG.size()+" frames");
			log.info("CHA for video "+data.video+": "+l_cha);

			cha += l_cha;
			++videoCnt;
		}

		//summarize over all datasets:
		if (videoCnt > 0)
		{
			cha /= (double)videoCnt;
			cha = Math.abs(cha);
			log.info("CHA for dataset: "+cha);
		}
		else
			log.info("CHA for dataset: Couldn't calculate average CHA because there are missing labels.");

		return (cha);
	}
}
