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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import hdf.hdf5lib.exceptions.HDF5Exception;
import ij.ImagePlus;

import sc.fiji.hdf5.HDF5ImageJ.ColorInterpretation;

/** Datasets of rank 4 and 5, and datasets whose last dimension holds three channels. */
public class HDF5HyperstackLayoutTest
{
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private static final String[] D = {"/d"};

  private String file() { return new File(tmp.getRoot(), "layout.h5").getAbsolutePath(); }

  /** Value stored at a flat index, distinct for distinct indices (mod 251, fits in a byte). */
  private static int value(final int flat) { return 1 + flat % 251; }

  private String writeUint8(final long... dims)
  {
    int n = 1;
    for (final long d : dims) n *= d;
    final byte[] data = new byte[n];
    for (int i = 0; i < n; i++) data[i] = (byte) value(i);
    final IHDF5Writer w = HDF5Factory.open(file());
    w.uint8().writeMDArray("/d", new MDByteArray(data, dims));
    w.close();
    return file();
  }

  private String writeUint16(final long... dims)
  {
    int n = 1;
    for (final long d : dims) n *= d;
    final short[] data = new short[n];
    for (int i = 0; i < n; i++) data[i] = (short) (1000 + i);
    final IHDF5Writer w = HDF5Factory.open(file());
    w.uint16().writeMDArray("/d", new MDShortArray(data, dims));
    w.close();
    return file();
  }

  // ---- the decision table ----------------------------------------------------------------

  @Test
  public void layoutForDecidesTheCustomLayout()
  {
    final ColorInterpretation rgb = ColorInterpretation.RGB, vol = ColorInterpretation.VOLUME,
        ch = ColorInterpretation.CHANNELS;
    // plain stacks and RGB images need no custom layout
    assertNull(HDF5ImageJ.layoutFor(2, new long[] {4, 5}, "uint8", rgb));
    assertNull(HDF5ImageJ.layoutFor(3, new long[] {2, 4, 5}, "uint8", rgb));
    assertNull(HDF5ImageJ.layoutFor(3, new long[] {4, 5, 3}, "uint8", rgb));
    assertNull(HDF5ImageJ.layoutFor(3, new long[] {4, 5, 3}, "uint8", vol));
    assertNull(HDF5ImageJ.layoutFor(4, new long[] {2, 4, 5, 3}, "uint8", rgb));
    // rank 4 is t, z, y, x unless it is uint8 RGB
    assertEquals("tzyx", HDF5ImageJ.layoutFor(4, new long[] {2, 3, 4, 5}, "uint8", rgb));
    assertEquals("tzyx", HDF5ImageJ.layoutFor(4, new long[] {2, 3, 4, 5}, "uint16", rgb));
    assertEquals("tzyx", HDF5ImageJ.layoutFor(4, new long[] {2, 4, 5, 3}, "uint16", rgb));
    assertEquals("tzyx", HDF5ImageJ.layoutFor(4, new long[] {2, 4, 5, 3}, "uint8", vol));
    // a last dimension of 3 as channels
    assertEquals("yxc", HDF5ImageJ.layoutFor(3, new long[] {4, 5, 3}, "uint16", ch));
    assertEquals("yxc", HDF5ImageJ.layoutFor(3, new long[] {4, 5, 3}, "uint8", ch));
    assertEquals("zyxc", HDF5ImageJ.layoutFor(4, new long[] {2, 4, 5, 3}, "uint8", ch));
    assertEquals("tzyxc", HDF5ImageJ.layoutFor(5, new long[] {2, 2, 4, 5, 3}, "uint8", ch));
    // rank 5 only makes sense with three channels, whatever the interpretation
    assertEquals("tzyxc", HDF5ImageJ.layoutFor(5, new long[] {2, 2, 4, 5, 3}, "uint8", rgb));
    assertNull(HDF5ImageJ.layoutFor(5, new long[] {2, 2, 4, 5, 6}, "uint8", rgb));
    // CHANNELS needs a last dimension of 3
    assertNull(HDF5ImageJ.layoutFor(3, new long[] {2, 4, 5}, "uint8", ch));
  }

  // ---- rank 4: [t, z, y, x] --------------------------------------------------------------

  @Test
  public void uint8TzyxIsAHyperstackNotRgb()
  {
    // before, any rank-4 uint8 dataset was read as RGB, whatever its last dimension
    final int T = 2, Z = 3, Y = 4, X = 5;
    final String f = writeUint8(T, Z, Y, X);
    final ImagePlus imp = HDF5ImageJ.hdf5read(f, D, 1, 1);
    assertNotNull(imp);
    assertEquals(ImagePlus.GRAY8, imp.getType());
    assertEquals(X, imp.getWidth());
    assertEquals(Y, imp.getHeight());
    assertEquals(1, imp.getNChannels());
    assertEquals(Z, imp.getNSlices());
    assertEquals(T, imp.getNFrames());
    for (int t = 0; t < T; t++)
      for (int z = 0; z < Z; z++)
      {
        final byte[] pixels = (byte[]) imp.getStack().getPixels(imp.getStackIndex(1, z + 1, t + 1));
        final int plane = (t * Z + z) * Y * X;
        assertEquals("t=" + t + " z=" + z, (byte) value(plane), pixels[0]);
        assertEquals("t=" + t + " z=" + z, (byte) value(plane + Y * X - 1), pixels[Y * X - 1]);
      }
  }

