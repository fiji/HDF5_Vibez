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
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.base.mdarray.MDIntArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import ij.ImagePlus;
import ij.VirtualStack;

/** HDF5-backed virtual stacks (#17) must give the same pixels as loading the data into memory. */
public class HDF5VirtualStackTest
{
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private static final int Z = 3, Y = 4, X = 5;

  private File file() { return new File(tmp.getRoot(), "virtual.h5"); }

  private IHDF5Writer open() { return HDF5Factory.open(file().getAbsolutePath()); }

  private static long[] dims(final int... d)
  {
    final long[] l = new long[d.length];
    for (int i = 0; i < d.length; i++) l[i] = d[i];
    return l;
  }

  private static byte[] bytes(final int n) { final byte[] a = new byte[n]; for (int i = 0; i < n; i++) a[i] = (byte) (i * 7 + 3); return a; }
  private static short[] shorts(final int n) { final short[] a = new short[n]; for (int i = 0; i < n; i++) a[i] = (short) (i * 1009 - 20000); return a; }
  private static int[] ints(final int n) { final int[] a = new int[n]; for (int i = 0; i < n; i++) a[i] = i * 100003 - 500000; return a; }
  private static float[] floats(final int n) { final float[] a = new float[n]; for (int i = 0; i < n; i++) a[i] = i * 0.37f - 5f; return a; }

  /** Asserts that every slice of the virtual stack equals the corresponding in-memory slice. */
  private static void assertSameSlices(final ImagePlus expected, final ImagePlus actual)
  {
    assertNotNull(actual);
    assertTrue(actual.getStack() instanceof VirtualStack);
    assertEquals(expected.getType(), actual.getType());
    assertEquals(expected.getWidth(), actual.getWidth());
    assertEquals(expected.getHeight(), actual.getHeight());
    assertEquals(expected.getStackSize(), actual.getStackSize());
    for (int i = 1; i <= expected.getStackSize(); i++)
    {
      final Object e = expected.getStack().getProcessor(i).getPixels();
      final Object a = actual.getStack().getProcessor(i).getPixels();
      if (e instanceof byte[]) assertArrayEquals("slice " + i, (byte[]) e, (byte[]) a);
      else if (e instanceof short[]) assertArrayEquals("slice " + i, (short[]) e, (short[]) a);
      else assertArrayEquals("slice " + i, (float[]) e, (float[]) a, 0f);
    }
  }

  private void check(final String dset)
  {
    final String f = file().getAbsolutePath();
    final ImagePlus memory = HDF5ImageJ.loadDataSetsToHyperStack(f, new String[] {dset}, 1, 1, false);
    final ImagePlus virtual = HDF5ImageJ.loadDataSetsToVirtualStack(f, new String[] {dset}, false);
    assertSameSlices(memory, virtual);
  }

  @Test
  public void uint8Stack()
  {
    final IHDF5Writer w = open();
    w.uint8().writeMDArray("/d", new MDByteArray(bytes(Z * Y * X), dims(Z, Y, X)));
    w.close();
    check("/d");
  }

  @Test
  public void uint16Stack()
  {
    final IHDF5Writer w = open();
    w.uint16().writeMDArray("/d", new MDShortArray(shorts(Z * Y * X), dims(Z, Y, X)));
    w.close();
    check("/d");
  }

  @Test
  public void int16Stack()
  {
    final IHDF5Writer w = open();
    w.int16().writeMDArray("/d", new MDShortArray(shorts(Z * Y * X), dims(Z, Y, X)));
    w.close();
    check("/d");
  }

  @Test
  public void float32Stack()
  {
    final IHDF5Writer w = open();
    w.float32().writeMDArray("/d", new MDFloatArray(floats(Z * Y * X), dims(Z, Y, X)));
    w.close();
    check("/d");
  }

  @Test
  public void int32Stack()
  {
    final IHDF5Writer w = open();
    w.int32().writeMDArray("/d", new MDIntArray(ints(Z * Y * X), dims(Z, Y, X)));
    w.close();
    check("/d");
  }

  @Test
  public void twoDimensionalDataset()
  {
    final IHDF5Writer w = open();
    w.uint16().writeMDArray("/d", new MDShortArray(shorts(Y * X), dims(Y, X)));
    w.close();
    check("/d");
  }

