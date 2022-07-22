package sc.fiji.hdf5;

import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.base.mdarray.MDIntArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ij.IJ;
import ij.VirtualStack;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import org.apache.commons.lang.NotImplementedException;

import java.util.Arrays;

public class HDF5VirtualStack extends VirtualStack
{
  private IHDF5Reader reader;
  private String dsetName;
  private int nRows, nCols, nLevels;
  private String typeText;
  private boolean isRGB;

  public HDF5VirtualStack(IHDF5Reader reader, String dsetName, int nRows, int nCols, int nLevels, boolean isRGB, String typeText)
  {
    this.reader = reader;
    this.dsetName = dsetName;
    this.nRows = nRows;
    this.nCols = nCols;
    this.nLevels = nLevels;
    this.typeText = typeText;
    this.isRGB = isRGB;
  }

  @Override
  public int getSize()
  {
    return this.nLevels;
  }


//	if (typestring.equals("float32"))
//	{
//		MDFloatArray rawdata = reader.float32().readMDArrayBlock(dsetName, new int[]{1, nRows, nCols}, new long[]{n, 0, 0});
//		ip = new FloatProcessor(nCols, nRows);
//
//		System.arraycopy(rawdata.getAsFlatArray(), 0,
//				(float[]) ip.getPixels(), 0,
//				nRows * nCols);
//	}


  @Override
  public ImageProcessor getProcessor(int n)
  {
    n = n - 1; // zero index
    ImageProcessor ip;
    if (typeText.equals("uint8") && !isRGB)
    {
      MDByteArray rawdata = reader.uint8().readMDArrayBlock(dsetName, new int[]{1, nRows, nCols}, new long[]{n, 0, 0});
      ip = new ByteProcessor(nCols, nRows);
      System.arraycopy(rawdata.getAsFlatArray(), 0,
          (byte[]) ip.getPixels(), 0,
          nRows * nCols);
    } else if (typeText.equals("uint8"))
    {  // RGB data
      throw new NotImplementedException("RGB 8 bit pixel images not supported in virtual stacks (yet)"); // I don't have a test file to check behavior on

    } else if (typeText.equals("uint16"))
    {
      MDShortArray rawdata = reader.uint16().readMDArrayBlock(dsetName, new int[]{1, nRows, nCols}, new long[]{n, 0, 0});
      ip = new ShortProcessor(nCols, nRows);
      System.arraycopy(rawdata.getAsFlatArray(), 0,
          (short[]) ip.getPixels(), 0,
          nRows * nCols);
    } else if (typeText.equals("int16"))
    {
      MDShortArray rawdata = reader.int16().readMDArrayBlock(dsetName, new int[]{1, nRows, nCols}, new long[]{n, 0, 0});
      ip = new ShortProcessor(nCols, nRows);
      System.arraycopy(rawdata.getAsFlatArray(), 0,
          (short[]) ip.getPixels(), 0,
          nRows * nCols);
    } else if (typeText.equals("float32") || typeText.equals("float64"))
    {
      MDFloatArray rawdata = reader.float32().readMDArrayBlock(dsetName, new int[]{1, nRows, nCols}, new long[]{n, 0, 0});
      ip = new FloatProcessor(nCols, nRows);
      System.arraycopy(rawdata.getAsFlatArray(), 0,
          (float[]) ip.getPixels(), 0,
          nRows * nCols);
    } else if (typeText.equals("int32"))
    {
      int[] rawdata_ints = reader.int32().readMDArrayBlock(dsetName, new int[]{1, nRows, nCols}, new long[]{n, 0, 0}).getAsFlatArray();
      float[] rawdata = new float[rawdata_ints.length];
      for (int i = 0; i < rawdata_ints.length; i++)
      {
        rawdata[i] = (float) rawdata_ints[i];
      }
      ip = new FloatProcessor(nCols, nRows);
      System.arraycopy(rawdata, 0,
          (float[]) ip.getPixels(), 0,
          nRows * nCols);
    } else
    {
      throw new NotImplementedException("Type " + typeText + " not supported by virtual stacks yet");
    }
    return ip;
  }
}
