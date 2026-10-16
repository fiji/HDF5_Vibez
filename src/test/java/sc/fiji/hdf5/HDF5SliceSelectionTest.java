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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import ij.ImagePlus;

/** Loading some of the slices of a stack. */
public class HDF5SliceSelectionTest
{
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private static final int Z = 10, Y = 3, X = 4;

  private String file() { return new File(tmp.getRoot(), "slices.h5").getAbsolutePath(); }

  /** Slice z (1-based) is filled with the value 10 * z, plus the pixel index. */
  private String writeStack()
  {
    final short[] data = new short[Z * Y * X];
    for (int z = 0; z < Z; z++)
      for (int i = 0; i < Y * X; i++)
        data[z * Y * X + i] = (short) (10 * (z + 1) + i * 1000);
    final IHDF5Writer w = HDF5Factory.open(file());
    w.uint16().writeMDArray("/d", new MDShortArray(data, new long[] {Z, Y, X}));
    w.float32().setArrayAttr("/d", "element_size_um", new float[] {2f, 0.5f, 0.25f});
    w.close();
    return file();
  }

  private static short firstPixel(final ImagePlus imp, final int slice)
  {
    return ((short[]) imp.getStack().getPixels(slice))[0];
  }

  @Test
  public void singleSlice()
  {
    final ImagePlus imp = HDF5ImageJ.hdf5readSlices(writeStack(), "/d", "4");
    assertNotNull(imp);
    assertEquals(1, imp.getStackSize());
    assertEquals(X, imp.getWidth());
    assertEquals(Y, imp.getHeight());
    assertEquals(ImagePlus.GRAY16, imp.getType());
    assertEquals(40, firstPixel(imp, 1));
  }

  @Test
  public void range()
  {
    final ImagePlus imp = HDF5ImageJ.hdf5readSlices(writeStack(), "/d", "3-5");
    assertEquals(3, imp.getStackSize());
    assertEquals(30, firstPixel(imp, 1));
    assertEquals(40, firstPixel(imp, 2));
    assertEquals(50, firstPixel(imp, 3));
  }

  @Test
  public void everyNthStartsAtTheFirst()
  {
    final ImagePlus imp = HDF5ImageJ.hdf5readSlices(writeStack(), "/d", "%3");
    assertEquals(4, imp.getStackSize()); // 1, 4, 7, 10
    assertEquals(10, firstPixel(imp, 1));
    assertEquals(40, firstPixel(imp, 2));
    assertEquals(70, firstPixel(imp, 3));
    assertEquals(100, firstPixel(imp, 4));
  }

  @Test
  public void allPixelsOfASelectedSliceAreRead()
  {
    final ImagePlus imp = HDF5ImageJ.hdf5readSlices(writeStack(), "/d", "2");
    final short[] expected = new short[Y * X];
    for (int i = 0; i < expected.length; i++) expected[i] = (short) (20 + i * 1000);
    assertArrayEquals(expected, (short[]) imp.getStack().getPixels(1));
  }

  @Test
  public void listKeepsTheOrderGiven()
  {
    final ImagePlus imp = HDF5ImageJ.hdf5readSlices(writeStack(), "/d", "7,2");
    assertEquals(2, imp.getStackSize());
    assertEquals(70, firstPixel(imp, 1));
    assertEquals(20, firstPixel(imp, 2));
  }

  @Test
  public void emptyLoadsEverything()
  {
    final ImagePlus imp = HDF5ImageJ.hdf5readSlices(writeStack(), "/d", "");
    assertEquals(Z, imp.getStackSize());
  }

  @Test
  public void evenlySpacedSlicesScaleTheZSpacing()
  {
    final String f = writeStack();
    final ImagePlus every3 = HDF5ImageJ.hdf5readSlices(f, "/d", "%3");
    assertEquals(6.0, every3.getCalibration().pixelDepth, 1e-6);
    assertEquals(0.5, every3.getCalibration().pixelHeight, 1e-6);
    assertEquals(0.25, every3.getCalibration().pixelWidth, 1e-6);
    // a contiguous range keeps the spacing, and uneven slices cannot be scaled
    assertEquals(2.0, HDF5ImageJ.hdf5readSlices(f, "/d", "3-6").getCalibration().pixelDepth, 1e-6);
    assertEquals(2.0, HDF5ImageJ.hdf5readSlices(f, "/d", "1,2,9").getCalibration().pixelDepth, 1e-6);
  }

