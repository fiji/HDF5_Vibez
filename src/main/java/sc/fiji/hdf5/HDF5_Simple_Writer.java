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
// written by: Olaf Ronneberger (ronneber@informatik.uni-freiburg.de)
// Copyright: GPL v2
//

package sc.fiji.hdf5;

import ij.ImagePlus;
import ij.plugin.*;
import ij.plugin.filter.PlugInFilter;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.process.ImageProcessor;
import ij.Prefs;

public class HDF5_Simple_Writer implements PlugInFilter
{
  String _saveMode;  // either "replace" or "append"
  ImagePlus _imp;

  public int setup(String arg, ImagePlus imp) 
  {
    _saveMode = arg;
    _imp = imp;
    return DOES_8G + DOES_8C + DOES_16 + DOES_32 + DOES_RGB + NO_CHANGES;
  }
  
  public void run(ImageProcessor ip) 
  {
    String filename;
    if( _saveMode.equals("append")) 
    {
      OpenDialog sd = new OpenDialog("Save to HDF5 (append) ...", OpenDialog.getLastDirectory(), "");
      String directory = sd.getDirectory();
      String name = sd.getFileName();
      if (name == null)
          return;
      if (name == "")
          return;
      filename = directory + name;
    }
    else
    {
      SaveDialog sd = new SaveDialog("Save to HDF5 (new or replace)...", OpenDialog.getLastDirectory(), ".h5");
      String directory = sd.getDirectory();
      String name = sd.getFileName();
      if (name == null)
          return;
      if (name == "")
          return;
      filename = directory + name;
    }
    
    GenericDialog gd = new GenericDialog("Save HDF5");
    gd.addMessage("Data set name template (for hyperstacks should contain placeholders '{t}' and '{c}')" );
    String dsetNameTemplate = (String)Prefs.get("hdf5writervibez.nametemplate", "/t{t}/channel{c}");
    gd.addStringField( "dsetnametemplate", dsetNameTemplate, 128);


    gd.addMessage("Format string for frame number. Either printf syntax (e.g. '%d') or comma-separated list of names." );
    String formatTime = (String)Prefs.get("hdf5writervibez.timeformat", "%d");
    gd.addStringField( "formattime", formatTime, 128);

    gd.addMessage("Format string for channel number. Either printf syntax (e.g. '%d') or comma-separated list of names." );
    String formatChannel = (String)Prefs.get("hdf5writervibez.timeformat", "%d");
    gd.addStringField( "formatchannel", formatChannel, 128);

    gd.addMessage("Compression level (0-9)");
    int compressionLevel = (int)Prefs.get("hdf5writervibez.compressionlevel", 0);
    gd.addNumericField( "compressionlevel", compressionLevel, 0);
    gd.showDialog();
    if (gd.wasCanceled()) return;
    
    dsetNameTemplate = gd.getNextString();
    formatTime = gd.getNextString();
    formatChannel = gd.getNextString();
    compressionLevel = (int)(gd.getNextNumber());

    HDF5ImageJ.saveHyperStack( _imp, filename, dsetNameTemplate, 
                               formatTime, formatChannel, 
                               compressionLevel, _saveMode);
  }
}
