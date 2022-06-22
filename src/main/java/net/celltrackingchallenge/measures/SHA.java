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

import net.imagej.ops.OpService;
import org.scijava.log.Logger;

import java.util.Vector;
import java.util.HashMap;

public class SHA extends AbstractDSmeasure
{
	///a constructor requiring connection to Fiji report/log services
	public SHA(final Logger _log, final OpService _ops)
	{ super(_log,_ops); }


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
			final Vector<HashMap<Integer,Double>> shaValuesFG = data.shaValuesFG;

			//go over all FG objects and calc their RESs
			long noFGs = 0;
			double l_sha = 0.0;
			//over all time points
			for (int time=0; time < shaValuesFG.size(); ++time)
			{
				//over all objects
				for (double val : shaValuesFG.get(time).values())
				{
					l_sha += val;
					++noFGs;
				}
			}

			//finish the calculation of the average
			if (noFGs > 0)
			{
				l_sha /= (double)noFGs;
				log.info("SHA for video "+data.video+": "+l_sha);

				sha += l_sha;
				++videoCnt;
			}
			else
				log.info("SHA for video "+data.video+": Couldn't calculate average SHA because there are no cells labelled.");
		}

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