  @Test
  public void titleNamesTheSlices()
  {
    final ImagePlus imp = HDF5ImageJ.hdf5readSlices(writeStack(), "/d", " 2-4 ");
    assertTrue(imp.getTitle(), imp.getTitle().endsWith("[2-4]"));
  }

  @Test
  public void badSpecsGiveNull()
  {
    final String f = writeStack();
    assertNull(HDF5ImageJ.hdf5readSlices(f, "/d", "abc"));
    assertNull(HDF5ImageJ.hdf5readSlices(f, "/d", "11"));
    assertNull(HDF5ImageJ.hdf5readSlices(f, "/d", "5-3"));
  }

  @Test
  public void customLayout()
  {
    // an x, y, z ordered dataset: slice z of "xyz" holds elements [:, :, z]
    final int x = 4, y = 3, z = 5;
    final byte[] data = new byte[x * y * z];
    for (int xi = 0; xi < x; xi++)
      for (int yi = 0; yi < y; yi++)
        for (int zi = 0; zi < z; zi++)
          data[(xi * y + yi) * z + zi] = (byte) (zi * 20 + yi * 4 + xi);
    final IHDF5Writer w = HDF5Factory.open(file());
    w.uint8().writeMDArray("/d", new MDByteArray(data, new long[] {x, y, z}));
    w.close();

    final ImagePlus imp = HDF5ImageJ.hdf5readSlices(file(), "/d", "xyz", "2,4");
    assertNotNull(imp);
    assertEquals(2, imp.getStackSize());
    assertEquals(x, imp.getWidth());
    assertEquals(y, imp.getHeight());
    for (int s = 0; s < 2; s++)
    {
      final int zi = s == 0 ? 1 : 3;
      final byte[] pixels = (byte[]) imp.getStack().getPixels(s + 1);
      for (int yi = 0; yi < y; yi++)
        for (int xi = 0; xi < x; xi++)
          assertEquals("z=" + zi + " y=" + yi + " x=" + xi, zi * 20 + yi * 4 + xi, pixels[yi * x + xi]);
    }
  }

  @Test
  public void rgbSlices()
  {
    final int n = 4, y = 3, x = 2;
    final byte[] data = new byte[n * y * x * 3];
    for (int i = 0; i < data.length; i++) data[i] = (byte) (i + 1);
    final IHDF5Writer w = HDF5Factory.open(file());
    w.uint8().writeMDArray("/d", new MDByteArray(data, new long[] {n, y, x, 3}));
    w.close();
    final ImagePlus imp = HDF5ImageJ.hdf5readSlices(file(), "/d", "3-4");
    assertEquals(ImagePlus.COLOR_RGB, imp.getType());
    assertEquals(2, imp.getStackSize());
    final int plane = y * x * 3; // slice 3 starts at element 2 * plane
    final int first = ((int[]) imp.getStack().getPixels(1))[0] & 0xffffff;
    assertEquals(((2 * plane + 1) << 16) | ((2 * plane + 2) << 8) | (2 * plane + 3), first);
  }

  /** The point of it: sample a dataset that cannot be loaded in full. */
  @Test
  public void sampleADatasetTooBigForMemory()
  {
    final int planes = 1 << 20; // 2^20 planes of 64 x 64 = 2^32 elements
    final IHDF5Writer w = HDF5Factory.open(file());
    w.uint8().createMDArray("/big", new long[] {planes, 64, 64}, new int[] {1, 64, 64});
    for (final int z : new int[] {0, planes / 2, planes - 1})
    {
      final byte[] block = new byte[64 * 64];
      java.util.Arrays.fill(block, (byte) (z == 0 ? 1 : z == planes - 1 ? 3 : 2));
      w.uint8().writeMDArrayBlockWithOffset("/big", new MDByteArray(block, new long[] {1, 64, 64}),
          new long[] {z, 0, 0});
    }
    w.close();

    assertNull("all of it cannot be loaded", HDF5ImageJ.hdf5read(file(), "/big"));
    final ImagePlus imp = HDF5ImageJ.hdf5readSlices(file(), "/big", "1," + (planes / 2 + 1) + "," + planes);
    assertNotNull(imp);
    assertEquals(3, imp.getStackSize());
    assertEquals(1, ((byte[]) imp.getStack().getPixels(1))[0]);
    assertEquals(2, ((byte[]) imp.getStack().getPixels(2))[0]);
    assertEquals(3, ((byte[]) imp.getStack().getPixels(3))[0]);
  }
}
