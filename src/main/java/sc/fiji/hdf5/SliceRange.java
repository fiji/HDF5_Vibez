package sc.fiji.hdf5;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Chooses slices of a stack from a short text such as {@code 5}, {@code 10-20}, {@code 10-20-3},
 * {@code 10-} or {@code %3}. Slices are numbered from 1, as in ImageJ.
 *
 * <ul>
 * <li>{@code 5}: slice 5</li>
 * <li>{@code 10-20}: slices 10 to 20</li>
 * <li>{@code 10-20-3}: every third slice from 10 to 20 (10, 13, 16, 19)</li>
 * <li>{@code 10-}: slice 10 to the last</li>
 * <li>{@code %3}: every third slice, starting at the first</li>
 * <li>{@code 1,5,10-12}: several of these, in the order given</li>
 * <li>empty: all slices</li>
 * </ul>
 */
public final class SliceRange
{
  private SliceRange() {}

  /** @return true if {@code spec} selects slices, false if it is empty (meaning all of them) */
  public static boolean isGiven(final String spec)
  {
    return spec != null && !spec.trim().isEmpty();
  }

  /**
   * @param spec the text to interpret
   * @param n the number of slices in the stack
   * @return the selected slices, numbered from 1, without repeats
   * @throws IllegalArgumentException if {@code spec} is malformed or selects a slice outside 1 to n
   */
  public static int[] parse(final String spec, final int n)
  {
    final Set<Integer> slices = new LinkedHashSet<Integer>();
    if (!isGiven(spec))
    {
      for (int i = 1; i <= n; i++) slices.add(i);
      return toArray(slices);
    }
    for (final String raw : spec.split(",", -1))
    {
      final String part = raw.trim();
      if (part.isEmpty()) throw new IllegalArgumentException("Empty entry in slices '" + spec + "'");
      if (part.startsWith("%"))
      {
        final int step = number(part.substring(1), spec);
        for (int i = 1; i <= n; i += step) slices.add(i);
        continue;
      }
      final String[] fields = part.split("-", -1);
      if (fields.length > 3) throw new IllegalArgumentException("Cannot read '" + part + "' in slices '" + spec + "'");
      final int first = number(fields[0], spec);
      final int last;
      if (fields.length == 1) last = first;
      else if (fields[1].isEmpty() && fields.length == 2) last = n;
      else last = number(fields[1], spec);
      final int step = fields.length == 3 ? number(fields[2], spec) : 1;
      if (first > last) throw new IllegalArgumentException("Slice range '" + part + "' ends before it starts");
      if (first < 1 || last > n)
        throw new IllegalArgumentException("Slices '" + part + "' are outside 1 to " + n);
      for (int i = first; i <= last; i += step) slices.add(i);
    }
    return toArray(slices);
  }

  private static int number(final String text, final String spec)
  {
    final int value;
    try
    {
      value = Integer.parseInt(text.trim());
    }
    catch (final NumberFormatException e)
    {
      throw new IllegalArgumentException("Cannot read '" + text.trim() + "' as a number in slices '" + spec + "'");
    }
    if (value < 1) throw new IllegalArgumentException("Slice numbers and steps start at 1 in '" + spec + "'");
    return value;
  }

  private static int[] toArray(final Set<Integer> slices)
  {
    final List<Integer> list = new ArrayList<Integer>(slices);
    final int[] array = new int[list.size()];
    for (int i = 0; i < array.length; i++) array[i] = list.get(i);
    return array;
  }
}
