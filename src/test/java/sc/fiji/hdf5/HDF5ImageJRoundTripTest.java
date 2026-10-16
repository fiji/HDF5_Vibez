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
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * Round-trip tests for {@link HDF5ImageJ}: write an {@link ImagePlus} with
 * {@code hdf5write}, then check what {@code hdf5list} and {@code hdf5read}
 * return.
 */
public class HDF5ImageJRoundTripTest
{
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private static final int W = 5, H = 4;

  private String path(final String name)
  {
    return new File(tmp.getRoot(), name).getAbsolutePath();
  }

  private static ImageProcessor makeProcessor(final int type, final int seed)
  {
    final Random rnd = new Random(seed);
    switch (type)
    {
      case ImagePlus.GRAY8:
      {
        final byte[] p = new byte[W * H];
        rnd.nextBytes(p);
        return new ByteProcessor(W, H, p);
      }
      case ImagePlus.GRAY16:
      {
        final short[] p = new short[W * H];
        for (int i = 0; i < p.length; i++)
          p[i] = (short) rnd.nextInt(65536); // includes values above Short.MAX_VALUE
        return new ShortProcessor(W, H, p, null);
      }
      case ImagePlus.GRAY32:
      {
        final float[] p = new float[W * H];
        for (int i = 0; i < p.length; i++)
          p[i] = (rnd.nextFloat() - 0.5f) * 1e4f;
        return new FloatProcessor(W, H, p, null);
      }
      case ImagePlus.COLOR_RGB:
      {
        final int[] p = new int[W * H];
        for (int i = 0; i < p.length; i++)
          p[i] = rnd.nextInt() & 0xffffff;
        return new ColorProcessor(W, H, p);
      }
      default:
        throw new IllegalArgumentException("type " + type);
    }
  }

  private static ImagePlus makeImage(final int type, final int nSlices)
  {
    final ImageStack stack = new ImageStack(W, H);
    for (int z = 0; z < nSlices; z++)
      stack.addSlice(makeProcessor(type, z + 1));
    return new ImagePlus("test", stack);
  }

  private static void assertSamePixels(final ImagePlus expected, final ImagePlus actual)
  {
    assertNotNull(actual);
    assertEquals(expected.getType(), actual.getType());
    assertEquals(expected.getWidth(), actual.getWidth());
    assertEquals(expected.getHeight(), actual.getHeight());
    assertEquals(expected.getNChannels(), actual.getNChannels());
    assertEquals(expected.getNSlices(), actual.getNSlices());
    assertEquals(expected.getNFrames(), actual.getNFrames());
    for (int i = 1; i <= expected.getStackSize(); i++)
    {
      final Object e = expected.getStack().getPixels(i);
      final Object a = actual.getStack().getPixels(i);
      if (e instanceof byte[]) assertArrayEquals("slice " + i, (byte[]) e, (byte[]) a);
      else if (e instanceof short[]) assertArrayEquals("slice " + i, (short[]) e, (short[]) a);
      else if (e instanceof float[]) assertArrayEquals("slice " + i, (float[]) e, (float[]) a, 0f);
      else
      {
        // the reader sets the alpha byte, so compare the RGB bits only
        final int[] ei = (int[]) e, ai = (int[]) a;
        assertEquals(ei.length, ai.length);
        for (int j = 0; j < ei.length; j++)
          assertEquals("slice " + i + " pixel " + j, ei[j] & 0xffffff, ai[j] & 0xffffff);
      }
    }
  }

  private void roundTrip(final int type, final int nSlices) throws Exception
  {
    final ImagePlus imp = makeImage(type, nSlices);
    final String file = path("rt.h5");
    HDF5ImageJ.hdf5write(imp, file, "/data");
    assertSamePixels(imp, HDF5ImageJ.hdf5read(file, "/data"));
  }

  @Test public void gray8Stack() throws Exception { roundTrip(ImagePlus.GRAY8, 3); }
  @Test public void gray16Stack() throws Exception { roundTrip(ImagePlus.GRAY16, 3); }
  @Test public void gray32Stack() throws Exception { roundTrip(ImagePlus.GRAY32, 3); }
  @Test public void rgbStack() throws Exception { roundTrip(ImagePlus.COLOR_RGB, 3); }

