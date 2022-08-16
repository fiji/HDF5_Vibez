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

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.process.FloatProcessor;
import ij.plugin.ContrastEnhancer;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// import hdf.hdf5lib.exceptions.HDF5Exception;
// //import hdf.hdflib.HDFException;
// import hdf.object.Attribute;
// import hdf.object.Dataset;
// import hdf.object.Datatype;
// import hdf.object.Group;
// import hdf.object.HObject;
// import hdf.object.Metadata;
// import hdf.object.h5.H5Datatype;
// import hdf.object.h5.H5File;

//import org.apache.commons.lang.ArrayUtils;

import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5ReaderConfigurator;
import hdf.hdf5lib.exceptions.HDF5Exception;
import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;


public class Vibez_Validate implements PlugIn 
{
  public void run(String arg) 
  {
    String directory = "";
    String name = "";
    boolean tryAgain;
    String openMSG = "Open HDF5...";
    do 
    {
      tryAgain = false;
      OpenDialog od;
      if (directory.equals(""))
          od = new OpenDialog(openMSG, arg);
      else
          od = new OpenDialog(openMSG, directory, arg);
      
      directory = od.getDirectory();
      name = od.getFileName();
      if (name == null)
          return;
      if (name == "")
          return;
      
      File testFile = new File(directory + name);
      if (!testFile.exists() || !testFile.canRead())
          return;
      
      if (testFile.isDirectory()) {
        directory = directory + name;
        tryAgain = true;
      }
    } while (tryAgain);

    String filename = directory + name;
    
    GenericDialog gd = new GenericDialog("Select Data Sets");
    gd.addCheckboxGroup( 5, 1,  
                         new String[]{ 
                             "step0 (raw data)", 
                             "step1 (fused HDR)",
                             "step2 (stitched)",
                             "step3 (ventral registered)",
                             "step4 (fused with attenuation correction)"},
                         new boolean[]{
                             false,
                             false,
                             false,
                             false,
                             false});
    gd.showDialog();
    if (gd.wasCanceled()) return;

    //
    //  Step 0
    //
    if (gd.getNextBoolean())
    {
      create_collages( 
          "step0 (raw data)", 
          filename, 4, 4, 
          new String[]{
              "/step0/raw/dorsal/tile0/laserset0/channel0",
              "/step0/raw/dorsal/tile0/laserset0/channel1",
              "/step0/raw/dorsal/tile0/laserset1/channel0",
              "/step0/raw/dorsal/tile0/laserset1/channel1",
              "/step0/raw/dorsal/tile1/laserset0/channel0",
              "/step0/raw/dorsal/tile1/laserset0/channel1",
              "/step0/raw/dorsal/tile1/laserset1/channel0",
              "/step0/raw/dorsal/tile1/laserset1/channel1",
              "/step0/raw/ventral/tile0/laserset0/channel0",
              "/step0/raw/ventral/tile0/laserset0/channel1",
              "/step0/raw/ventral/tile0/laserset1/channel0",
              "/step0/raw/ventral/tile0/laserset1/channel1",
              "/step0/raw/ventral/tile1/laserset0/channel0",
              "/step0/raw/ventral/tile1/laserset0/channel1",
              "/step0/raw/ventral/tile1/laserset1/channel0",
              "/step0/raw/ventral/tile1/laserset1/channel1"}, 
          new int[][]{
              {0, 0}, 
              {0, 2}, 
              {0, 1}, 
              {0, 3}, 
              {1, 0}, 
              {1, 2}, 
              {1, 1}, 
              {1, 3}, 
              {2, 0}, 
              {2, 2}, 
              {2, 1}, 
              {2, 3}, 
              {3, 0}, 
              {3, 2}, 
              {3, 1}, 
              {3, 3}}
                       );
    }
    
    //
    //  Step 1
    //
   if (gd.getNextBoolean())
    {
      create_collages( 
          "step1 (fused HDR)", 
          filename, 4, 2, 
          new String[]{
              "/step1/hdr/dorsal/tile0/channel0",
              "/step1/hdr/dorsal/tile0/channel1",
              "/step1/hdr/dorsal/tile1/channel0",
              "/step1/hdr/dorsal/tile1/channel1",
              "/step1/hdr/ventral/tile0/channel0",
              "/step1/hdr/ventral/tile0/channel1",
              "/step1/hdr/ventral/tile1/channel0",
              "/step1/hdr/ventral/tile1/channel1"},
          new int[][]{
              {0, 0}, 
              {0, 1}, 
              {1, 0}, 
              {1, 1}, 
              {2, 0}, 
              {2, 1}, 
              {3, 0}, 
              {3, 1}}
                       );
    }
    
    //
    //  Step 2
    //
   if (gd.getNextBoolean())
    {
      create_collages( 
          "step2 (stitched)", 
          filename, 2, 2, 
          new String[]{
              "/step2/stitched/dorsal/channel0",
              "/step2/stitched/dorsal/channel1",
              "/step2/stitched/ventral/channel0",
              "/step2/stitched/ventral/channel1"},
          new int[][]{
              {0, 0}, 
              {0, 1}, 
              {1, 0}, 
              {1, 1}}
                       );
    }
    
    
    //
    //  Step 3
    //
   if (gd.getNextBoolean())
    {
      create_collages( 
          "step3 (ventral registered)", 
          filename, 2, 2, 
          new String[]{
              "/step2/stitched/dorsal/channel0",
              "/step2/stitched/dorsal/channel1",
              "/step3/registered/ventral/channel0",
              "/step3/registered/ventral/channel1"},
          new int[][]{
              {0, 0}, 
              {0, 1}, 
              {1, 0}, 
              {1, 1}}
                       );
    }
    
    //
    //  Step 4
    //
   if (gd.getNextBoolean())
    {
      create_collages( 
          "step4 (fused att corr)", 
          filename, 1, 2, 
          new String[]{
              "/step4/fused/channel0",
              "/step4/fused/channel1"},
          new int[][]{
              {0, 0}, 
              {0, 1}}
                       );
    }
    
    
    
  }
  


