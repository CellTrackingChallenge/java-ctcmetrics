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

public class SNR extends AbstractDSmeasure
{
	///a constructor requiring connection to Fiji report/log services
	public SNR(final Logger _log)
	{ super(_log); }


	//---------------------------------------------------------------------/
	/// This is the main SNR calculator.
	@Override
	protected double calculateBottomStage()
	{
		//do the bottom stage
		//DEBUG//log.info("Computing the SNR bottom part...");
		double snr = 0.0;
		long videoCnt = 0; //how many videos were processed

		//go over all encountered videos and calc
		//their respective avg. SNRs and average them
		for (ImgQualityDataCache.videoDataContainer data : cache.cachedVideoData)
		{
			//shadows of the/short-cuts to the cache data
			final Vector<HashMap<Integer,Double>> avgFG = data.avgFG;
			final Vector<Double> avgBG = data.avgBG;
			final Vector<Double> stdBG = data.stdBG;

			//go over all FG objects and calc their SNRs
			long noFGs = 0;
			double l_snr = 0.; //local snr

			//over all time points
			for (int time=0; time < avgFG.size(); ++time)
			{
				//skip this frame if we cannot compute anything on it
				if (stdBG.get(time) == 0.0) continue;

				//over all objects, in fact use their avg intensities
				for (Map.Entry<Integer,Double> aCellAndItsParam : avgFG.get(time).entrySet())
				{
					final double one_snr = Math.abs(aCellAndItsParam.getValue() - avgBG.get(time)) / stdBG.get(time);
					l_snr += one_snr;
					data.getTableRowFor(time, aCellAndItsParam.getKey()).snr = one_snr;
					++noFGs;
				}
			}

			//finish the calculation of the local average SNR
			if (noFGs > 0)
			{
				l_snr /= (double)noFGs;
				log.info("SNR for video "+data.video+": "+l_snr);

				snr += l_snr;
				++videoCnt;
			}
			else
				log.info("SNR for video "+data.video+": Couldn't calculate average SNR because there are missing labels.");
		}

		//summarize over all datasets:
		if (videoCnt > 0)
		{
			snr /= (double)videoCnt;
			log.info("SNR for dataset: "+snr);
		}
		else
			log.info("SNR for dataset: Couldn't calculate average SNR because there are missing labels.");

		return (snr);
	}
}