  @Test public void gray8SingleSlice() throws Exception { roundTrip(ImagePlus.GRAY8, 1); }
  @Test public void gray16SingleSlice() throws Exception { roundTrip(ImagePlus.GRAY16, 1); }
  @Test public void gray32SingleSlice() throws Exception { roundTrip(ImagePlus.GRAY32, 1); }
  @Test public void rgbSingleSlice() throws Exception { roundTrip(ImagePlus.COLOR_RGB, 1); }

  @Test
  public void datasetTypesAreListed() throws Exception
  {
    final String file = path("types.h5");
    HDF5ImageJ.hdf5write(makeImage(ImagePlus.GRAY8, 2), file, "/u8", false);
    HDF5ImageJ.hdf5write(makeImage(ImagePlus.GRAY16, 2), file, "/u16", false);
    HDF5ImageJ.hdf5write(makeImage(ImagePlus.GRAY32, 2), file, "/f32", false);

    final ArrayList<DataSetInfo> list = HDF5ImageJ.hdf5list(file);
    assertEquals(3, list.size());
    for (final DataSetInfo info : list)
    {
      assertEquals("2x" + H + "x" + W, info.dimText);
      switch (info.path)
      {
        case "/u8": assertEquals("uint8", info.typeText); break;
        case "/u16": assertEquals("uint16", info.typeText); break;
        case "/f32": assertEquals("float32", info.typeText); break;
        default: throw new AssertionError("unexpected dataset " + info.path);
      }
    }
  }

  @Test
  public void listFindsDatasetsInNestedGroups() throws Exception
  {
    final String file = path("nested.h5");
    HDF5ImageJ.hdf5write(makeImage(ImagePlus.GRAY8, 2), file, "/a/b/c", false);
    HDF5ImageJ.hdf5write(makeImage(ImagePlus.GRAY8, 2), file, "/a/d", false);
    final ArrayList<DataSetInfo> list = HDF5ImageJ.hdf5list(file);
    assertEquals(2, list.size());
    final ArrayList<String> paths = new ArrayList<String>();
    for (final DataSetInfo info : list) paths.add(info.path);
    assertTrue(paths.contains("/a/b/c"));
    assertTrue(paths.contains("/a/d"));
  }

  @Test
  public void calibrationRoundTripsAsMicrometers() throws Exception
  {
    final ImagePlus imp = makeImage(ImagePlus.GRAY16, 3);
    imp.getCalibration().pixelDepth = 3.5;
    imp.getCalibration().pixelHeight = 0.5;
    imp.getCalibration().pixelWidth = 0.25;
    final String file = path("cal.h5");
    HDF5ImageJ.hdf5write(imp, file, "/data");

    final ImagePlus read = HDF5ImageJ.hdf5read(file, "/data");
    assertEquals(3.5, read.getCalibration().pixelDepth, 1e-6);
    assertEquals(0.5, read.getCalibration().pixelHeight, 1e-6);
    assertEquals(0.25, read.getCalibration().pixelWidth, 1e-6);
    assertEquals("micrometer", read.getCalibration().getUnit());

    final ArrayList<DataSetInfo> list = HDF5ImageJ.hdf5list(file);
    assertEquals("3.5x0.5x0.25", list.get(0).element_size_um_text);
  }

  @Test
  public void compressionDoesNotChangeData() throws Exception
  {
    final ImagePlus imp = makeImage(ImagePlus.GRAY16, 4);
    final String file = path("deflate.h5");
    HDF5ImageJ.hdf5write(imp, file, "/data", "", "", 6);
    assertSamePixels(imp, HDF5ImageJ.hdf5read(file, "/data"));
  }

  @Test
  public void replaceOverwritesTheFile() throws Exception
  {
    final String file = path("replace.h5");
    HDF5ImageJ.hdf5write(makeImage(ImagePlus.GRAY8, 2), file, "/first", true);
    HDF5ImageJ.hdf5write(makeImage(ImagePlus.GRAY8, 2), file, "/second", true);
    final ArrayList<DataSetInfo> list = HDF5ImageJ.hdf5list(file);
    assertEquals(1, list.size());
    assertEquals("/second", list.get(0).path);
  }

