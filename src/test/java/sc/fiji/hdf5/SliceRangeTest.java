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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class SliceRangeTest
{
  private static void assertSlices(final String spec, final int n, final int... expected)
  {
    assertArrayEquals("'" + spec + "' of " + n, expected, SliceRange.parse(spec, n));
  }

  private static void assertRejected(final String spec, final int n)
  {
    try
    {
      SliceRange.parse(spec, n);
      fail("'" + spec + "' should have been rejected");
    }
    catch (final IllegalArgumentException expected)
    {
      assertTrue(expected.getMessage(), expected.getMessage().length() > 0);
    }
  }

  @Test
  public void emptyMeansAll()
  {
    assertSlices("", 4, 1, 2, 3, 4);
    assertSlices("   ", 3, 1, 2, 3);
    assertSlices(null, 2, 1, 2);
    assertFalse(SliceRange.isGiven(null));
    assertFalse(SliceRange.isGiven("  "));
    assertTrue(SliceRange.isGiven("5"));
  }

  @Test
  public void singleSlice()
  {
    assertSlices("5", 10, 5);
    assertSlices("1", 1, 1);
    assertSlices(" 10 ", 10, 10);
  }

  @Test
  public void ranges()
  {
    assertSlices("3-6", 10, 3, 4, 5, 6);
    assertSlices("4-4", 10, 4);
    assertSlices("8-", 10, 8, 9, 10);
    assertSlices("1-10", 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
  }

  @Test
  public void steps()
  {
    assertSlices("10-20-3", 30, 10, 13, 16, 19);
    assertSlices("1-9-4", 9, 1, 5, 9);
    assertSlices("2-3-5", 10, 2);
  }

  @Test
  public void everyNth()
  {
    assertSlices("%3", 10, 1, 4, 7, 10);
    assertSlices("%1", 3, 1, 2, 3);
    assertSlices("%5", 3, 1);
  }

  @Test
  public void lists()
  {
    assertSlices("1,5,10-12", 20, 1, 5, 10, 11, 12);
    assertSlices("5,1", 20, 5, 1); // the order given
    assertSlices("2-4,3-5", 10, 2, 3, 4, 5); // without repeats
    assertSlices("1, 3 , %4", 8, 1, 3, 5);
  }

  @Test
  public void malformed()
  {
    assertRejected("abc", 10);
    assertRejected("1-2-3-4", 10);
    assertRejected("5-3", 10);
    assertRejected("1,,2", 10);
    assertRejected("1,", 10);
    assertRejected("%", 10);
    assertRejected("%x", 10);
    assertRejected("-5", 10);
    assertRejected("1-5-0", 10);
    assertRejected("0", 10);
    assertRejected("%0", 10);
  }

  @Test
  public void outsideTheStack()
  {
    assertRejected("11", 10);
    assertRejected("5-11", 10);
    assertRejected("1,11", 10);
    assertRejected("1", 0);
  }

  @Test
  public void messageNamesTheProblem()
  {
    try
    {
      SliceRange.parse("5-11", 10);
      fail();
    }
    catch (final IllegalArgumentException e)
    {
      assertEquals("Slices '5-11' are outside 1 to 10", e.getMessage());
    }
  }
}
