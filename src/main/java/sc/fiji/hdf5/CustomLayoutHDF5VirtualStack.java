package sc.fiji.hdf5;

import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ij.process.ImageProcessor;

/**
 * A virtual stack for datasets whose axes are in an arbitrary order, described by a layout
 * string of 'x', 'y' and 'z' (one character per dataset dimension, slowest first). Only the
 * block for the requested z slice is read.
 */
public class CustomLayoutHDF5VirtualStack extends HDF5VirtualStack
{
  private final String layout;
  private final int[] extent;

  public CustomLayoutHDF5VirtualStack(IHDF5Reader reader, String dsetName, int nRows, int nCols, int nLevels, String typeText, String layout)
  {
    super(reader, dsetName, nRows, nCols, nLevels, false, typeText);
    this.layout = layout;
    final long[] dims = reader.object().getDataSetInformation(dsetName).getDimensions();
    this.extent = new int[dims.length];
    for (int d = 0; d < dims.length; d++)
      extent[d] = (int) dims[d];
  }

  @Override
  public ImageProcessor getProcessor(int n)
  {
    n = n - 1; // zero index
    final int rank = extent.length;

    // read the whole slice: every dimension in full, except z, of which only plane n
    final int[] blockDims = extent.clone();
    final long[] offset = new long[rank];
    for (int d = 0; d < rank; d++)
    {
      if (layout.charAt(d) == 'z')
      {
        blockDims[d] = 1;
        offset[d] = n;
      }
    }

    // strides of the block in the flat array
    final int[] stride = new int[rank];
    stride[rank - 1] = 1;
    for (int d = rank - 2; d >= 0; --d)
      stride[d] = blockDims[d + 1] * stride[d + 1];

    int rowStride = 0, colStride = 0;
    for (int d = 0; d < rank; d++)
    {
      if (layout.charAt(d) == 'y') rowStride = stride[d];
      else if (layout.charAt(d) == 'x') colStride = stride[d];
    }
    return toProcessor(readBlock(blockDims, offset), rowStride, colStride);
  }
}
