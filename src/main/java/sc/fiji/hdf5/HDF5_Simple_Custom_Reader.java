/*-
 * #%L
 * HDF5 plugin for ImageJ and Fiji.
 * %%
 * Copyright (C) 2011 - 2017 Fiji developers.
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
// written by: Kyle M. Douglass (kyle.douglass@epfl.ch)
// Copyright: GPL v2
//

package sc.fiji.hdf5;

import ij.plugin.*;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.Prefs;

public class HDF5_Simple_Custom_Reader implements PlugIn 
{
  public void run(String arg) {
    OpenDialog od = new OpenDialog("Load HDF5 with custom layout","","");
    String filename = od.getPath();
    

    GenericDialog gd = new GenericDialog("Load HDF5: custom layout");
    gd.addMessage("Please specify the dataset name and layout.");
    
    gd.addStringField( "datasetname", "/path/to/dataset", 128);
    gd.addStringField( "dataset layout", "yx", 5);
    gd.showDialog();
    if (gd.wasCanceled()) return;

    String datasetnames = gd.getNextString();
    String dsetLayout   = gd.getNextString();

    HDF5ImageJ.loadCustomLayoutDataSetToHyperStack(
            filename, datasetnames, dsetLayout);
    
  }
}
