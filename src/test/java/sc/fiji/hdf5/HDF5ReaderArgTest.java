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
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** The reader plugin can be called with the path of a file to open (#12). */
public class HDF5ReaderArgTest
{
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void emptyArgumentsShowTheDialog()
  {
    assertNull(HDF5_Reader_Vibez.fileFromArg(null));
    assertNull(HDF5_Reader_Vibez.fileFromArg(""));
    assertNull(HDF5_Reader_Vibez.fileFromArg("   "));
  }

  @Test
  public void missingFileOrDirectoryShowsTheDialog() throws IOException
  {
    assertNull(HDF5_Reader_Vibez.fileFromArg(new File(tmp.getRoot(), "missing.h5").getPath()));
    assertNull(HDF5_Reader_Vibez.fileFromArg(tmp.getRoot().getPath()));
  }

  @Test
  public void acceptsAnyHdf5FileExtension() throws IOException
  {
    for (final String name : new String[] {"a.h5", "a.hdf5", "a.hdf", "a.he5", "a.jhdf"})
    {
      final File f = tmp.newFile(name);
      assertEquals(name, f, HDF5_Reader_Vibez.fileFromArg(f.getPath()));
    }
  }

  @Test
  public void trimsWhitespace() throws IOException
  {
    final File f = tmp.newFile("padded.h5");
    assertEquals(f, HDF5_Reader_Vibez.fileFromArg("  " + f.getPath() + "\n"));
  }

  @Test
  public void resolvesRelativePaths() throws IOException
  {
    final File f = new File("pom.xml");
    assertEquals(f.getAbsoluteFile(), HDF5_Reader_Vibez.fileFromArg("pom.xml"));
  }
}
