package sc.fiji.hdf5;

import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5ReaderConfigurator;
import ij.IJ;
import ij.VirtualStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * A stack that reads one slice at a time from an HDF5 dataset, so datasets that are too large to
 * hold in memory (or in a single Java array) can be browsed. The dataset is {@code [z, y, x]},
 * or {@code [y, x]} for a single plane; RGB data has an extra trailing dimension of 3.
 *
 * <p>The file is opened for each read and closed again, so no file handle is held for the life
 * of the image (which would keep the file locked on Windows).</p>
 */
public class HDF5VirtualStack extends VirtualStack
{
  protected final String filename;
  protected String dsetName;
  protected int nRows, nCols, nLevels;
  protected String typeText;
  protected boolean isRGB;
  private final int rank;
  private boolean warnedPrecision = false;

  public HDF5VirtualStack(String filename, String dsetName, int nRows, int nCols, int nLevels, boolean isRGB, String typeText)
  {
    this.filename = filename;
    this.dsetName = dsetName;
    this.nRows = nRows;
    this.nCols = nCols;
    this.nLevels = nLevels;
    this.typeText = typeText;
    this.isRGB = isRGB;
    final IHDF5Reader reader = openReader();
    try
    {
      this.rank = reader.object().getDataSetInformation(dsetName).getRank();
    }
    finally
    {
      reader.close();
    }
  }

  /** Opens the file for reading, converting numeric types as the in-memory loaders do. */
  protected IHDF5Reader openReader()
  {
    final IHDF5ReaderConfigurator conf = HDF5Factory.configureForReading(filename);
    conf.performNumericConversions();
    return conf.reader();
  }

  @Override
  public int getSize()
  {
    return this.nLevels;
  }

  @Override
  public ImageProcessor getProcessor(int n)
  {
    n = n - 1; // zero index
    if (isRGB)
    {
      // [z, y, x, 3], or [y, x, 3] for a single plane
      final int[] rgbDims = rank == 3 ? new int[] {nRows, nCols, 3} : new int[] {1, nRows, nCols, 3};
      final long[] rgbOffset = rank == 3 ? new long[] {0, 0, 0} : new long[] {n, 0, 0, 0};
      return toColorProcessor((byte[]) readBlock(rgbDims, rgbOffset));
    }
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
    final IHDF5Reader reader = openReader();
    try
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
        if (!warnedPrecision && HDF5ImageJ.int32LosesPrecision(ints))
        {
          warnedPrecision = true;
          IJ.log("Warning: '" + dsetName + "' has int32 values beyond +-2^24, which cannot be represented exactly as 32-bit floats");
        }
        return HDF5ImageJ.int32ToFloat(ints);
      }
    }
    finally
    {
      reader.close();
    }
    throw new UnsupportedOperationException("Type " + typeText + " not supported by virtual stacks yet");
  }

  /** Packs interleaved RGB bytes ({@code r, g, b, r, g, b, ...}) into a color processor. */
  private ImageProcessor toColorProcessor(final byte[] rgb)
  {
    final ColorProcessor cp = new ColorProcessor(nCols, nRows);
    final int[] pixels = (int[]) cp.getPixels();
    for (int i = 0; i < pixels.length; i++)
      pixels[i] = ((rgb[3 * i] & 0xff) << 16) | ((rgb[3 * i + 1] & 0xff) << 8) | (rgb[3 * i + 2] & 0xff);
    return cp;
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
