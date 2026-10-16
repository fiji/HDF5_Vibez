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

import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;

/** element_size_um is written in micrometers and may be stored as (y, x) by other tools (#10). */
public class HDF5CalibrationTest
{
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private static ImagePlus image(final int nSlices, final String unit,
      final double depth, final double height, final double width)
  {
    final ImageStack stack = new ImageStack(4, 3);
    for (int z = 0; z < nSlices; z++) stack.addSlice(new ByteProcessor(4, 3));
    final ImagePlus imp = new ImagePlus("cal", stack);
    imp.getCalibration().setUnit(unit);
    imp.getCalibration().pixelDepth = depth;
    imp.getCalibration().pixelHeight = height;
    imp.getCalibration().pixelWidth = width;
    return imp;
  }

  private static void assertElementSize(final float[] expected, final String unit)
  {
    assertArrayEquals(unit, expected,
        HDF5ImageJ.getElementSizeUm(image(2, unit, 1, 2, 3)), 1e-6f);
  }

  @Test
  public void convertsUnitsToMicrometers()
  {
    assertElementSize(new float[] {1, 2, 3}, "micrometer");
    assertElementSize(new float[] {1, 2, 3}, "pixel");
    assertElementSize(new float[] {1e6f, 2e6f, 3e6f}, "m");
    assertElementSize(new float[] {1e6f, 2e6f, 3e6f}, "meter");
    assertElementSize(new float[] {1e4f, 2e4f, 3e4f}, "cm");
    assertElementSize(new float[] {1e3f, 2e3f, 3e3f}, "mm");
    assertElementSize(new float[] {1e-3f, 2e-3f, 3e-3f}, "nm");
    assertElementSize(new float[] {1e-3f, 2e-3f, 3e-3f}, "nanometer");
    assertElementSize(new float[] {1e-6f, 2e-6f, 3e-6f}, "pm");
    assertElementSize(new float[] {1e-6f, 2e-6f, 3e-6f}, "picometer");
  }

  @Test
  public void alwaysReturnsThreeValues()
  {
    assertEquals(3, HDF5ImageJ.getElementSizeUm(image(1, "mm", 1, 2, 3)).length);
    assertEquals(3, HDF5ImageJ.getElementSizeUm(image(5, "mm", 1, 2, 3)).length);
  }

  @Test
  public void normalizePrependsOneToTwoValues()
  {
    assertArrayEquals(new float[] {1, 2, 3}, HDF5ImageJ.normalizeElementSizeUm(new float[] {2, 3}), 0f);
    assertArrayEquals(new float[] {4, 5, 6}, HDF5ImageJ.normalizeElementSizeUm(new float[] {4, 5, 6}), 0f);
  }

  @Test
  public void writesMillimetersAsMicrometers() throws Exception
  {
    final String file = new File(tmp.getRoot(), "mm.h5").getAbsolutePath();
    HDF5ImageJ.hdf5write(image(3, "mm", 0.5, 0.25, 0.125), file, "/data");

    final ImagePlus read = HDF5ImageJ.hdf5read(file, "/data");
    assertEquals(500, read.getCalibration().pixelDepth, 1e-3);
    assertEquals(250, read.getCalibration().pixelHeight, 1e-3);
    assertEquals(125, read.getCalibration().pixelWidth, 1e-3);
    assertEquals("micrometer", read.getCalibration().getUnit());
  }

  @Test
  public void singleSlicePlaneRoundTripsAndLists() throws Exception
  {
    final String file = new File(tmp.getRoot(), "plane.h5").getAbsolutePath();
    HDF5ImageJ.hdf5write(image(1, "micrometer", 1, 0.5, 0.25), file, "/plane");

    final ArrayList<DataSetInfo> list = HDF5ImageJ.hdf5list(file);
    assertEquals(1, list.size());
    assertEquals("1.0x0.5x0.25", list.get(0).element_size_um_text);

    final ImagePlus read = HDF5ImageJ.hdf5read(file, "/plane");
    assertEquals(0.5, read.getCalibration().pixelHeight, 1e-6);
    assertEquals(0.25, read.getCalibration().pixelWidth, 1e-6);
  }

  /** A 2-D dataset written by another tool, with element_size_um as (y, x). */
  private String writeTwoElementFile()
  {
    final String file = new File(tmp.getRoot(), "twoelem.h5").getAbsolutePath();
    final IHDF5Writer writer = HDF5Factory.open(file);
    writer.uint8().writeMDArray("/plane", new MDByteArray(new byte[12], new long[] {3, 4}));
    writer.float32().setArrayAttr("/plane", "element_size_um", new float[] {0.5f, 0.25f});
    writer.close();
    return file;
  }

  @Test
  public void readsTwoElementSizes()
  {
    final ImagePlus read = HDF5ImageJ.hdf5read(writeTwoElementFile(), "/plane");
    assertNotNull(read);
    assertEquals(1, read.getCalibration().pixelDepth, 1e-6);
    assertEquals(0.5, read.getCalibration().pixelHeight, 1e-6);
    assertEquals(0.25, read.getCalibration().pixelWidth, 1e-6);
  }

  @Test
  public void readsTwoElementSizesWithCustomLayout()
  {
    final ImagePlus read = HDF5ImageJ.hdf5read(writeTwoElementFile(), "/plane", "yx");
    assertNotNull(read);
    assertEquals(0.5, read.getCalibration().pixelHeight, 1e-6);
    assertEquals(0.25, read.getCalibration().pixelWidth, 1e-6);
  }

  @Test
  public void listsTwoElementSizes()
  {
    final ArrayList<DataSetInfo> list = HDF5ImageJ.hdf5list(writeTwoElementFile());
    assertEquals(1, list.size());
    assertEquals("1.0x0.5x0.25", list.get(0).element_size_um_text);
  }
}
