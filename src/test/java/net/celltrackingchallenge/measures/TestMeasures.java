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
			//uncomment the following to silence the measures completely
			//...in which case the enabling of the reporting below becomes rather pointless... ;-)
			//logService.setLevel(0);

			final SEG seg = new SEG(logService);
			seg.doLogReports = true;          //reports scores associated with every GT label
			seg.doAllResReports = true;       //reports GT label to which every RES label got matched to
			seg.doStopOnEmptyImages = false;  //optional extra (sanity) test

			//either set to limit time points over which SEG is computed;
			//or leave unset to make the SEG take all files from the input folder
			seg.doOnlyTheseTimepoints = new TreeSet<>();
			seg.doOnlyTheseTimepoints.add(2);
			seg.doOnlyTheseTimepoints.add(5);

			double segValue = seg.calculate(folder_GT, folder_result);
			System.out.println("Got SEG value = "+segValue);
		} catch (IOException e) {
			System.out.println("SEG error: "+e.getMessage());
			e.printStackTrace();
		}
	}


	public void calc_TRAandDET()
	{
		try {
			//uncomment the following to silence the measures completely
			//...in which case the enabling of the reporting below becomes rather pointless... ;-)
			//logService.setLevel(0);

			final TRA tra = new TRA(logService);
			tra.doLogReports = true;          //reports scores associated with every GT label and link
			tra.doMatchingReports = true;     //reports GT label to which every RES label got matched to

			//TRA always takes all files from the input folder

			double traValue = tra.calculate(folder_GT, folder_result);
			System.out.println("Got TRA value = "+traValue);

			final DET det = new DET(logService);
			//NB: the 3rd param enables to re-use the most from the previous TRA call,
			//    for example the image data is not loaded again
			double detValue = det.calculate(folder_GT, folder_result, tra.getCache());
			System.out.println("Got DET value = "+detValue);
		} catch (IOException e) {
			System.out.println("TRA or DET error: "+e.getMessage());
			e.printStackTrace();
		}
	}


	public void calc_everything()
	{
		try {
			//silence...
			final int prevLogLevel = logService.getLevel();
			//logService.setLevel(0);

			final SEG seg = new SEG(logService);
			final TRA tra = new TRA(logService);
			final DET det = new DET(logService);
			//
			final CT  ct  = new CT(logService);
			final TF  tf  = new TF(logService);
			final CCA cca = new CCA(logService);
			final BCi bci = new BCi(logService);
			bci.setI(2);

			double segValue = seg.calculate(folder_GT, folder_result);
			double traValue = tra.calculate(folder_GT, folder_result);
			final TrackDataCache sharedCache = tra.getCache();
			double detValue = det.calculate(folder_GT, folder_result, sharedCache);
			double ctValue = ct.calculate(folder_GT, folder_result, sharedCache);
			double tfValue = tf.calculate(folder_GT, folder_result, sharedCache);
			//throws exception on the test data: "GT tracking data show no complete cell cycle!"
			//note: BCi also does not make too much sense on the test data from this repo
			//double ccaValue = cca.calculate(folder_GT, folder_result, sharedCache);
			double ccaValue = -1.0;
			double bciValue = bci.calculate(folder_GT, folder_result, sharedCache);

			//restore previous verbosity
			logService.setLevel(prevLogLevel);

			System.out.println("SEG: "+segValue
					+"\nTRA: "+traValue
					+"\nDET: "+detValue
					+"\nCT: "+ctValue
					+"\nTF: "+tfValue
					+"\nCCA: "+ccaValue
					+"\nBCi: "+bciValue);
		} catch (IOException e) {
			System.out.println("Measures error: "+e.getMessage());
			e.printStackTrace();
		}
	}


	final String folder_GT = "/temp/test/GT";
	final String folder_result = "/temp/test/res";

	public static void main(String[] args) {
		final TestMeasures tst = new TestMeasures();
		//tst.calc_SEG();
		//tst.calc_TRAandDET();
		tst.calc_everything();
	}
}