  @Test
  public void sizeCalibrationAndLazyLoading()
  {
    final IHDF5Writer w = open();
    w.uint8().writeMDArray("/d", new MDByteArray(bytes(Z * Y * X), dims(Z, Y, X)));
    w.float32().setArrayAttr("/d", "element_size_um", new float[] {3f, 2f, 1f});
    w.close();
    final ImagePlus imp = HDF5ImageJ.loadDataSetsToVirtualStack(file().getAbsolutePath(), new String[] {"/d"}, false);
    assertNotNull(imp);
    assertEquals(Z, imp.getStackSize());
    assertEquals(X, imp.getWidth());
    assertEquals(Y, imp.getHeight());
    assertEquals(3, imp.getCalibration().pixelDepth, 1e-6);
    assertEquals(2, imp.getCalibration().pixelHeight, 1e-6);
    assertEquals(1, imp.getCalibration().pixelWidth, 1e-6);
  }

  @Test
  public void slicesCanBeReadOutOfOrderAndRepeatedly()
  {
    final byte[] data = bytes(Z * Y * X);
    final IHDF5Writer w = open();
    w.uint8().writeMDArray("/d", new MDByteArray(data, dims(Z, Y, X)));
    w.close();
    final ImagePlus imp = HDF5ImageJ.loadDataSetsToVirtualStack(file().getAbsolutePath(), new String[] {"/d"}, false);
    for (final int z : new int[] {3, 1, 3, 2, 1})
    {
      final byte[] expected = new byte[Y * X];
      System.arraycopy(data, (z - 1) * Y * X, expected, 0, Y * X);
      assertArrayEquals("slice " + z, expected, (byte[]) imp.getStack().getProcessor(z).getPixels());
    }
  }

  /** Compares RGB slices, ignoring the alpha byte. */
  private static void assertSameRgb(final ImagePlus expected, final ImagePlus actual)
  {
    assertNotNull(actual);
    assertTrue(actual.getStack() instanceof VirtualStack);
    assertEquals(ImagePlus.COLOR_RGB, actual.getType());
    assertEquals(expected.getStackSize(), actual.getStackSize());
    for (int i = 1; i <= expected.getStackSize(); i++)
    {
      final int[] e = (int[]) expected.getStack().getProcessor(i).getPixels();
      final int[] a = (int[]) actual.getStack().getProcessor(i).getPixels();
      assertEquals(e.length, a.length);
      for (int j = 0; j < e.length; j++)
        assertEquals("slice " + i + " pixel " + j, e[j] & 0xffffff, a[j] & 0xffffff);
    }
  }

  @Test
  public void rgbStack()
  {
    final IHDF5Writer w = open();
    w.uint8().writeMDArray("/d", new MDByteArray(bytes(Z * Y * X * 3), dims(Z, Y, X, 3)));
    w.close();
    final String f = file().getAbsolutePath();
    assertSameRgb(HDF5ImageJ.loadDataSetsToHyperStack(f, new String[] {"/d"}, 1, 1, false),
        HDF5ImageJ.loadDataSetsToVirtualStack(f, new String[] {"/d"}, false));
  }

  @Test
  public void rgbPlane()
  {
    final IHDF5Writer w = open();
    w.uint8().writeMDArray("/d", new MDByteArray(bytes(Y * X * 3), dims(Y, X, 3)));
    w.close();
    final String f = file().getAbsolutePath();
    assertSameRgb(HDF5ImageJ.loadDataSetsToHyperStack(f, new String[] {"/d"}, 1, 1, false),
        HDF5ImageJ.loadDataSetsToVirtualStack(f, new String[] {"/d"}, false));
  }

  @Test
  public void fileIsNotHeldOpenBetweenReads()
  {
    final IHDF5Writer w = open();
    w.uint8().writeMDArray("/d", new MDByteArray(bytes(Z * Y * X), dims(Z, Y, X)));
    w.close();
    final ImagePlus imp = HDF5ImageJ.loadDataSetsToVirtualStack(file().getAbsolutePath(), new String[] {"/d"}, false);
    imp.getStack().getProcessor(2);

    // HDF5 refuses to open a file for writing while this process still has it open for reading
    final IHDF5Writer again = HDF5Factory.open(file().getAbsolutePath());
    again.uint8().writeMDArray("/other", new MDByteArray(bytes(4), dims(4)));
    again.close();
    assertEquals(Z, imp.getStackSize());
    imp.getStack().getProcessor(3); // and the stack still works afterwards
  }

