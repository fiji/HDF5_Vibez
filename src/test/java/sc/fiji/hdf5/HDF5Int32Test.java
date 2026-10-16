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

import java.io.File;
import java.util.ArrayList;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.systemsx.cisd.base.mdarray.MDIntArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import ij.ImagePlus;
import ij.process.FloatProcessor;

/** int32 datasets are loaded as 32-bit float images (#16). */
public class HDF5Int32Test
{
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private String writeInt32(final String name, final int[] flat, final long... dims)
  {
    final String file = new File(tmp.getRoot(), "int32.h5").getAbsolutePath();
    final IHDF5Writer writer = HDF5Factory.open(file);
    writer.int32().writeMDArray(name, new MDIntArray(flat, dims));
    writer.close();
    return file;
  }

  private static float[] toFloat(final int[] ints)
  {
    final float[] f = new float[ints.length];
    for (int i = 0; i < f.length; i++) f[i] = ints[i];
    return f;
  }

  @Test
  public void bitdepthIs32()
  {
    assertEquals(32, HDF5ImageJ.assignHDF5TypeToImagePlusBitdepth("int32", false));
  }

  @Test
  public void listReportsInt32()
  {
    final String file = writeInt32("/data", new int[2 * 3 * 4], 2, 3, 4);
    final ArrayList<DataSetInfo> list = HDF5ImageJ.hdf5list(file);
    assertEquals(1, list.size());
    assertEquals("int32", list.get(0).typeText);
    assertEquals("2x3x4", list.get(0).dimText);
  }

  @Test
  public void stackLoadsAsFloat()
  {
    final int[] values = new int[2 * 3 * 4];
    for (int i = 0; i < values.length; i++) values[i] = (i - 5) * 100000; // includes negatives
    final String file = writeInt32("/data", values, 2, 3, 4);

    final ImagePlus imp = HDF5ImageJ.hdf5read(file, "/data");
    assertNotNull(imp);
    assertEquals(ImagePlus.GRAY32, imp.getType());
    assertEquals(4, imp.getWidth());
    assertEquals(3, imp.getHeight());
    assertEquals(2, imp.getNSlices());
    final float[] expected = toFloat(values);
    for (int z = 0; z < 2; z++)
    {
      final float[] slice = new float[12];
      System.arraycopy(expected, z * 12, slice, 0, 12);
      assertArrayEquals("slice " + (z + 1), slice, (float[]) imp.getStack().getPixels(z + 1), 0f);
    }
  }

  @Test
  public void singlePlaneLoads()
  {
    final int[] values = {1, -2, 3, -4, 5, -6};
    final String file = writeInt32("/plane", values, 2, 3);
    final ImagePlus imp = HDF5ImageJ.hdf5read(file, "/plane");
    assertNotNull(imp);
    assertEquals(1, imp.getNSlices());
    assertArrayEquals(toFloat(values), (float[]) imp.getProcessor().getPixels(), 0f);
  }

  @Test
  public void customLayoutLoadsInt32()
  {
    final int[] values = new int[2 * 3 * 4];
    for (int i = 0; i < values.length; i++) values[i] = i * 7;
    final String file = writeInt32("/data", values, 2, 3, 4);
    final ImagePlus imp = HDF5ImageJ.hdf5read(file, "/data", "zyx");
    assertNotNull(imp);
    assertEquals(ImagePlus.GRAY32, imp.getType());
    final float[] expected = toFloat(values);
    final float[] second = new float[12];
    System.arraycopy(expected, 12, second, 0, 12);
    assertArrayEquals(second, (float[]) imp.getStack().getPixels(2), 0f);
  }

  /** The cast to float is lossy above 2^24; this documents that rather than endorsing it. */
  @Test
  public void valuesAbove2Pow24LosePrecision()
  {
    final int big = (1 << 24) + 1;
    final String file = writeInt32("/plane", new int[] {big, 0, 0, 0}, 2, 2);
    final ImagePlus imp = HDF5ImageJ.hdf5read(file, "/plane");
    assertEquals((float) (1 << 24), ((FloatProcessor) imp.getProcessor()).getf(0), 0f);
  }
}
