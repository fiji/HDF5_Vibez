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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import ij.ImagePlus;

/**
 * A 3-D uint8 dataset ending in 3 can be an RGB image ([y, x, 3]) or a volume that is 3 pixels
 * wide ([z, y, 3]); {@link HDF5ImageJ.ColorInterpretation} lets the caller say which.
 */
public class HDF5ColorInterpretationTest
{
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private static final int A = 4, B = 5; // dataset is A x B x 3

  private String write(final boolean uint8)
  {
    final String file = new File(tmp.getRoot(), "ambiguous.h5").getAbsolutePath();
    final IHDF5Writer w = HDF5Factory.open(file);
    final long[] dims = {A, B, 3};
    if (uint8)
    {
      final byte[] data = new byte[A * B * 3];
      for (int i = 0; i < data.length; i++) data[i] = (byte) (i + 1);
      w.uint8().writeMDArray("/d", new MDByteArray(data, dims));
    }
    else
    {
      final short[] data = new short[A * B * 3];
      for (int i = 0; i < data.length; i++) data[i] = (short) (i + 1);
      w.uint16().writeMDArray("/d", new MDShortArray(data, dims));
    }
    w.close();
    return file;
  }

  private static final String[] D = {"/d"};

  @Test
  public void rgbIsTheDefault()
  {
    final String f = write(true);
    final ImagePlus imp = HDF5ImageJ.loadDataSetsToHyperStack(f, D, 1, 1, false);
    assertEquals(ImagePlus.COLOR_RGB, imp.getType());
    assertEquals(B, imp.getWidth());
    assertEquals(A, imp.getHeight());
    assertEquals(1, imp.getNSlices());
  }

  @Test
  public void volumeLoadsAsZyx()
  {
    final String f = write(true);
    final ImagePlus imp = HDF5ImageJ.loadDataSetsToHyperStack(f, D, 1, 1, false,
        HDF5ImageJ.ColorInterpretation.VOLUME);
    assertEquals(ImagePlus.GRAY8, imp.getType());
    assertEquals(3, imp.getWidth());
    assertEquals(B, imp.getHeight());
    assertEquals(A, imp.getNSlices());
    // slice z holds the dataset's elements [z, :, :] in order
    final byte[] expected = new byte[B * 3];
    for (int i = 0; i < expected.length; i++) expected[i] = (byte) (2 * B * 3 + i + 1);
    assertArrayEquals(expected, (byte[]) imp.getStack().getPixels(3));
  }

  @Test
  public void publicReadHonorsTheOption()
  {
    final String f = write(true);
    final ImagePlus imp = HDF5ImageJ.hdf5read(f, D, 1, 1, HDF5ImageJ.ColorInterpretation.VOLUME);
    assertEquals(A, imp.getNSlices());
    assertEquals(ImagePlus.COLOR_RGB,
        HDF5ImageJ.hdf5read(f, D, 1, 1, HDF5ImageJ.ColorInterpretation.RGB).getType());
  }

  @Test
  public void virtualStackHonorsTheOption()
  {
    final String f = write(true);
    final ImagePlus rgb = HDF5ImageJ.loadDataSetsToVirtualStack(f, D, false);
    assertEquals(ImagePlus.COLOR_RGB, rgb.getType());
    assertEquals(1, rgb.getStackSize());

    final ImagePlus volume = HDF5ImageJ.loadDataSetsToVirtualStack(f, D, false,
        HDF5ImageJ.ColorInterpretation.VOLUME);
    final ImagePlus memory = HDF5ImageJ.loadDataSetsToHyperStack(f, D, 1, 1, false,
        HDF5ImageJ.ColorInterpretation.VOLUME);
    assertEquals(A, volume.getStackSize());
    for (int z = 1; z <= A; z++)
      assertArrayEquals("slice " + z, (byte[]) memory.getStack().getPixels(z),
          (byte[]) volume.getStack().getProcessor(z).getPixels());
  }

  @Test
  public void otherTypesAreNeverRgb()
  {
    final String f = write(false);
    for (final HDF5ImageJ.ColorInterpretation c : new HDF5ImageJ.ColorInterpretation[] {
        HDF5ImageJ.ColorInterpretation.RGB, HDF5ImageJ.ColorInterpretation.VOLUME})
    {
      final ImagePlus imp = HDF5ImageJ.loadDataSetsToHyperStack(f, D, 1, 1, false, c);
      assertNotNull(imp);
      assertEquals(ImagePlus.GRAY16, imp.getType());
      assertEquals(A, imp.getNSlices());
    }
  }
}
