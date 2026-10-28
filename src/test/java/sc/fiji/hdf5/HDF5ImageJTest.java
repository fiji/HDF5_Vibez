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

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;

public class HDF5ImageJTest
{
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void testWriteListRead() throws Exception
  {
    final int w = 4, h = 3, d = 2;
    final ImageStack stack = new ImageStack(w, h);
    for (int z = 0; z < d; z++) {
      final byte[] pixels = new byte[w * h];
      for (int i = 0; i < pixels.length; i++)
        pixels[i] = (byte) (z * 100 + i);
      stack.addSlice(new ByteProcessor(w, h, pixels));
    }
    final ImagePlus imp = new ImagePlus("test", stack);

    final String file = new File(tmp.getRoot(), "test.h5").getAbsolutePath();
    HDF5ImageJ.hdf5write(imp, file, "/data");

    final ArrayList<DataSetInfo> list = HDF5ImageJ.hdf5list(file);
    assertEquals(1, list.size());
    assertEquals("/data", list.get(0).path);

    final ImagePlus read = HDF5ImageJ.hdf5read(file, new String[]{"/data"}, 1, 1);
    assertNotNull(read);
    assertEquals(w, read.getWidth());
    assertEquals(h, read.getHeight());
    assertEquals(d, read.getNSlices());
    for (int z = 1; z <= d; z++)
      assertArrayEquals((byte[]) stack.getPixels(z), (byte[]) read.getStack().getPixels(z));
  }
}
