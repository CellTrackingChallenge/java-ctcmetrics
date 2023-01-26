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

public class RES extends AbstractDSmeasure
{
	///a constructor requiring connection to Fiji report/log services
	public RES(final Logger _log)
	{ super(_log); }


	//---------------------------------------------------------------------/
	/// This is the main RES calculator.
	@Override
	protected double calculateBottomStage()
	{
		//do the bottom stage
		//DEBUG//log.info("Computing the RES bottom part...");
		double res = 0.0;
		long fgCnt = 0; //how many videos were processed

		//go over all encountered videos and calc
		//their respective avg. RESes and average them
		for (ImgQualityDataCache.videoDataContainer data : cache.cachedVideoData)
		{
			//shadows of the/short-cuts to the cache data
			final Vector<HashMap<Integer,Long>> volumeFG = data.volumeFG;

			//go over all FG objects and calc their RESs
			long noFGs = 0;
			double l_res = 0.0;
			//over all time points
			for (int time=0; time < volumeFG.size(); ++time)
			{
				//over all objects
				for (Map.Entry<Integer,Long> aCellAndItsParam : volumeFG.get(time).entrySet())
				{
					final double vol = aCellAndItsParam.getValue();
					data.getTableRowFor(time, aCellAndItsParam.getKey()).res = vol;
					l_res += vol;
					++noFGs;
				}
			}

			//finish the calculation of the average
			if (noFGs > 0)
			{
				log.info("RES for video "+data.video+": "+l_res/(double)noFGs);

				res += l_res;
				fgCnt += noFGs;
			}
			else
				log.info("RES for video "+data.video+": Couldn't calculate average RES because there are no cells labelled.");
		}

		//summarize over all datasets:
		if (fgCnt > 0)
		{
			res /= (double)fgCnt;
			log.info("RES for dataset: "+res);
		}
		else
			log.info("RES for dataset: Couldn't calculate average RES because there are missing labels.");

		return (res);
	}
}
