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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

/** Tests for the small pure helpers in {@link HDF5ImageJ} and {@link DataSetInfo}. */
public class HDF5ImageJHelpersTest
{
  @Test
  public void createNameListFromPrintfFormat()
  {
    assertArrayEquals(new String[] {"0", "1", "2"}, HDF5ImageJ.createNameList("%d", 3));
    assertArrayEquals(new String[] {"00", "01"}, HDF5ImageJ.createNameList("%02d", 2));
  }

  @Test
  public void createNameListOnlyTreatsLeadingPercentAsFormat()
  {
    // "t%d" does not start with '%', so it is a one-token list that gets padded with numbers
    assertArrayEquals(new String[] {"t%d", "1"}, HDF5ImageJ.createNameList("t%d", 2));
  }

  @Test
  public void createNameListFromTokens()
  {
    assertArrayEquals(new String[] {"a", "b"}, HDF5ImageJ.createNameList("a,b", 2));
    assertArrayEquals(new String[] {"a", "b", "c"}, HDF5ImageJ.createNameList("a b  c", 3));
  }

  @Test
  public void createNameListPadsMissingTokensWithNumbers()
  {
    assertArrayEquals(new String[] {"a", "1", "2"}, HDF5ImageJ.createNameList("a", 3));
  }

  @Test
  public void bitdepthForSupportedTypes()
  {
    assertEquals(8, HDF5ImageJ.assignHDF5TypeToImagePlusBitdepth("uint8", false));
    assertEquals(24, HDF5ImageJ.assignHDF5TypeToImagePlusBitdepth("uint8", true));
    assertEquals(16, HDF5ImageJ.assignHDF5TypeToImagePlusBitdepth("uint16", false));
    assertEquals(16, HDF5ImageJ.assignHDF5TypeToImagePlusBitdepth("int16", false));
    assertEquals(32, HDF5ImageJ.assignHDF5TypeToImagePlusBitdepth("float32", false));
    assertEquals(32, HDF5ImageJ.assignHDF5TypeToImagePlusBitdepth("float64", false));
  }

  @Test
  public void dataSetInfoSortsNumbersNumerically()
  {
    final ArrayList<DataSetInfo> infos = new ArrayList<DataSetInfo>();
    for (final String p : new String[] {"/t10", "/t2", "/t1"})
      infos.add(new DataSetInfo(p, "", "", ""));
    Collections.sort(infos, DataSetInfo.createComparator());
    assertEquals("/t1", infos.get(0).path);
    assertEquals("/t2", infos.get(1).path);
    assertEquals("/t10", infos.get(2).path);
  }

  @Test
  public void dataSetInfoPadsNumbersInSortKey()
  {
    final DataSetInfo info = new DataSetInfo("/a7", "", "", "");
    assertTrue(info.numericSortablePath.startsWith("/a"));
    assertTrue(info.numericSortablePath.endsWith("0000000007"));
  }
}
