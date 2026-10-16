package sc.fiji.hdf5;

import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ij.VirtualStack;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * A stack that reads one slice at a time from an HDF5 dataset, so datasets that are too large to
 * hold in memory (or in a single Java array) can be browsed. The dataset is {@code [z, y, x]},
 * or {@code [y, x]} for a single plane.
 */
public class HDF5VirtualStack extends VirtualStack
{
  protected IHDF5Reader reader;
  protected String dsetName;
  protected int nRows, nCols, nLevels;
  protected String typeText;
  protected boolean isRGB;
  private final int rank;

  public HDF5VirtualStack(IHDF5Reader reader, String dsetName, int nRows, int nCols, int nLevels, boolean isRGB, String typeText)
  {
    this.reader = reader;
    this.dsetName = dsetName;
    this.nRows = nRows;
    this.nCols = nCols;
    this.nLevels = nLevels;
    this.typeText = typeText;
    this.isRGB = isRGB;
    this.rank = reader.object().getDataSetInformation(dsetName).getRank();
  }

  @Override
  public int getSize()
  {
    return this.nLevels;
  }

  @Override
  public ImageProcessor getProcessor(int n)
  {
    if (isRGB)
      throw new UnsupportedOperationException("RGB 8 bit pixel images not supported in virtual stacks (yet)");
    n = n - 1; // zero index
    final int[] blockDims = rank == 2 ? new int[] {nRows, nCols} : new int[] {1, nRows, nCols};
    final long[] offset = rank == 2 ? new long[] {0, 0} : new long[] {n, 0, 0};
    return toProcessor(readBlock(blockDims, offset), nCols, 1);
  }

  /**
   * Reads a block of the dataset as a flat array: {@code byte[]}, {@code short[]} or
   * {@code float[]} (int32 is converted to float, as when loading into memory).
   */
  protected Object readBlock(final int[] blockDims, final long[] offset)
  {
    if (typeText.equals("uint8"))
      return reader.uint8().readMDArrayBlock(dsetName, blockDims, offset).getAsFlatArray();
    else if (typeText.equals("uint16"))
      return reader.uint16().readMDArrayBlock(dsetName, blockDims, offset).getAsFlatArray();
    else if (typeText.equals("int16"))
      return reader.int16().readMDArrayBlock(dsetName, blockDims, offset).getAsFlatArray();
    else if (typeText.equals("float32") || typeText.equals("float64"))
      return reader.float32().readMDArrayBlock(dsetName, blockDims, offset).getAsFlatArray();
    else if (typeText.equals("int32"))
    {
      final int[] ints = reader.int32().readMDArrayBlock(dsetName, blockDims, offset).getAsFlatArray();
      final float[] floats = new float[ints.length];
      for (int i = 0; i < ints.length; i++)
        floats[i] = ints[i];
      return floats;
    }
    throw new UnsupportedOperationException("Type " + typeText + " not supported by virtual stacks yet");
  }

  /**
   * Copies a flat block into a new processor. Pixel (row, col) is read from
   * {@code flat[row * rowStride + col * colStride]}.
   */
  protected ImageProcessor toProcessor(final Object flat, final int rowStride, final int colStride)
  {
    final ImageProcessor ip;
    if (flat instanceof byte[])
      ip = new ByteProcessor(nCols, nRows);
    else if (flat instanceof short[])
      ip = new ShortProcessor(nCols, nRows);
    else
      ip = new FloatProcessor(nCols, nRows);
    final Object pixels = ip.getPixels();

    if (rowStride == nCols && colStride == 1)
    {
      System.arraycopy(flat, 0, pixels, 0, nRows * nCols);
      return ip;
    }
    for (int row = 0; row < nRows; ++row)
    {
      int src = row * rowStride;
      int dst = row * nCols;
      for (int col = 0; col < nCols; ++col)
      {
        if (flat instanceof byte[]) ((byte[]) pixels)[dst] = ((byte[]) flat)[src];
        else if (flat instanceof short[]) ((short[]) pixels)[dst] = ((short[]) flat)[src];
        else ((float[]) pixels)[dst] = ((float[]) flat)[src];
        ++dst;
        src += colStride;
      }
    }
    return ip;
  }
}