  /** More than 2^31 elements: cannot be one Java array, but each plane is small. */
  @Test
  public void datasetLargerThanAJavaArray()
  {
    final int planes = 1 << 20;
    final int[] block = {1, 64, 64};
    final IHDF5Writer w = open();
    // chunked and unwritten, so the file stays tiny
    w.uint8().createMDArray("/big", dims(planes, 64, 64), new int[] {1, 64, 64});
    final byte[] last = bytes(64 * 64);
    w.uint8().writeMDArrayBlockWithOffset("/big", new MDByteArray(last, dims(block[0], block[1], block[2])),
        new long[] {planes - 1, 0, 0});
    w.close();

    final String f = file().getAbsolutePath();
    assertNull("the in-memory loader reports that the dataset is too big",
        HDF5ImageJ.loadDataSetsToHyperStack(f, new String[] {"/big"}, 1, 1, false));
    assertNull(HDF5ImageJ.loadCustomLayoutDataSetToHyperStack(f, "/big", "zyx", false));

    final ImagePlus virtual = HDF5ImageJ.loadDataSetsToVirtualStack(f, new String[] {"/big"}, false);
    assertNotNull(virtual);
    assertEquals(planes, virtual.getStackSize());
    assertArrayEquals(last, (byte[]) virtual.getStack().getProcessor(planes).getPixels());
    assertArrayEquals(new byte[64 * 64], (byte[]) virtual.getStack().getProcessor(1).getPixels());

    final ImagePlus custom = HDF5ImageJ.loadCustomLayoutDataSetToVirtualStack(f, "/big", "zyx", false);
    assertArrayEquals(last, (byte[]) custom.getStack().getProcessor(planes).getPixels());
  }

  // ---- custom layouts --------------------------------------------------------------------

  private void checkLayout(final String layout)
  {
    final String f = file().getAbsolutePath();
    final ImagePlus memory = HDF5ImageJ.loadCustomLayoutDataSetToHyperStack(f, "/d", layout, false);
    final ImagePlus virtual = HDF5ImageJ.loadCustomLayoutDataSetToVirtualStack(f, "/d", layout, false);
    assertSameSlices(memory, virtual);
  }

  private void writeUint16Cube()
  {
    final IHDF5Writer w = open();
    w.uint16().writeMDArray("/d", new MDShortArray(shorts(Z * Y * X), dims(Z, Y, X)));
    w.close();
  }

  @Test public void layoutZyx() { writeUint16Cube(); checkLayout("zyx"); }
  @Test public void layoutYzx() { writeUint16Cube(); checkLayout("yzx"); }
  @Test public void layoutXyz() { writeUint16Cube(); checkLayout("xyz"); }
  @Test public void layoutZxy() { writeUint16Cube(); checkLayout("zxy"); }

  @Test
  public void layoutFloat32()
  {
    final IHDF5Writer w = open();
    w.float32().writeMDArray("/d", new MDFloatArray(floats(Z * Y * X), dims(Z, Y, X)));
    w.close();
    checkLayout("yzx");
  }

  @Test
  public void layoutOfPlane()
  {
    final IHDF5Writer w = open();
    w.uint8().writeMDArray("/d", new MDByteArray(bytes(Y * X), dims(Y, X)));
    w.close();
    checkLayout("yx");
  }

  @Test
  public void layoutWithChannelOrTimeIsRejected()
  {
    writeUint16Cube();
    assertNull(HDF5ImageJ.loadCustomLayoutDataSetToVirtualStack(file().getAbsolutePath(), "/d", "ctx", false));
  }

  @Test
  public void layoutOfWrongRankIsRejected()
  {
    writeUint16Cube();
    assertNull(HDF5ImageJ.loadCustomLayoutDataSetToVirtualStack(file().getAbsolutePath(), "/d", "yx", false));
  }
}