  void create_collages(  String windowtitle, String filename,
                         int grid_nx, int grid_ny,
                         String[] dsetNameList, int[][] gpos_xy)
  {
    IJ.showStatus("Loading HDF5 File: " + filename);
    
    String[] errors = new String[ dsetNameList.length];

    try
    {
      IHDF5ReaderConfigurator conf = HDF5Factory.configureForReading(filename);
      conf.performNumericConversions();
      IHDF5Reader reader = conf.reader();
      
      //
      // get dimensions of all data sets
      //
      int maxNLevs = 0;
      int maxNRows = 0;
      int maxNCols = 0;
      
      for( int i = 0; i < dsetNameList.length; ++i)
      {
        errors[i] = "";
        
        if( reader.exists( dsetNameList[i]) == false)
        {
          IJ.log( "ERROR: dataset " + dsetNameList[i] + " does not exist\n");
          
          errors[i] += "Does not exist\n";
          continue;
        } 
          
        HDF5DataSetInformation info = reader.getDataSetInformation( dsetNameList[i]);
        if( info.getRank() != 3)
        {
          IJ.log( "ERROR: dataset " + dsetNameList[i] + " has wrong rank: " + info.getRank() + "\n");
          errors[i] += "wrong rank: " + info.getRank() + "\n";
          continue;
        } 
        long[] dims = info.getDimensions();
        IJ.log( "dataset " + dsetNameList[i] + " has dimensions " 
                + dims[0] + "x"
                + dims[1] + "x"
                + dims[2] + "\n");
        
        maxNLevs = Math.max( maxNLevs, (int)dims[0]);
        maxNRows = Math.max( maxNRows, (int)dims[1]);
        maxNCols = Math.max( maxNCols, (int)dims[2]);
      }
      IJ.log( "Max dimensions: " 
              + maxNLevs + "x"
              + maxNRows + "x"
              + maxNCols + "\n");
 
      // 
      // Create the Collages
      //
      int gridsize_x = maxNCols + 10;
      int gridsize_y = maxNRows + 10;
      int gridsize_z = maxNLevs + 10;
      
      float maxGray = 255;
      ImageProcessor collage_z    = new FloatProcessor( grid_nx*gridsize_x, grid_ny*gridsize_y);
      ImageProcessor collage_zmip = new FloatProcessor( grid_nx*gridsize_x, grid_ny*gridsize_y);
      ImageProcessor collage_y    = new FloatProcessor( grid_nx*gridsize_x, grid_ny*gridsize_z);
      ImageProcessor collage_ymip = new FloatProcessor( grid_nx*gridsize_x, grid_ny*gridsize_z);

      collage_z.setValue(maxGray/2);    collage_z.fill();
      collage_zmip.setValue(maxGray/2); collage_zmip.fill();
      collage_y.setValue(maxGray/2);    collage_y.fill();
      collage_ymip.setValue(maxGray/2); collage_ymip.fill();
      
      ImagePlus impCollage_z    = new ImagePlus( windowtitle + " collage Z (central slice)", collage_z);
      ImagePlus impCollage_zmip = new ImagePlus( windowtitle + " collage Z (MIP)", collage_zmip);
      ImagePlus impCollage_y    = new ImagePlus( windowtitle + " collage Y (central slice)", collage_y);
      ImagePlus impCollage_ymip = new ImagePlus( windowtitle + " collage Y (MIP)", collage_ymip);

      impCollage_z.setDisplayRange(0,255);
      impCollage_zmip.setDisplayRange(0,255);
      impCollage_y.setDisplayRange(0,255);
      impCollage_ymip.setDisplayRange(0,255);

      impCollage_y.show();
      impCollage_ymip.show();
      impCollage_z.show();
      impCollage_zmip.show();
      
      //
      // load datasets and insert into collages
      //
      float textColor = maxGray;
      
      collage_z.setValue(textColor);
      collage_zmip.setValue(textColor);
      collage_y.setValue(textColor);
      collage_ymip.setValue(textColor);

      for( int i = 0; i < dsetNameList.length; ++i)
      {
        if( errors[i].equals( ""))
        {
          IJ.log( "loading dataset " + dsetNameList[i] + "\n");
          
          MDFloatArray rawdata = reader.float32().readMDArray(dsetNameList[i]);
          int nLevels = rawdata.dimensions()[0];
          int nRows = rawdata.dimensions()[1];
          int nCols = rawdata.dimensions()[2];
          float[] flatArray = rawdata.getAsFlatArray();

          // extract central slice in z direction
          ImageProcessor slice_z = new FloatProcessor( nCols, nRows);
          float[] flatSlice_z = (float[]) slice_z.getPixels();
          
          System.arraycopy( flatArray, (nLevels/2)*nRows*nCols, 
                            flatSlice_z, 0, 
                            nRows*nCols);

          collage_z.insert( slice_z, 
                            gpos_xy[i][0]*gridsize_x, 
                            gpos_xy[i][1]*gridsize_y);

          // extract central slice in y direction
          ImageProcessor slice_y = new FloatProcessor( nCols, nLevels);
          float[] flatSlice_y = (float[]) slice_y.getPixels();
          int centralRow = nRows/2;
          
          for( int lev = 0; lev < nLevels; ++lev)
          {
            for( int col = 0; col < nCols; ++col)
            {
              flatSlice_y[lev*nCols+col] = flatArray[(lev*nRows + centralRow)*nCols + col];
            }
          }
          collage_y.insert( slice_y, 
                            gpos_xy[i][0]*gridsize_x, 
                            gpos_xy[i][1]*gridsize_z);
          

          // compute mips in z- and y-direction
          ImageProcessor mip_z = new FloatProcessor( nCols, nRows);
          ImageProcessor mip_y = new FloatProcessor( nCols, nLevels);
          float[] flatMip_z = (float[]) mip_z.getPixels();
          float[] flatMip_y = (float[]) mip_y.getPixels();
         
          for( int lev = 0; lev < nLevels; ++lev)
          {
            for( int row = 0; row < nRows; ++row)
            {
              for( int col = 0; col < nCols; ++col)
              {
                float g = flatArray[(lev*nRows + row)*nCols + col];
                if( g > flatMip_z[row*nCols+col]) flatMip_z[row*nCols+col] = g;
                if( g > flatMip_y[lev*nCols+col]) flatMip_y[lev*nCols+col] = g;
              }
            }
          }
          collage_zmip.insert( mip_z, 
                               gpos_xy[i][0]*gridsize_x, 
                               gpos_xy[i][1]*gridsize_y);

          collage_ymip.insert( mip_y, 
                               gpos_xy[i][0]*gridsize_x, 
                               gpos_xy[i][1]*gridsize_z);
        }

        // annotate the images and redraw
        //
        int fontheight = collage_z.getFont().getSize();
        int textoffset = 2;
        
        collage_z.drawString(dsetNameList[i], 
                             gpos_xy[i][0]*gridsize_x + textoffset, 
                             gpos_xy[i][1]*gridsize_y + textoffset + fontheight);
        collage_zmip.drawString(dsetNameList[i], 
                                gpos_xy[i][0]*gridsize_x + textoffset, 
                                gpos_xy[i][1]*gridsize_y + textoffset + fontheight);
        collage_y.drawString(dsetNameList[i], 
                             gpos_xy[i][0]*gridsize_x + textoffset, 
                             gpos_xy[i][1]*gridsize_z + textoffset + fontheight);
        collage_ymip.drawString(dsetNameList[i], 
                                gpos_xy[i][0]*gridsize_x + textoffset, 
                                gpos_xy[i][1]*gridsize_z + textoffset + fontheight);
          
        ContrastEnhancer ce = new ContrastEnhancer();
        ce.stretchHistogram(impCollage_z,0.4);
        ce.stretchHistogram(impCollage_y,0.4);
        ce.stretchHistogram(impCollage_zmip,0.4);
        ce.stretchHistogram(impCollage_ymip,0.4);

        impCollage_z.updateAndDraw();
        impCollage_y.updateAndDraw();
        impCollage_zmip.updateAndDraw();
        impCollage_ymip.updateAndDraw();
         
      }
      
      
    }
    
    catch (HDF5Exception err) 
    {
      IJ.error("Error while opening '" + filename + "':\n"
               + err);
    } 
    catch (Exception err) 
    {
      IJ.error("Error while opening '" + filename + "':\n"
               + err);
    } 
    catch (OutOfMemoryError o) 
    {
      IJ.outOfMemory("Load HDF5");
    }
    
    
//
//    
//    ImageProcessor ip = new ShortProcessor( 
//    for( String dsetName : dsetNameList)
//    {
//      loadDataSetToImagePlus_Byte( directory + name, dsetName);
//    }
  }
  

