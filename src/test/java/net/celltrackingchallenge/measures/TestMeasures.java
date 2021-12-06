package net.celltrackingchallenge.measures;

import org.scijava.Context;
import org.scijava.log.LogService;
import java.io.IOException;
import java.util.TreeSet;

public class TestMeasures
{
	final LogService logService;
	//
	public TestMeasures()
	{
		final Context ctx = new Context(LogService.class);
		logService = ctx.getService(LogService.class);
	}


	public void calc_SEG()
	{
		try {
			logService.setLevel(0);
			//a demo how to silence from any logging,
			//btw, the reporting below does not make sense with this setLevel
			//and all could be set to false

			final SEG seg = new SEG(logService);
			seg.doLogReports = true;          //reports scores associated with every GT label
			seg.doAllResReports = true;       //reports GT label to which every RES label got matched to
			seg.doStopOnEmptyImages = false;  //optional extra (sanity) test

			//either set to limit time points over which SEG is computed;
			//or leave unset to make the SEG take all files from the input folder
			seg.doOnlyTheseTimepoints = new TreeSet<>();
			seg.doOnlyTheseTimepoints.add(2);
			//seg.doOnlyTheseTimepoints.add(5);

			double segValue = seg.calculate("/temp/test/gt","/temp/test/res_forSEG");
			System.out.println("Got SEG value = "+segValue);
		} catch (IOException e) {
			System.out.println("SEG Error: "+e.getMessage());
			e.printStackTrace();
		}
	}


	public void calc_TRAandDET()
	{
		try {
			logService.setLevel(0);
			//a demo how to silence from any logging,
			//btw, the reporting below does not make sense with this setLevel
			//and all could be set to false

			final TRA tra = new TRA(logService);
			tra.doLogReports = true;          //reports scores associated with every GT label and link
			tra.doMatchingReports = true;     //reports GT label to which every RES label got matched to

			//TRA always takes all files from the input folder

			double traValue = tra.calculate("/temp/test/gt","/temp/test/res_forTRA");
			System.out.println("Got TRA value = "+traValue);

			final DET det = new DET(logService);
			//NB: the 3rd param enables to re-use the most from the previous TRA call,
			//    for example the image data is not loaded again
			double detValue = det.calculate("/temp/test/gt","/temp/test/res_forTRA", tra.getCache());
			System.out.println("Got DET value = "+detValue);
		} catch (IOException e) {
			System.out.println("TRA Error: "+e.getMessage());
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {
		final TestMeasures tst = new TestMeasures();
		//tst.calc_SEG();
		tst.calc_TRAandDET();
	}
}
