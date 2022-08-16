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
package sc.fiji.hdf5;

import ij.IJ;

import java.util.Comparator;

class DataSetInfo
{
    static class DataSetInfoComparator implements Comparator<DataSetInfo> {
        public int compare(DataSetInfo a, DataSetInfo b) {
            return a.numericSortablePath.compareTo( b.numericSortablePath);
        }
    }

    public String path;
    public String numericSortablePath;
    public String dimText;
    public String typeText;
    public String element_size_um_text;
    final int numPaddingSize = 10;

    public DataSetInfo( String p, String d, String t, String e) {
        setPath(p);
        dimText = d;
        typeText = t;
        element_size_um_text = e;
    }

    public void setPath( String p) {
        path = p;
        numericSortablePath = "";
        String num = "";
        for( int i = 0; i < p.length(); ++i) {
            if (isNum(p.charAt(i))) {
                num += p.charAt(i);
            } else {
                if (num != "") {
                    for (int j = 0; j < numPaddingSize - num.length(); ++j) {
                        numericSortablePath += "0";
                    }
                    numericSortablePath += num;
                    num = "";
                }
                numericSortablePath += p.charAt(i);
            }
        }
        if (num != "") {
            for (int j = 0; j < numPaddingSize - num.length(); ++j) {
                numericSortablePath += "0";
            }
            numericSortablePath += num;
        }
        IJ.log( path);
        IJ.log( numericSortablePath);
    }

    public static Comparator<DataSetInfo> createComparator()
    {
        return new DataSetInfoComparator();
    }

    private boolean isNum( char c) {
        return c >= '0' && c <= '9';
    }
}
