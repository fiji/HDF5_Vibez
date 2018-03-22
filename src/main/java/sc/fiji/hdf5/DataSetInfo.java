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
