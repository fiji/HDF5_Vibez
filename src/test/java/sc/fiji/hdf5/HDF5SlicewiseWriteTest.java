/*-
 * #%L
 * HDF5 plugin for ImageJ and Fiji.
 * %%
 * Copyright (C) 2011 - 2026 Fiji developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */


package sc.fiji.hdf5;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/** Saving writes one plane at a time, so it needs no copy of the whole stack. */
public class HDF5SlicewiseWriteTest
{
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private String file() { return new File(tmp.getRoot(), "write.h5").getAbsolutePath(); }

  private HDF5DataSetInformation info(final String dset)
  {
    final IHDF5Reader reader = HDF5Factory.openForReading(file());
    try
    {
      return reader.object().getDataSetInformation(dset);
    }
    finally
    {
      reader.close();
    }
  }

  private static ImagePlus stack(final int n, final int w, final int h, final int type)
  {
    final ImageStack stack = new ImageStack(w, h);
    for (int z = 0; z < n; z++)
    {
      final ImageProcessor ip;
      switch (type)
      {
        case ImagePlus.GRAY8: ip = new ByteProcessor(w, h); break;
        case ImagePlus.GRAY16: ip = new ShortProcessor(w, h); break;
        case ImagePlus.GRAY32: ip = new FloatProcessor(w, h); break;
        default: ip = new ColorProcessor(w, h);
      }
      for (int i = 0; i < w * h; i++) ip.set(i, type == ImagePlus.COLOR_RGB ? (z + 1) * 0x010203 + i : z * 100 + i);
      stack.addSlice(ip);
    }
    return new ImagePlus("s", stack);
  }

  @Test
  public void eachPlaneIsAChunk()
  {
    HDF5ImageJ.hdf5write(stack(5, 6, 4, ImagePlus.GRAY16), file(), "/d");
    final int[] chunks = info("/d").tryGetChunkSizes() == null ? null : info("/d").tryGetChunkSizes();
    assertNotNull("the dataset is chunked", chunks);
    assertArrayEquals(new int[] {1, 4, 6}, chunks);
    assertArrayEquals(new long[] {5, 4, 6}, info("/d").getDimensions());
  }

  @Test
  public void rgbChunksKeepTheColorDimension()
  {
    HDF5ImageJ.hdf5write(stack(3, 6, 4, ImagePlus.COLOR_RGB), file(), "/d");
    assertArrayEquals(new long[] {3, 4, 6, 3}, info("/d").getDimensions());
    assertArrayEquals(new int[] {1, 4, 6, 3}, info("/d").tryGetChunkSizes());
  }

  @Test
  public void aSinglePlaneIsOneChunk()
  {
    HDF5ImageJ.hdf5write(stack(1, 6, 4, ImagePlus.GRAY8), file(), "/d");
    assertArrayEquals(new long[] {4, 6}, info("/d").getDimensions());
    assertArrayEquals(new int[] {4, 6}, info("/d").tryGetChunkSizes());
  }

  @Test
  public void compressedPlanesReadBack()
  {
    final ImagePlus imp = stack(4, 8, 8, ImagePlus.GRAY32);
    HDF5ImageJ.hdf5write(imp, file(), "/d", "", "", 5);
    final ImagePlus read = HDF5ImageJ.hdf5read(file(), "/d");
    for (int z = 1; z <= 4; z++)
      assertArrayEquals((float[]) imp.getStack().getPixels(z), (float[]) read.getStack().getPixels(z), 0f);
  }

  @Test
  public void writingTheSameNameWhileAppendingReplacesTheDataset()
  {
    HDF5ImageJ.hdf5write(stack(2, 4, 3, ImagePlus.GRAY8), file(), "/d", false);
    final ImagePlus second = stack(3, 5, 2, ImagePlus.GRAY16);
    HDF5ImageJ.hdf5write(second, file(), "/d", false);
    final ArrayList<DataSetInfo> list = HDF5ImageJ.hdf5list(file());
    assertEquals(1, list.size());
    assertEquals("3x2x5", list.get(0).dimText);
    assertEquals("uint16", list.get(0).typeText);
    assertArrayEquals((short[]) second.getStack().getPixels(3),
        (short[]) HDF5ImageJ.hdf5read(file(), "/d").getStack().getPixels(3));
  }

  @Test
  public void aVirtualStackCanBeSaved()
  {
    final String source = new File(tmp.getRoot(), "source.h5").getAbsolutePath();
    final ImagePlus original = stack(4, 7, 5, ImagePlus.GRAY16);
    HDF5ImageJ.hdf5write(original, source, "/d");
    final ImagePlus virtual = HDF5ImageJ.loadDataSetsToVirtualStack(source, new String[] {"/d"}, false);

    HDF5ImageJ.hdf5write(virtual, file(), "/copy");
    final ImagePlus copy = HDF5ImageJ.hdf5read(file(), "/copy");
    assertEquals(4, copy.getStackSize());
    for (int z = 1; z <= 4; z++)
      assertArrayEquals((short[]) original.getStack().getPixels(z), (short[]) copy.getStack().getPixels(z));
  }

  /** A stack whose planes are made up when asked for, so it takes no memory. */
  private static class GeneratedStack extends ImageStack
  {
    private final int size;

    GeneratedStack(final int w, final int h, final int size)
    {
      super(w, h);
      this.size = size;
    }

    @Override public int getSize() { return size; }

    @Override public boolean isVirtual() { return true; }

    @Override public void setPixels(final Object pixels, final int n) { }

    @Override public Object getPixels(final int n)
    {
      final byte[] pixels = new byte[getWidth() * getHeight()];
      java.util.Arrays.fill(pixels, (byte) (n % 251 + 1));
      return pixels;
    }

    @Override public ImageProcessor getProcessor(final int n)
    {
      return new ByteProcessor(getWidth(), getHeight(), (byte[]) getPixels(n), null);
    }

    @Override public String getSliceLabel(final int n) { return null; }
  }

  /** More than 2^31 elements cannot be copied into one Java array, which the old writer needed. */
  @Test
  public void aStackOfMoreThan2Pow31ElementsCanBeSaved()
  {
    final int w = 256, h = 256, planes = (int) (((1L << 31) / (w * h)) + 2);
    assertTrue((long) w * h * planes > Integer.MAX_VALUE);
    final ImagePlus imp = new ImagePlus("generated", new GeneratedStack(w, h, planes));

    HDF5ImageJ.hdf5write(imp, file(), "/big", "", "", 1);

    assertArrayEquals(new long[] {planes, h, w}, info("/big").getDimensions());
    final ImagePlus virtual = HDF5ImageJ.loadDataSetsToVirtualStack(file(), new String[] {"/big"}, false);
    for (final int z : new int[] {1, planes / 2, planes - 1, planes})
      assertEquals("plane " + z, (byte) (z % 251 + 1), ((byte[]) virtual.getStack().getProcessor(z).getPixels())[w * h - 1]);
  }
}