  ImagePlus loadDataSetToImagePlus_Byte( String filename, String dsetName)
        {
          try
          {
            IHDF5ReaderConfigurator conf = HDF5Factory.configureForReading(filename);
            conf.performNumericConversions();
            IHDF5Reader reader = conf.reader();
            MDByteArray rawdata = reader.uint8().readMDArray(dsetName);
            float[] element_size_um = reader.float32().getArrayAttr(dsetName, "element_size_um");
        
            reader.close();
          
            System.out.println( "dimensions: " 
                                + rawdata.dimensions()[0] + 
                                "," + rawdata.dimensions()[1] +
                                "," + rawdata.dimensions()[2]);
          
            // create a new image stack and fill in the data
            int nLevels = rawdata.dimensions()[0];
            int nRows = rawdata.dimensions()[1];
            int nCols = rawdata.dimensions()[2];
          
            ImageStack stack = new ImageStack(nCols, nRows, nLevels);
            long stackSize = nCols * nRows;
            byte[] flatArray = rawdata.getAsFlatArray();
            for( int lev = 0; lev < nLevels; ++lev)
            {
              byte[] slice = new byte[nRows*nCols];
            
              System.arraycopy( flatArray, lev*nRows*nCols, 
                                slice, 0, 
                                nRows*nCols);
              stack.setPixels(slice, lev+1);
            }
            ImagePlus imp = new ImagePlus( "test", stack);
            imp.getCalibration().pixelDepth  = element_size_um[0];
            imp.getCalibration().pixelHeight = element_size_um[1];
            imp.getCalibration().pixelWidth  = element_size_um[2];
            imp.getCalibration().setUnit("micrometer");
            imp.setDisplayRange(0,255);
        
          
            imp.show();
            return imp;
          }
       
          catch (HDF5Exception err) 
          {
            IJ.error("Error while opening '" + filename 
                     + "', dataset '" + dsetName + "':\n"
                     + err);
          } 
          catch (Exception err) 
          {
            IJ.error("Error while opening '" + filename 
                     + "', dataset '" + dsetName + "':\n"
                     + err);
          } 
          catch (OutOfMemoryError o) 
          {
            IJ.outOfMemory("Load HDF5");
          }
          return null;
          
        }
  
}
