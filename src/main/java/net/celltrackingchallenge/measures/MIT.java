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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import net.celltrackingchallenge.measures.TrackDataCache.Track;

public class MIT extends AbstractDSmeasure
{
	///a constructor requiring connection to Fiji report/log services
	public MIT(final Logger _log)
	{ super(_log); }

	private double mit = -1.0;


	//---------------------------------------------------------------------/
	/// This is upper part of the MIT calculator.
	@Override
	protected void calculateUpperStage(final String imgPath, final double[] resolution,
	                                 final String annPath,
	                                 final ImgQualityDataCache _cache)
	throws IOException
	{
		//classes with helpful data structures and functions
		final TrackDataCache tcache = new TrackDataCache(log);

		//iterate over videos and call this.getMITforVideo() for every video
		mit = 0.0;
		int cnt = 0;

		//single or multiple video situation?
		if (Files.isReadable(
			new File(String.format("%s/01_GT/TRA/man_track.txt",annPath)).toPath()))
		{
			//multiple video situation: paths point on a dataset
			int video = 1;
			while (Files.isReadable(
				new File(String.format("%s/%02d_GT/TRA/man_track.txt",annPath,video)).toPath()))
			{
				//load our own working data
				tcache.gt_tracks.clear();
				tcache.LoadTrackFile(
					String.format("%s/%02d_GT/TRA/man_track.txt",annPath,video), tcache.gt_tracks);

				if (tcache.gt_tracks.size() == 0)
					throw new IllegalArgumentException("No reference (GT) track was found!");

				final double l_mit = getMITforVideo(tcache);
				log.info("MIT for video "+video+": "+l_mit);

				mit += l_mit;
				++cnt;

				++video;
			}
		}
		else
		{
			//single video situation
			//load our own working data
			tcache.LoadTrackFile(annPath+"/TRA/man_track.txt", tcache.gt_tracks);
			if (tcache.gt_tracks.size() == 0)
				throw new IllegalArgumentException("No reference (GT) track was found!");

			mit = getMITforVideo(tcache);
			cnt = 1;
		}

		if (cnt > 0)
		{
			mit /= (double)cnt;
			log.info("MIT for dataset: "+mit);
		}
		else
			log.info("MIT for dataset: Couldn't calculate average MIT because there are missing labels.");
	}

	private double getMITforVideo(final TrackDataCache tcache)
	{
		//do the bottom stage
		//DEBUG//log.info("Computing the MIT completely...");

		//to detected span of time points: this is believed to give the length of the
		//underlying video provided the video does not begin and/or end with empty frames...
		int minTime = Integer.MAX_VALUE;
		int maxTime = Integer.MIN_VALUE;

		//detect the span
		for (Track t : tcache.gt_tracks.values())
		{
			minTime = Math.min(minTime, t.m_begin);
			maxTime = Math.max(maxTime, t.m_end);
		}
		//NB: we do not mind negative time points (can happen only with minTime)

		//check to prevent from having negative length of the video
		if (minTime > maxTime)
			throw new IllegalArgumentException("Reference (GT) tracks have wrong (negative) time span.");

		//calculate the average number of dividing cells per frame, which would be
		//to accumulate numbers of divisions happening in every frame and divide by
		//video length -- but the accumulation amounts to the number of all division
		//across the video
		tcache.DetectForks(tcache.gt_tracks, tcache.gt_forks);

		//log.info("MIT_debug: span="+(maxTime-minTime+1)+", forks cnt="+tcache.gt_forks.size());
		return ( (double)tcache.gt_forks.size() / (double)(maxTime - minTime +1) );
	}

	/// This is bottom part of the MIT calculator.
	@Override
	protected double calculateBottomStage()
	{ return (mit); }
}
