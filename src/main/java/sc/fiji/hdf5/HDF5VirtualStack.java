package sc.fiji.hdf5;

import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ij.IJ;
import ij.VirtualStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.Arrays;

public class HDF5VirtualStack extends VirtualStack {
	private  IHDF5Reader reader;
	private String dsetName;
	private int nRows, nCols, nLevels;

	public HDF5VirtualStack (IHDF5Reader reader, String dsetName, int nRows, int nCols, int nLevels) {
		this.reader = reader;
		this.dsetName = dsetName;
		this.nRows = nRows;
		this.nCols = nCols;
		this.nLevels = nLevels;
	}

	@Override
	public int getSize() {
		return this.nLevels;
	}

	@Override
	public ImageProcessor getProcessor(int n){
		// only floats for now
		MDFloatArray rawdata = reader.float32().readMDArrayBlock(dsetName, new int[]{1, nRows, nCols}, new long[]{n, 0, 0});
		ImageProcessor ip = new FloatProcessor(nCols, nRows);

		System.arraycopy( rawdata.getAsFlatArray(), 0,
						(float[])ip.getPixels(),   0,
						nRows*nCols);
		return ip;
	}
}
