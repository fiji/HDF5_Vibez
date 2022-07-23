package sc.fiji.hdf5;

import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import org.apache.commons.lang.NotImplementedException;

public class CustomLayoutHDF5VirtualStack extends HDF5VirtualStack
{
  private int levelToLevelOffset;
  private int rowToRowOffset;
  private int colToColOffset;

  public CustomLayoutHDF5VirtualStack(IHDF5Reader reader, String dsetName, int nRows, int nCols, int nLevels, boolean isRGB, String typeText, int levelToLevelOffset, int rowToRowOffset, int colToColOffset)
  {
    super(reader, dsetName, nRows, nCols, nLevels, isRGB, typeText);
    this.levelToLevelOffset = levelToLevelOffset;
    this.rowToRowOffset = rowToRowOffset;
    this.colToColOffset = colToColOffset;
  }


  @Override
  public ImageProcessor getProcessor(int n)
  {
    ImageProcessor ip = null;
    n = n - 1; // zero index n
    if (typeText.equals("uint8"))
    {
      byte[] rawdata = reader.uint8().readMDArray(dsetName).getAsFlatArray();

      ip = new ByteProcessor(nCols, nRows);
      for (int row = 0; row < nRows; ++row)
      {
        byte[] trgData = (byte[]) ip.getPixels();
        int trgOffset = row * nCols;
        int srcOffset =
            n * levelToLevelOffset + row * rowToRowOffset;
        for (int col = 0; col < nCols; ++col)
        {
          trgData[trgOffset] = rawdata[srcOffset];
          ++trgOffset;
          srcOffset += colToColOffset;
        }
      }

    } else if (typeText.equals("uint16"))
    {
      short[] rawdata = reader.uint16().readMDArray(dsetName).getAsFlatArray();

      ip = new ShortProcessor(nCols, nRows);
      for (int row = 0; row < nRows; ++row)
      {
        short[] trgData = (short[]) ip.getPixels();
        int trgOffset = row * nCols;
        int srcOffset =
            n * levelToLevelOffset + row * rowToRowOffset;
        for (int col = 0; col < nCols; ++col)
        {
          trgData[trgOffset] = rawdata[srcOffset];
          ++trgOffset;
          srcOffset += colToColOffset;
        }
      }


    } else if (typeText.equals("int16"))
    {
      short[] rawdata = reader.int16().readMDArray(dsetName).getAsFlatArray();

      ip = new ShortProcessor(nCols, nRows);
      for (int row = 0; row < nRows; ++row)
      {
        short[] trgData = (short[]) ip.getPixels();
        int trgOffset = row * nCols;
        int srcOffset =
            n * levelToLevelOffset + row * rowToRowOffset;
        for (int col = 0; col < nCols; ++col)
        {
          trgData[trgOffset] = rawdata[srcOffset];
          ++trgOffset;
          srcOffset += colToColOffset;
        }
      }


    } else if (typeText.equals("float32") || typeText.equals("float64"))
    {
      float[] rawdata = reader.float32().readMDArray(dsetName).getAsFlatArray();

      ip = new FloatProcessor(nCols, nRows);
      for (int row = 0; row < nRows; ++row)
      {
        float[] trgData = (float[]) ip.getPixels();
        int trgOffset = row * nCols;
        int srcOffset =
            n * levelToLevelOffset + row * rowToRowOffset;
        for (int col = 0; col < nCols; ++col)
        {
          trgData[trgOffset] = rawdata[srcOffset];
          ++trgOffset;
          srcOffset += colToColOffset;
        }
      }

    } else
    {
      throw new NotImplementedException("Type " + typeText + " not supported yet");
    }
    return ip;
  }
}