  @Test
  public void uint16Tzyx()
  {
    final String f = writeUint16(2, 3, 4, 5);
    final ImagePlus imp = HDF5ImageJ.hdf5read(f, D, 1, 1);
    assertNotNull(imp);
    assertEquals(ImagePlus.GRAY16, imp.getType());
    assertEquals(3, imp.getNSlices());
    assertEquals(2, imp.getNFrames());
    final short[] last = (short[]) imp.getStack().getPixels(imp.getStackIndex(1, 3, 2));
    assertEquals((short) (1000 + 2 * 3 * 4 * 5 - 1), last[last.length - 1]);
  }

  @Test
  public void rgbStackIsStillRgbByDefault()
  {
    final String f = writeUint8(2, 4, 5, 3);
    final ImagePlus imp = HDF5ImageJ.hdf5read(f, D, 1, 1);
    assertEquals(ImagePlus.COLOR_RGB, imp.getType());
    assertEquals(2, imp.getNSlices());
  }

  @Test
  public void volumeMakesARgbShapedRank4ATzyx()
  {
    final String f = writeUint8(2, 4, 5, 3);
    final ImagePlus imp = HDF5ImageJ.hdf5read(f, D, 1, 1, ColorInterpretation.VOLUME);
    assertEquals(ImagePlus.GRAY8, imp.getType());
    assertEquals(3, imp.getWidth());
    assertEquals(5, imp.getHeight());
    assertEquals(4, imp.getNSlices());
    assertEquals(2, imp.getNFrames());
  }

  // ---- three channels --------------------------------------------------------------------

  @Test
  public void channelsOfAPlane()
  {
    final int Y = 4, X = 5;
    final String f = writeUint16(Y, X, 3);
    final ImagePlus imp = HDF5ImageJ.hdf5read(f, D, 1, 1, ColorInterpretation.CHANNELS);
    assertNotNull(imp);
    assertEquals(ImagePlus.GRAY16, imp.getType());
    assertEquals(X, imp.getWidth());
    assertEquals(Y, imp.getHeight());
    assertEquals(3, imp.getNChannels());
    assertEquals(1, imp.getNSlices());
    // sample c of pixel (y, x) is at flat index (y * X + x) * 3 + c
    for (int c = 0; c < 3; c++)
    {
      final short[] pixels = (short[]) imp.getStack().getPixels(imp.getStackIndex(c + 1, 1, 1));
      assertEquals("channel " + c, (short) (1000 + c), pixels[0]);
      assertEquals("channel " + c, (short) (1000 + (X + 2) * 3 + c), pixels[X + 2]); // y=1, x=2
    }
  }

  @Test
  public void channelsOfAUint8Stack()
  {
    final String f = writeUint8(2, 4, 5, 3);
    final ImagePlus imp = HDF5ImageJ.hdf5read(f, D, 1, 1, ColorInterpretation.CHANNELS);
    assertEquals(ImagePlus.GRAY8, imp.getType());
    assertEquals(3, imp.getNChannels());
    assertEquals(2, imp.getNSlices());
    assertEquals(1, imp.getNFrames());
    final byte[] pixels = (byte[]) imp.getStack().getPixels(imp.getStackIndex(2, 2, 1));
    assertEquals((byte) value(4 * 5 * 3 + 1), pixels[0]); // z=1, y=0, x=0, c=1
  }

  @Test
  public void rank5IsTzyxWithThreeChannels()
  {
    final int T = 2, Z = 2, Y = 3, X = 4;
    final String f = writeUint8(T, Z, Y, X, 3);
    final ImagePlus imp = HDF5ImageJ.hdf5read(f, D, 1, 1);
    assertNotNull(imp);
    assertEquals(3, imp.getNChannels());
    assertEquals(Z, imp.getNSlices());
    assertEquals(T, imp.getNFrames());
    final byte[] pixels = (byte[]) imp.getStack().getPixels(imp.getStackIndex(3, 2, 2));
    assertEquals((byte) value(((1 * Z + 1) * Y * X) * 3 + 2), pixels[0]); // t=1, z=1, c=2
  }

  // ---- what is refused -------------------------------------------------------------------

  @Test
  public void virtualStacksRefuseHyperstacksAndChannels()
  {
    assertNull(HDF5ImageJ.loadDataSetsToVirtualStack(writeUint8(2, 3, 4, 5), D, false));
    assertNull(HDF5ImageJ.loadDataSetsToVirtualStack(writeUint16(4, 5, 3), D, false, ColorInterpretation.CHANNELS));
  }

  @Test
  public void rank4CannotBeCombinedWithOtherDatasets()
  {
    final IHDF5Writer w = HDF5Factory.open(file());
    w.uint8().writeMDArray("/a", new MDByteArray(new byte[2 * 3 * 4 * 5], new long[] {2, 3, 4, 5}));
    w.uint8().writeMDArray("/b", new MDByteArray(new byte[2 * 3 * 4 * 5], new long[] {2, 3, 4, 5}));
    w.close();
    assertNull(HDF5ImageJ.hdf5read(file(), new String[] {"/a", "/b"}, 2, 1));
  }

  @Test
  public void unsupportedRankIsStillRefused() throws HDF5Exception
  {
    assertNull(HDF5ImageJ.hdf5read(writeUint8(2, 2, 3, 4, 5), D, 1, 1));
  }
}
