package net.celltrackingchallenge.measures.util;

import net.imagej.mesh.Mesh;
import net.imagej.mesh.Vertices;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.roi.geom.real.DefaultWritablePolygon2D;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.roi.util.RealLocalizableRealPositionable;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.scijava.Context;
import org.scijava.log.LogService;
import sc.fiji.simplifiedio.SimplifiedIO;

import java.util.List;

public class TestShaValues {
	public static void test3D() {
		Context ctx = new Context(LogService.class, OpService.class);

		// ============= INPUT DATA =============
		//
		final Img<UnsignedShortType> img = new ArrayImgFactory<>(new UnsignedShortType()).create(20,20,5);
		RandomAccess<UnsignedShortType> ra = img.randomAccess();

		LoopBuilder.setImages(img).forEachPixel(UnsignedShortType::setZero);
		/*
		ra.setPositionAndGet( 9,10,2).setInteger(200);
		ra.setPositionAndGet(10,10,2).setInteger(200);
		ra.setPositionAndGet( 9,11,2).setInteger(200);
		ra.setPositionAndGet(10,11,2).setInteger(200);
		ra.setPositionAndGet(11,11,2).setInteger(200);
		ra.setPositionAndGet( 9,10,3).setInteger(200);
		ra.setPositionAndGet(10,10,3).setInteger(200);
		*/
		ra.setPositionAndGet(11,10,2).setInteger(200);
		ra.setPositionAndGet(11,10,3).setInteger(200);
		ra.setPositionAndGet( 9,11,3).setInteger(200);
		ra.setPositionAndGet(10,11,3).setInteger(200);
		ra.setPositionAndGet(11,11,3).setInteger(200);
		SimplifiedIO.saveImage(img,"/temp/sha.tif");

		final int fgValue = 200;

		final OpService ops = ctx.getService(OpService.class);
		if (ops == null)
			throw new RuntimeException("foo() is missing the Ops service in its context, sorry.");

		final Img<BitType> boolImg = new ArrayImgFactory(new BitType()).create(img);
		LoopBuilder.setImages(img,boolImg).forEachPixel( (s,t) -> { if (s.getInteger() == fgValue) t.setOne(); else t.setZero(); } );

		Cursor<BitType> c = boolImg.localizingCursor();
		System.out.println("True positions - start:");
		while (c.hasNext()) {
			boolean val = c.next().get();
			if (val) System.out.println("["+c.getIntPosition(0)+","
					+c.getIntPosition(1)+","+c.getIntPosition(2)+"]: "+val);
		}
		System.out.println("True positions - end:");
		Mesh m = ops.geom().marchingCubes(boolImg);
		System.out.println("Created mesh with " + m.vertices().size() + " vertices");
		/*
		 */
		final Vertices mv = m.vertices();
		for (int cnt = 0; cnt < mv.size(); ++cnt) {
			System.out.println(cnt + ": [" + mv.xf(cnt) + "," + mv.yf(cnt) + "," + mv.zf(cnt) + "]");
		}
		System.out.println("      size: "+ops.geom().size(m));
		System.out.println("   surface: "+ops.geom().boundarySize(m));
		System.out.println("sphericity: "+ops.geom().sphericity(m));

		//apply resolution correction
		for (int cnt = 0; cnt < mv.size(); ++cnt) {
			mv.setPositionf(cnt, mv.xf(cnt), mv.yf(cnt), 5*mv.zf(cnt) );
		}
		System.out.println("      size: "+ops.geom().size(m));
		System.out.println("   surface: "+ops.geom().boundarySize(m));
		System.out.println("sphericity: "+ops.geom().sphericity(m));

		double V = ops.geom().size(m).getRealDouble();
		double A = ops.geom().boundarySize(m).getRealDouble();
		double S = Math.pow(36*Math.PI*V*V, 1.0/3.0) / A;
		System.out.println("own result: "+S);

		//volume = ops.geom().size(m)
		//surface = ops.geom().boundarySize(m)

		//apply resolution correction
		for (int cnt = 0; cnt < mv.size(); ++cnt) {
			mv.setPositionf(cnt, 5*mv.xf(cnt), 5*mv.yf(cnt), mv.zf(cnt) );
		}
		System.out.println("      size: "+ops.geom().size(m));
		System.out.println("   surface: "+ops.geom().boundarySize(m));
		System.out.println("sphericity: "+ops.geom().sphericity(m));

		V = ops.geom().size(m).getRealDouble();
		A = ops.geom().boundarySize(m).getRealDouble();
		S = Math.pow(36*Math.PI*V*V, 1.0/3.0) / A;
		System.out.println("own result: "+S);
	}


	public static void test2D() {
		Context ctx = new Context(LogService.class, OpService.class);

		// ============= INPUT DATA =============
		//
		final Img<UnsignedShortType> img = new ArrayImgFactory<>(new UnsignedShortType()).create(20,20);
		RandomAccess<UnsignedShortType> ra = img.randomAccess();

		LoopBuilder.setImages(img).forEachPixel(UnsignedShortType::setZero);
		ra.setPositionAndGet( 8,10).setInteger(200);
		ra.setPositionAndGet( 9,10).setInteger(200);
		ra.setPositionAndGet(10,10).setInteger(200);
		ra.setPositionAndGet(11,10).setInteger(200);
		ra.setPositionAndGet(11,11).setInteger(200);
		SimplifiedIO.saveImage(img,"/temp/sha.tif");

		final int fgValue = 200;
		final double[] resolution = { 1.0, 4.0 };

		final OpService ops = ctx.getService(OpService.class);
		if (ops == null)
			throw new RuntimeException("foo() is missing the Ops service in its context, sorry.");

		final Img<BitType> boolImg = new ArrayImgFactory(new BitType()).create(img);
		LoopBuilder.setImages(img,boolImg).forEachPixel( (s,t) -> { if (s.getInteger() == fgValue) t.setOne(); else t.setZero(); } );

		final Polygon2D p = ops.geom().contour(boolImg, true);
		if (p.numDimensions() != 2)
			throw new RuntimeException("foo() failed extracting 2D polygon, sorry.");

		final DefaultWritablePolygon2D new_p = new DefaultWritablePolygon2D(new double[p.numVertices()], new double[p.numVertices()]);

		//apply resolution correction
		for (int cnt = 0; cnt < p.numVertices(); ++cnt) {
			RealLocalizable v = p.vertex(cnt);
			RealLocalizableRealPositionable nv = new_p.vertex(cnt);
			nv.setPosition(resolution[0]*v.getDoublePosition(0), 0);
			nv.setPosition(resolution[1]*v.getDoublePosition(1), 1);
			System.out.println("Vertex "+cnt+": "+v+" -> "+nv);
		}

		System.out.println("area orig: "+ops.geom().size(p).getRealDouble());
		System.out.println("area  new: "+ops.geom().size(new_p).getRealDouble());
		System.out.println("boundary orig: "+ops.geom().boundarySize(p).getRealDouble());
		System.out.println("boundary  new: "+ops.geom().boundarySize(new_p).getRealDouble());
		System.out.println("circularity orig: "+ops.geom().circularity(p).getRealDouble());
		System.out.println("circularity  new: "+ops.geom().circularity(new_p).getRealDouble());
	}


	public static void main(String[] args) {
		//test3D();
		test2D();
	}
}