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

import ij.plugin.*;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.Prefs;


public class HDF5_Simple_Reader implements PlugIn 
{
  public void run(String arg) {
    OpenDialog od = new OpenDialog("Load HDF5","","");
    String filename = od.getPath();
    

    GenericDialog gd = new GenericDialog("Load HDF5");
    gd.addMessage("Comma-separated or space-separated list of dataset names.");
    
    String commaSeparatedDsetNames = (String)Prefs.get("hdf5readervibez.dsetnames", "");
    gd.addStringField( "datasetnames", commaSeparatedDsetNames, 128);
    int nFrames = (int)Prefs.get("hdf5readervibez.nframes", 1);
    gd.addNumericField( "nframes", nFrames, 0);
    int nChannels = (int)Prefs.get("hdf5readervibez.nchannels", 1);
    gd.addNumericField( "nchannels", nChannels, 0);
    gd.showDialog();
    if (gd.wasCanceled()) return;

    String datasetnames = gd.getNextString();
    int nframes =  (int)(gd.getNextNumber());
    int nchannels = (int)(gd.getNextNumber());

    HDF5ImageJ.loadDataSetsToHyperStack( filename, 
                                         datasetnames.split(","),
                                         nframes,
                                         nchannels);
    
  }
}
