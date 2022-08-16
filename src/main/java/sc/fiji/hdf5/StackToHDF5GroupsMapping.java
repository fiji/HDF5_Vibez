/*-
 * #%L
 * HDF5 plugin for ImageJ and Fiji.
 * %%
 * Copyright (C) 2011 - 2022 Fiji developers.
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
//
// Part of the HDF5 plugin for ImageJ
// written by: Olaf Ronneberger (ronneber@informatik.uni-freiburg.de)
// Copyright: GPL v2
//

package sc.fiji.hdf5;

public class StackToHDF5GroupsMapping
{
  String uniqueName_;
  String formatString_;
  String formatT_;
  String formatC_;
  
  public StackToHDF5GroupsMapping( String paramInOneLine) {
    String[] tokens = paramInOneLine.split("[\\s]*,[\\s]*");
    uniqueName_   = tokens[0];
    formatString_ = tokens[1];
    formatT_      = tokens[2];
    formatC_      = tokens[3];
  }
  

  public String toString() {
    return uniqueName_ + ","
        + formatString_ + ","
        + formatT_ + ","
        + formatC_;
  }
}

  
  