  @Test
  public void appendKeepsExistingDatasets() throws Exception
  {
    final String file = path("append.h5");
    final ImagePlus first = makeImage(ImagePlus.GRAY8, 2);
    final ImagePlus second = makeImage(ImagePlus.GRAY16, 2);
    HDF5ImageJ.hdf5write(first, file, "/first", false);
    HDF5ImageJ.hdf5write(second, file, "/second", false);
    assertEquals(2, HDF5ImageJ.hdf5list(file).size());
    assertSamePixels(first, HDF5ImageJ.hdf5read(file, "/first"));
    assertSamePixels(second, HDF5ImageJ.hdf5read(file, "/second"));
  }

  @Test
  public void hyperstackWritesOneDatasetPerFrameAndChannel() throws Exception
  {
    final int nC = 2, nZ = 3, nT = 2;
    final ImageStack stack = new ImageStack(W, H);
    for (int i = 0; i < nC * nZ * nT; i++)
      stack.addSlice(makeProcessor(ImagePlus.GRAY16, i + 1));
    final ImagePlus imp = new ImagePlus("hyper", stack);
    imp.setDimensions(nC, nZ, nT);

    final String file = path("hyper.h5");
    HDF5ImageJ.hdf5write(imp, file, "/t{t}/c{c}", "%d", "%d", 0);

    final ArrayList<DataSetInfo> list = HDF5ImageJ.hdf5list(file);
    assertEquals(nC * nT, list.size());

    final String[] names = {"/t0/c0", "/t0/c1", "/t1/c0", "/t1/c1"};
    final ImagePlus read = HDF5ImageJ.hdf5read(file, names, nT, nC);
    assertNotNull(read);
    assertEquals(nC, read.getNChannels());
    assertEquals(nZ, read.getNSlices());
    assertEquals(nT, read.getNFrames());
    for (int t = 1; t <= nT; t++)
      for (int c = 1; c <= nC; c++)
        for (int z = 1; z <= nZ; z++)
          assertArrayEquals("c=" + c + " z=" + z + " t=" + t,
              (short[]) imp.getStack().getPixels(imp.getStackIndex(c, z, t)),
              (short[]) read.getStack().getPixels(read.getStackIndex(c, z, t)));
  }

  @Test
  public void channelNamesCanBeGivenAsAList() throws Exception
  {
    final ImageStack stack = new ImageStack(W, H);
    stack.addSlice(makeProcessor(ImagePlus.GRAY8, 1));
    stack.addSlice(makeProcessor(ImagePlus.GRAY8, 2));
    final ImagePlus imp = new ImagePlus("two-channel", stack);
    imp.setDimensions(2, 1, 1);

    final String file = path("named.h5");
    HDF5ImageJ.hdf5write(imp, file, "/{c}", "", "dapi,gfp", 0);

    final ArrayList<String> paths = new ArrayList<String>();
    for (final DataSetInfo info : HDF5ImageJ.hdf5list(file)) paths.add(info.path);
    assertEquals(2, paths.size());
    assertTrue(paths.contains("/dapi"));
    assertTrue(paths.contains("/gfp"));
  }

  @Test
  public void customLayoutReadsTransposedDatasets() throws Exception
  {
    // write zyx, then ask for it back as a stack of xzy-ordered planes
    final ImagePlus imp = makeImage(ImagePlus.GRAY8, 3);
    final String file = path("layout.h5");
    HDF5ImageJ.hdf5write(imp, file, "/data");

    final ImagePlus same = HDF5ImageJ.hdf5read(file, "/data", "zyx");
    assertSamePixels(imp, same);

    // reading with layout "yzx" swaps the roles of z and y
    final ImagePlus swapped = HDF5ImageJ.hdf5read(file, "/data", "yzx");
    assertNotNull(swapped);
    assertEquals(W, swapped.getWidth());
    assertEquals(3, swapped.getHeight());
    assertEquals(H, swapped.getNSlices());
    for (int y = 0; y < H; y++)
      for (int z = 0; z < 3; z++)
        for (int x = 0; x < W; x++)
          assertEquals("y=" + y + " z=" + z + " x=" + x,
              ((byte[]) imp.getStack().getPixels(z + 1))[y * W + x],
              ((byte[]) swapped.getStack().getPixels(y + 1))[z * W + x]);
  }
}
