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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDIntArray;
import ch.systemsx.cisd.base.mdarray.MDLongArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import ij.ImagePlus;
import ij.VirtualStack;

/** int8, uint32, int64 and uint64 datasets are loaded as 32-bit floats. */
public class HDF5IntegerTypesTest
{
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private static final int Z = 2, Y = 3, X = 4, N = Z * Y * X;
  private static final long[] DIMS = {Z, Y, X};

  /** Values the datasets hold, as longs; includes negatives (for signed types). */
  private static long[] signedValues()
  {
    final long[] v = new long[N];
    for (int i = 0; i < N; i++) v[i] = (i - 10) * 5;
    return v;
  }

  private static long[] unsignedValues()
  {
    final long[] v = new long[N];
    for (int i = 0; i < N; i++) v[i] = i * 100003L;
    return v;
  }

  private String file() { return new File(tmp.getRoot(), "ints.h5").getAbsolutePath(); }

  private static float[] toFloat(final long[] v)
  {
    final float[] f = new float[v.length];
    for (int i = 0; i < f.length; i++) f[i] = v[i];
    return f;
  }

  private String write(final String type)
  {
    final IHDF5Writer w = HDF5Factory.open(file());
    switch (type)
    {
      case "int8":
      {
        final byte[] b = new byte[N];
        final long[] v = signedValues();
        for (int i = 0; i < N; i++) b[i] = (byte) v[i];
        w.int8().writeMDArray("/d", new MDByteArray(b, DIMS));
        break;
      }
      case "uint32":
      {
        final int[] a = new int[N];
        final long[] v = unsignedValues();
        for (int i = 0; i < N; i++) a[i] = (int) v[i];
        w.uint32().writeMDArray("/d", new MDIntArray(a, DIMS));
        break;
      }
      case "int64":
        w.int64().writeMDArray("/d", new MDLongArray(signedValues(), DIMS));
        break;
      case "uint64":
        w.uint64().writeMDArray("/d", new MDLongArray(unsignedValues(), DIMS));
        break;
      default:
        throw new IllegalArgumentException(type);
    }
    w.close();
    return file();
  }

  private static float[] expected(final String type)
  {
    return toFloat(type.equals("int8") || type.equals("int64") ? signedValues() : unsignedValues());
  }

  private void check(final String type)
  {
    final String f = write(type);
    final float[] expected = expected(type);

    assertEquals(type, HDF5ImageJ.hdf5list(f).get(0).typeText);
    assertEquals(32, HDF5ImageJ.assignHDF5TypeToImagePlusBitdepth(type, false));

    final ImagePlus memory = HDF5ImageJ.hdf5read(f, "/d");
    assertNotNull(type, memory);
    assertEquals(ImagePlus.GRAY32, memory.getType());
    assertEquals(Z, memory.getNSlices());
    final ImagePlus custom = HDF5ImageJ.hdf5read(f, "/d", "zyx");
    final ImagePlus virtual = HDF5ImageJ.loadDataSetsToVirtualStack(f, new String[] {"/d"}, false);
    assertTrue(virtual.getStack() instanceof VirtualStack);
    for (int z = 0; z < Z; z++)
    {
      final float[] slice = new float[Y * X];
      System.arraycopy(expected, z * Y * X, slice, 0, slice.length);
      assertArrayEquals(type + " in memory, slice " + (z + 1), slice, (float[]) memory.getStack().getPixels(z + 1), 0f);
      assertArrayEquals(type + " custom layout, slice " + (z + 1), slice, (float[]) custom.getStack().getPixels(z + 1), 0f);
      assertArrayEquals(type + " virtual, slice " + (z + 1), slice, (float[]) virtual.getStack().getProcessor(z + 1).getPixels(), 0f);
    }
  }

  @Test public void int8() { check("int8"); }
  @Test public void uint32() { check("uint32"); }
  @Test public void int64() { check("int64"); }
  @Test public void uint64() { check("uint64"); }

  @Test
  public void uint32AboveInt32MaxIsNotNegative()
  {
    final IHDF5Writer w = HDF5Factory.open(file());
    w.uint32().writeMDArray("/d", new MDIntArray(new int[] {(int) 4000000000L, 1, 2, 3}, new long[] {2, 2}));
    w.close();
    final ImagePlus imp = HDF5ImageJ.hdf5read(file(), "/d");
    assertEquals(4.0e9f, imp.getProcessor().getf(0), 1e3f);
  }

  @Test
  public void helpers()
  {
    for (final String t : new String[] {"int8", "int32", "uint32", "int64", "uint64"})
      assertTrue(t, HDF5ImageJ.isIntegerConvertedToFloat(t));
    for (final String t : new String[] {"uint8", "uint16", "int16", "float32", "float64"})
      assertEquals(t, false, HDF5ImageJ.isIntegerConvertedToFloat(t));
    assertEquals(false, HDF5ImageJ.longLosesPrecision(new long[] {1L << 24, -(1L << 24)}));
    assertEquals(true, HDF5ImageJ.longLosesPrecision(new long[] {(1L << 24) + 1}));
    assertEquals(true, HDF5ImageJ.longLosesPrecision(new long[] {Long.MIN_VALUE}));
  }
}
