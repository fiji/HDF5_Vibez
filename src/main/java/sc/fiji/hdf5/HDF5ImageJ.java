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

import ch.systemsx.cisd.hdf5.*;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import org.apache.commons.lang.NotImplementedException;

import java.awt.HeadlessException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class HDF5ImageJ
{
  //-----------------------------------------------------------------------------
  public static ImagePlus hdf5read( String filename, String datasetname)
  {
    String[] dsetNames = new String[1];
    dsetNames[0] = datasetname;
    return loadDataSetsToHyperStack( filename, dsetNames, 1, 1);
  }

  public static ImagePlus hdf5read( String filename, String[] datasets, int nFrames, int nChannels)
  {
    return loadDataSetsToHyperStack( filename, datasets, nFrames, nChannels, false);
  }

  public static ImagePlus hdf5read( String filename, String datasetname, String layout)
  {
    String[] dsetNames = new String[1];
    dsetNames[0] = datasetname;
    return loadCustomLayoutDataSetToHyperStack( filename, datasetname, layout, false);
  }

  public static void hdf5write( String filename, String datasetname)
  {
    saveHyperStack( IJ.getImage(), filename, datasetname, "", "", 0, "replace");
  }

  public static void hdf5write( ImagePlus imp, String filename, String datasetname)
  {
    saveHyperStack( imp, filename, datasetname, "", "", 0, "replace");
  }

  public static void hdf5write( ImagePlus imp, String filename, String datasetname, boolean replace)
  {
    if (replace) {
      saveHyperStack( imp, filename, datasetname, "", "", 0, "replace");
    } else {
      saveHyperStack( imp, filename, datasetname, "", "", 0, "append");
    }
  }

  public static void hdf5write( ImagePlus imp, String filename, String datasetname, String formatTime, String formatChannel, int compressionLevel)
  {
    saveHyperStack( imp, filename, datasetname, formatTime, formatChannel, compressionLevel, "replace");
  }

  public static void hdf5write( ImagePlus imp, String filename, String datasetname, String formatTime, String formatChannel, int compressionLevel, boolean replace)
  {
    if (replace) {
      saveHyperStack( imp, filename, datasetname, formatTime, formatChannel, compressionLevel, "replace");
    } else {
      saveHyperStack( imp, filename, datasetname, formatTime, formatChannel, compressionLevel, "append");
    }
  }

  public static ArrayList<DataSetInfo> hdf5list( String filename)
  {
    IHDF5Reader reader = HDF5Factory.openForReading(filename);
    ArrayList<DataSetInfo> dataSets = recursiveGetInfo( reader, reader.object().getLinkInformation("/"));
    reader.close();
    return dataSets;
  }

  static ArrayList<DataSetInfo> recursiveGetInfo(IHDF5Reader reader, HDF5LinkInformation link)
  {
    ArrayList<DataSetInfo> dataSets = new ArrayList<DataSetInfo>();
    recursiveGetInfo(reader, link, dataSets);
    return dataSets;
  }

  static void recursiveGetInfo(IHDF5Reader reader, HDF5LinkInformation link, ArrayList<DataSetInfo> dataSets)
  {
    List<HDF5LinkInformation> members = reader.object().getGroupMemberInformation(link.getPath(), true);
    //    DefaultMutableTreeNode node = new DefaultMutableTreeNode(link.getName());

    for (HDF5LinkInformation info : members)
    {
      HDF5ObjectType type = info.getType();
      IJ.log(info.getPath() + ":" + type);
      switch (type)
      {
        case EXTERNAL_LINK:
          // update type to external link type - proceed through switch (no break)
          // external link target paths are formatted: "EXTERNAL::/path/to/file::/path/to/object"
          String[] extl_paths = info.tryGetSymbolicLinkTarget().split("::");
          IHDF5Reader extl_reader = HDF5Factory.openForReading(extl_paths[1]);
          HDF5LinkInformation extl_target = extl_reader.object().getLinkInformation(extl_paths[2]);
          type = extl_target.getType();
          extl_reader.close();
        case DATASET:
          HDF5DataSetInformation dsInfo = reader.object().getDataSetInformation(info.getPath());
          HDF5DataTypeInformation dsType = dsInfo.getTypeInformation();

          String dimText = "";
          if( dsInfo.getRank() == 0)
          {
            dimText ="1";
          }
          else
          {
            dimText += dsInfo.getDimensions()[0];
            for( int i = 1; i < dsInfo.getRank(); ++i)
            {
              dimText += "x" + dsInfo.getDimensions()[i];
            }
          }


          String typeText = HDF5ImageJ.dsInfoToTypeString(dsInfo);

          // try to read element_size_um attribute
          String element_size_um_text = "unknown";
          try {
            float[] element_size_um = reader.float32().getArrayAttr(info.getPath(), "element_size_um");
            element_size_um_text = "" + element_size_um[0] + "x"
                    + element_size_um[1] + "x" + element_size_um[2];

          }
          catch (HDF5Exception err) {
            IJ.log("Warning: Can't read attribute 'element_size_um' from dataset '" + info.getPath() + "':\n"
                    + err );
          }

          IJ.log(info.getPath() + ":" + dsInfo);

          dataSets.add( new DataSetInfo( info.getPath(), dimText, typeText,
                  element_size_um_text));


          break;
        case SOFT_LINK:
          IJ.log(info.getPath() + "     -> " + info.tryGetSymbolicLinkTarget());
          //      node.add(new DefaultMutableTreeNode(info.getName() + "     -> " + info.tryGetSymbolicLinkTarget()));

          break;
        case GROUP:
          recursiveGetInfo( reader, info, dataSets);
          //        node.add( browse(reader,info));

          break;
        default:
          break;
      }
    }
  }

  static ImagePlus loadDataSetsToHyperStack( String filename, String[] dsetNames,
                                             int nFrames, int nChannels)
  {
    return loadDataSetsToHyperStack( filename, dsetNames, nFrames, nChannels, true);
  }

  //-----------------------------------------------------------------------------
   static ImagePlus loadDataSetsToHyperStack( String filename, String[] dsetNames,
                                            int nFrames, int nChannels, boolean show)
  {
    String dsetName = "";
    try
    {
      IHDF5ReaderConfigurator conf = HDF5Factory.configureForReading(filename);
      conf.performNumericConversions();
      IHDF5Reader reader = conf.reader();
      ImagePlus imp = null;
      int rank      = 0;
      int nLevels   = 0;
      int nRows     = 0;
      int nCols     = 0;
      boolean isRGB = false;
      int nBits     = 0;
      double maxGray = 1;
      String typeText = "";
      for (int frame = 0; frame < nFrames; ++frame) {
        for (int channel = 0; channel < nChannels; ++channel) {
          // load data set
          //
          dsetName = dsetNames[frame*nChannels+channel];
          IJ.showStatus( "Loading " + dsetName);
          IJ.showProgress( frame*nChannels+channel+1, nFrames*nChannels);
          HDF5DataSetInformation dsInfo = reader.object().getDataSetInformation(dsetName);
          float[] element_size_um = {1,1,1};
          try {
            element_size_um = reader.float32().getArrayAttr(dsetName, "element_size_um");
          }
          catch (HDF5Exception err) {
            IJ.log("Warning: Can't read attribute 'element_size_um' from file '" + filename
                     + "', dataset '" + dsetName + "':\n"
                     + err + "\n"
                     + "Assuming element size of 1 x 1 x 1 um^3");
          }

          // in first call create hyperstack
          //
          if (imp == null) {
            rank = dsInfo.getRank();
            typeText = dsInfoToTypeString(dsInfo);
            if (rank == 2) {
              nLevels = 1;
              nRows = (int)dsInfo.getDimensions()[0];
              nCols = (int)dsInfo.getDimensions()[1];
            } else if (rank == 3) {
              nLevels = (int)dsInfo.getDimensions()[0];
              nRows   = (int)dsInfo.getDimensions()[1];
              nCols   = (int)dsInfo.getDimensions()[2];
              if( typeText.equals( "uint8") && nCols == 3)
              {
                nLevels = 1;
                nRows = (int)dsInfo.getDimensions()[0];
                nCols = (int)dsInfo.getDimensions()[1];
                isRGB = true;
              }
            } else if (rank == 4 && typeText.equals( "uint8")) {
              nLevels = (int)dsInfo.getDimensions()[0];
              nRows   = (int)dsInfo.getDimensions()[1];
              nCols   = (int)dsInfo.getDimensions()[2];
              isRGB   = true;
            } else {
              IJ.error( dsetName + ": rank " + rank + " of type " + typeText + " not supported (yet)");
              return null;
            }

            nBits = assignHDF5TypeToImagePlusBitdepth( typeText, isRGB);


            imp = IJ.createHyperStack( filename + ": " + dsetName,
                                       nCols, nRows, nChannels, nLevels, nFrames, nBits);
            imp.getCalibration().pixelDepth  = element_size_um[0];
            imp.getCalibration().pixelHeight = element_size_um[1];
            imp.getCalibration().pixelWidth  = element_size_um[2];
            imp.getCalibration().setUnit("micrometer");
            imp.setDisplayRange(0,255);
          }

          // copy slices to hyperstack
          int sliceSize = nCols * nRows;

          if (typeText.equals( "uint8") && isRGB == false) {
            MDByteArray rawdata = reader.uint8().readMDArray(dsetName);
            for( int lev = 0; lev < nLevels; ++lev) {
              ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex(
                  channel+1, lev+1, frame+1));
              System.arraycopy( rawdata.getAsFlatArray(), lev*sliceSize,
                                (byte[])ip.getPixels(),   0,
                                sliceSize);
            }
            maxGray = 255;
          }  else if (typeText.equals( "uint8") && isRGB) {  // RGB data
            MDByteArray rawdata = reader.uint8().readMDArray(dsetName);
            byte[] srcArray = rawdata.getAsFlatArray();


            for( int lev = 0; lev < nLevels; ++lev) {
              ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex(
                  channel+1, lev+1, frame+1));
              int[] trgArray = (int[])ip.getPixels();
              int srcOffset = lev*sliceSize*3;

              for( int rc = 0; rc < sliceSize; ++rc)
              {
                int red   = srcArray[srcOffset + rc*3] & 0xff;
                int green = srcArray[srcOffset + rc*3 + 1] & 0xff;
                int blue  = srcArray[srcOffset + rc*3 + 2] & 0xff;
                trgArray[rc] = (red<<16) + (green<<8) + blue;
              }

            }
            maxGray = 255;

          } else if (typeText.equals( "uint16")) {
            MDShortArray rawdata = reader.uint16().readMDArray(dsetName);
            for( int lev = 0; lev < nLevels; ++lev) {
              ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex(
                  channel+1, lev+1, frame+1));
              System.arraycopy( rawdata.getAsFlatArray(), lev*sliceSize,
                                (short[])ip.getPixels(),   0,
                                sliceSize);
            }
            short[] data = rawdata.getAsFlatArray();
            for (int i = 0; i < data.length; ++i) {
              if (data[i] > maxGray) maxGray = data[i];
            }
          } else if (typeText.equals( "int16")) {
            MDShortArray rawdata = reader.int16().readMDArray(dsetName);
            for( int lev = 0; lev < nLevels; ++lev) {
              ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex(
                  channel+1, lev+1, frame+1));
              System.arraycopy( rawdata.getAsFlatArray(), lev*sliceSize,
                                (short[])ip.getPixels(),   0,
                                sliceSize);
            }
            short[] data = rawdata.getAsFlatArray();
            for (int i = 0; i < data.length; ++i) {
              if (data[i] > maxGray) maxGray = data[i];
            }
          } else if (typeText.equals( "float32") || typeText.equals( "float64") ) {
            MDFloatArray rawdata = reader.float32().readMDArray(dsetName);
            for( int lev = 0; lev < nLevels; ++lev) {
              ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex(
                  channel+1, lev+1, frame+1));
              System.arraycopy( rawdata.getAsFlatArray(), lev*sliceSize,
                                (float[])ip.getPixels(),   0,
                                sliceSize);
            }
            float[] data = rawdata.getAsFlatArray();
            for (int i = 0; i < data.length; ++i) {
              if (data[i] > maxGray) maxGray = data[i];
            }
          } else if (typeText.equals( "int32")) {
            int[] rawdata_asflat = reader.int32().readMDArray(dsetName).getAsFlatArray();
            float[] rawdata_asfloat = new float[rawdata_asflat.length];
            for (int i = 0; i < rawdata_asfloat.length; i++)
            {
              rawdata_asfloat[i] = (float) rawdata_asflat[i];
            }
            for( int lev = 0; lev < nLevels; ++lev) {
              ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex(
                      channel+1, lev+1, frame+1));

              System.arraycopy(rawdata_asfloat, lev * sliceSize,
                  (float[]) ip.getPixels(), 0,
                  sliceSize);
            }
            for (int i = 0; i < rawdata_asfloat.length; ++i)
            {
              if (rawdata_asfloat[i] > maxGray) maxGray = rawdata_asfloat[i];
            }
          }
        }
      }
      reader.close();

      // aqdjust max gray
      for( int c = 1; c <= nChannels; ++c)
      {
        imp.setC(c);
        imp.setDisplayRange(0,maxGray);
      }

      imp.setC(1);

      if (show) {
        try {
          imp.show();
        }
        catch (HeadlessException herr) {}
      }
      return imp;
    }

    catch (HDF5Exception err)
    {

      IJ.log(Arrays.toString(err.getStackTrace()));

      IJ.error("Error while opening '" + filename
               + "', dataset '" + dsetName + "':\n"
               + err);
    }
    catch (Exception err)
    {
      IJ.log(Arrays.toString(err.getStackTrace()));
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

  static ImagePlus loadCustomLayoutDataSetToHyperStack( String filename, String dsetName, String layout) {
    return loadCustomLayoutDataSetToHyperStack(filename, dsetName, layout, true);
  }

  //-----------------------------------------------------------------------------
  //
  // Layout: any order of the letters x,y,z,c,t as string, e.g. "zyx" for a standard volumetric data set
  //
  static ImagePlus loadCustomLayoutDataSetToHyperStack( String filename, String dsetName, String layout, boolean show) {
    try
    {
      IHDF5ReaderConfigurator conf = HDF5Factory.configureForReading(filename);
      conf.performNumericConversions();
      IHDF5Reader reader = conf.reader();
      ImagePlus imp = null;

      // get datat set info and check layout string
      //
      IJ.showStatus( "Loading " + dsetName);
      //      IJ.showProgress( frame*nChannels+channel+1, nFrames*nChannels);
      HDF5DataSetInformation dsInfo = reader.object().getDataSetInformation(dsetName);
      float[] element_size_um = {1,1,1};
      try {
        element_size_um = reader.float32().getArrayAttr(dsetName, "element_size_um");
      }
      catch (HDF5Exception err) {
        IJ.log("Warning: Can't read attribute 'element_size_um' from file '" + filename
               + "', dataset '" + dsetName + "':\n"
               + err + "\n"
               + "Assuming element size of 1 x 1 x 1 um^3");
      }

      int rank = dsInfo.getRank();
      String typeText = dsInfoToTypeString(dsInfo);

      if( rank != layout.length()) {
        IJ.error( dsetName + ": rank " + rank + " is incompatible with your given layout string '" + layout +"' (rank " + layout.length() + ")");
        return null;
      }


      // compute dset stride (element-to-element offset in the linear array)
      //
      long[] dsetExtent = dsInfo.getDimensions();
      int[] stride = new int[rank];
      stride[rank-1] = 1;
      for (int d = rank-2; d >= 0; --d) {
        stride[d] = (int)dsetExtent[d+1] * stride[d+1];
      }

     // interpret layout string and get assigned data set extents
      //
      int nLevels   = 1;
      int nRows     = 1;
      int nCols     = 1;
      int nFrames   = 1;
      int nChannels = 1;
      int levelToLevelOffset     = 0;
      int rowToRowOffset         = 0;
      int colToColOffset         = 0;
      int frameToFrameOffset     = 0;
      int channelToChannelOffset = 0;

      int nBits     = 0;
      double maxGray = 1;

      //
      for (int d = 0; d < rank; ++d) {
        switch( layout.charAt(d)) {
          case 'x': nCols     = (int)dsetExtent[d]; colToColOffset         = stride[d]; break;
          case 'y': nRows     = (int)dsetExtent[d]; rowToRowOffset         = stride[d]; break;
          case 'z': nLevels   = (int)dsetExtent[d]; levelToLevelOffset     = stride[d]; break;
          case 'c': nChannels = (int)dsetExtent[d]; channelToChannelOffset = stride[d]; break;
          case 't': nFrames   = (int)dsetExtent[d]; frameToFrameOffset     = stride[d]; break;
          default:
            IJ.error( "your given layout string '" + layout +"' contains the illegal character '" + layout.charAt(d) + "'. Allowed characters are 'xyzct'");
            return null;
        }
      }

      // create appropriate hyperstack
      //
      IJ.log("Creating hyperstack with " + nFrames + " frames, "
             + nChannels + " channels, "
             + nLevels + " levels, "
             + nRows + " rows, and "
             + nCols + " cols");

      boolean isRGB = false;
      nBits = assignHDF5TypeToImagePlusBitdepth( typeText, isRGB);
      imp = IJ.createHyperStack( filename + ": " + dsetName,
                                 nCols, nRows, nChannels, nLevels, nFrames, nBits);
      imp.getCalibration().pixelDepth  = element_size_um[0];
      imp.getCalibration().pixelHeight = element_size_um[1];
      imp.getCalibration().pixelWidth  = element_size_um[2];
      imp.getCalibration().setUnit("micrometer");
      imp.setDisplayRange(0,255);


      // load data set and copy it to hyperstack
      //
      if (typeText.equals( "uint8") ) {
        byte[] rawdata = reader.uint8().readMDArray(dsetName).getAsFlatArray();
        for( int frame = 0; frame < nFrames; ++frame) {
          for( int channel = 0; channel < nChannels; ++channel) {
            for( int lev = 0; lev < nLevels; ++lev) {
              ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex(
                  channel+1, lev+1, frame+1));
              for( int row = 0; row < nRows; ++row) {
                byte[] trgData = (byte[])ip.getPixels();
                int trgOffset = row * nCols;
                int srcOffset =
                    frame * frameToFrameOffset
                    + channel * channelToChannelOffset
                    + lev * levelToLevelOffset
                    + row * rowToRowOffset;
                for( int col = 0; col < nCols; ++col) {
                  trgData[trgOffset] = rawdata[srcOffset];
                  ++trgOffset;
                  srcOffset += colToColOffset;
                }
              }
            }
          }
        }
        maxGray = 255;
      }
      else if (typeText.equals( "uint16") ) {
        short[] rawdata = reader.uint16().readMDArray(dsetName).getAsFlatArray();
        for( int frame = 0; frame < nFrames; ++frame) {
          for( int channel = 0; channel < nChannels; ++channel) {
            for( int lev = 0; lev < nLevels; ++lev) {
              ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex(
                  channel+1, lev+1, frame+1));
              for( int row = 0; row < nRows; ++row) {
                short[] trgData = (short[])ip.getPixels();
                int trgOffset = row * nCols;
                int srcOffset =
                    frame * frameToFrameOffset
                    + channel * channelToChannelOffset
                    + lev * levelToLevelOffset
                    + row * rowToRowOffset;
                for( int col = 0; col < nCols; ++col) {
                  trgData[trgOffset] = rawdata[srcOffset];
                  ++trgOffset;
                  srcOffset += colToColOffset;
                }
              }
            }
          }
        }
        for (int i = 0; i < rawdata.length; ++i) {
          if (rawdata[i] > maxGray) maxGray = rawdata[i];
        }
      }
      else if (typeText.equals( "int16") ) {
        short[] rawdata = reader.int16().readMDArray(dsetName).getAsFlatArray();
        for( int frame = 0; frame < nFrames; ++frame) {
          for( int channel = 0; channel < nChannels; ++channel) {
            for( int lev = 0; lev < nLevels; ++lev) {
              ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex(
                  channel+1, lev+1, frame+1));
              for( int row = 0; row < nRows; ++row) {
                short[] trgData = (short[])ip.getPixels();
                int trgOffset = row * nCols;
                int srcOffset =
                    frame * frameToFrameOffset
                    + channel * channelToChannelOffset
                    + lev * levelToLevelOffset
                    + row * rowToRowOffset;
                for( int col = 0; col < nCols; ++col) {
                  trgData[trgOffset] = rawdata[srcOffset];
                  ++trgOffset;
                  srcOffset += colToColOffset;
                }
              }
            }
          }
        }
        for (int i = 0; i < rawdata.length; ++i) {
          if (rawdata[i] > maxGray) maxGray = rawdata[i];
        }
      }
      else if (typeText.equals( "float32")  || typeText.equals( "float64") ) {
        float[] rawdata = reader.float32().readMDArray(dsetName).getAsFlatArray();
        for( int frame = 0; frame < nFrames; ++frame) {
          for( int channel = 0; channel < nChannels; ++channel) {
            for( int lev = 0; lev < nLevels; ++lev) {
              ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex(
                  channel+1, lev+1, frame+1));
              for( int row = 0; row < nRows; ++row) {
                float[] trgData = (float[])ip.getPixels();
                int trgOffset = row * nCols;
                int srcOffset =
                    frame * frameToFrameOffset
                    + channel * channelToChannelOffset
                    + lev * levelToLevelOffset
                    + row * rowToRowOffset;
                for( int col = 0; col < nCols; ++col) {
                  trgData[trgOffset] = rawdata[srcOffset];
                  ++trgOffset;
                  srcOffset += colToColOffset;
                }
              }
            }
          }
        }
        for (int i = 0; i < rawdata.length; ++i) {
          if (rawdata[i] > maxGray) maxGray = rawdata[i];
        }
      }
      else if (typeText.equals( "int32")) {
        IJ.error("Not implemented");
        throw new NotImplementedException();
//        int[] rawdata = reader.int32().readMDArray(dsetName).getAsFlatArray();
//        for( int frame = 0; frame < nFrames; ++frame) {
//          for( int channel = 0; channel < nChannels; ++channel) {
//            for( int lev = 0; lev < nLevels; ++lev) {
//              ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex(
//                      channel+1, lev+1, frame+1));
//              for( int row = 0; row < nRows; ++row) {
//                int[] trgData = (int[])ip.getPixels();
//                int trgOffset = row * nCols;
//                int srcOffset =
//                        frame * frameToFrameOffset
//                                + channel * channelToChannelOffset
//                                + lev * levelToLevelOffset
//                                + row * rowToRowOffset;
//                for( int col = 0; col < nCols; ++col) {
//                  trgData[trgOffset] = rawdata[srcOffset];
//                  ++trgOffset;
//                  srcOffset += colToColOffset;
//                }
//              }
//            }
//          }
//        }
//        for (int i = 0; i < rawdata.length; ++i) {
//          if (rawdata[i] > maxGray) maxGray = rawdata[i];
//        }
      }




//         int sliceSize = nCols * nRows;
//
//         if (typeText.equals( "uint8") && rank < 4) {
//           MDByteArray rawdata = reader.uint8().readMDArray(dsetName);
//           for( int lev = 0; lev < nLevels; ++lev) {
//             ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex(
//                 channel+1, lev+1, frame+1));
//             System.arraycopy( rawdata.getAsFlatArray(), lev*sliceSize,
//                               (byte[])ip.getPixels(),   0,
//                               sliceSize);
//           }
//           maxGray = 255;
//         }  else if (typeText.equals( "uint8") && rank == 4) {  // RGB data
//           MDByteArray rawdata = reader.uint8().readMDArray(dsetName);
//           byte[] srcArray = rawdata.getAsFlatArray();
//
//
//           for( int lev = 0; lev < nLevels; ++lev) {
//             ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex(
//                 channel+1, lev+1, frame+1));
//             int[] trgArray = (int[])ip.getPixels();
//             int srcOffset = lev*sliceSize*3;
//
//             for( int rc = 0; rc < sliceSize; ++rc)
//             {
//               int red   = srcArray[srcOffset + rc*3];
//               int green = srcArray[srcOffset + rc*3 + 1];
//               int blue  = srcArray[srcOffset + rc*3 + 2];
//               trgArray[rc] = (red<<16) + (green<<8) + blue;
//             }
//
//           }
//           maxGray = 255;
//
//         } else if (typeText.equals( "uint16")) {
//           MDShortArray rawdata = reader.uint16().readMDArray(dsetName);
//           for( int lev = 0; lev < nLevels; ++lev) {
//             ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex(
//                 channel+1, lev+1, frame+1));
//             System.arraycopy( rawdata.getAsFlatArray(), lev*sliceSize,
//                               (short[])ip.getPixels(),   0,
//                               sliceSize);
//           }
//           short[] data = rawdata.getAsFlatArray();
//           for (int i = 0; i < data.length; ++i) {
//             if (data[i] > maxGray) maxGray = data[i];
//           }
//         } else if (typeText.equals( "int16")) {
//           MDShortArray rawdata = reader.int16().readMDArray(dsetName);
//           for( int lev = 0; lev < nLevels; ++lev) {
//             ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex(
//                 channel+1, lev+1, frame+1));
//             System.arraycopy( rawdata.getAsFlatArray(), lev*sliceSize,
//                               (short[])ip.getPixels(),   0,
//                               sliceSize);
//           }
//           short[] data = rawdata.getAsFlatArray();
//           for (int i = 0; i < data.length; ++i) {
//             if (data[i] > maxGray) maxGray = data[i];
//           }
//         } else if (typeText.equals( "float32") || typeText.equals( "float64") ) {
//           MDFloatArray rawdata = reader.float32().readMDArray(dsetName);
//           for( int lev = 0; lev < nLevels; ++lev) {
//             ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex(
//                 channel+1, lev+1, frame+1));
//             System.arraycopy( rawdata.getAsFlatArray(), lev*sliceSize,
//                               (float[])ip.getPixels(),   0,
//                               sliceSize);
//           }
//           float[] data = rawdata.getAsFlatArray();
//           for (int i = 0; i < data.length; ++i) {
//             if (data[i] > maxGray) maxGray = data[i];
//           }
//         }
//       }
//     }


      reader.close();

      // aqdjust max gray
      imp.setDisplayRange(0,maxGray);
      if (show) {
        try {
          imp.show();
        }
        catch (HeadlessException herr) {}
      }
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


  //-----------------------------------------------------------------------------
  static void saveHyperStack( ImagePlus imp, String filename, String dsetNameTemplate,
                              String formatTime, String formatChannel, int compressionLevel,
                              String saveMode)
  {
    int nFrames   = imp.getNFrames();
    int nChannels = imp.getNChannels();
    int nLevs     = imp.getNSlices();
    int nRows     = imp.getHeight();
    int nCols     = imp.getWidth();

    // Name stubs for time points and channels
    String[] substT = HDF5ImageJ.createNameList( formatTime, nFrames);
    String[] substC = HDF5ImageJ.createNameList( formatChannel, nChannels);

    //
    //  Open output file
    //
    try
    {
      IHDF5Writer writer;
      if( saveMode.equals( "append"))
      {
        writer = HDF5Factory.configure(filename).useSimpleDataSpaceForAttributes().writer();
      }
      else
      {
        writer = HDF5Factory.configure(filename).useSimpleDataSpaceForAttributes().overwrite().writer();
      }

      //  get element_size_um
      //
      ij.measure.Calibration cal = imp.getCalibration();
      float[] element_size_um = new float[3];
      element_size_um[0] = (float) cal.pixelDepth;
      element_size_um[1] = (float) cal.pixelHeight;
      element_size_um[2] = (float) cal.pixelWidth;

      //  create channelDims vector for MDxxxArray
      //
      long[] channelDims = null;
      if (nLevs > 1)
      {
        channelDims = new long[3];
        channelDims[0] = nLevs;
        channelDims[1] = nRows;
        channelDims[2] = nCols;
      }
      else
      {
        channelDims = new long[2];
        channelDims[0] = nRows;
        channelDims[1] = nCols;
      }


      //
      //  loop through frames and channels
      //
      for( int t=0; t < nFrames; ++t)
      {
        for( int c=0; c < nChannels; ++c)
        {
          // format the data set name
          //
          String dsetName = dsetNameTemplate;
          dsetName = dsetName.replace("{t}", substT[t]);
          dsetName = dsetName.replace("{c}", substC[c]);

          System.out.println( "t="+t+",c="+c+" --> "+dsetName);

          // write Stack according to data type
          //
          int imgColorType = imp.getType();

          if (imgColorType == ImagePlus.GRAY8
              || imgColorType == ImagePlus.COLOR_256 )
          {
            // Save as Byte Array
            //
            MDByteArray arr = new MDByteArray( channelDims);
            // copy data
            //
            ImageStack stack = imp.getStack();
            byte[] flatArr   = arr.getAsFlatArray();
            int sliceSize    = nRows*nCols;

            for(int lev = 0; lev < nLevs; ++lev)
            {
              int stackIndex = imp.getStackIndex(c + 1,
                                                 lev + 1,
                                                 t + 1);
              System.arraycopy( stack.getPixels(stackIndex), 0,
                                flatArr, lev*sliceSize,
                                sliceSize);
            }

            // save it
            //
            writer.uint8().writeMDArray( dsetName, arr, HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));
          }
          else if (imgColorType == ImagePlus.GRAY16)
          {
            // Save as Short Array
            //
            MDShortArray arr = new MDShortArray( channelDims);

            // copy data
            //
            ImageStack stack = imp.getStack();
            short[] flatArr   = arr.getAsFlatArray();
            int sliceSize    = nRows*nCols;

            for(int lev = 0; lev < nLevs; ++lev)
            {
              int stackIndex = imp.getStackIndex(c + 1,
                                                 lev + 1,
                                                 t + 1);
              System.arraycopy( stack.getPixels(stackIndex), 0,
                                flatArr, lev*sliceSize,
                                sliceSize);
            }

            // save it
            //
            writer.uint16().writeMDArray( dsetName, arr, HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));
          }
          else if (imgColorType == ImagePlus.GRAY32)
          {
            // Save as Float Array
            //
            MDFloatArray arr = new MDFloatArray( channelDims);

            // copy data
            //
            ImageStack stack = imp.getStack();
            float[] flatArr   = arr.getAsFlatArray();
            int sliceSize    = nRows*nCols;

            for(int lev = 0; lev < nLevs; ++lev)
            {
              int stackIndex = imp.getStackIndex(c + 1,
                                                 lev + 1,
                                                 t + 1);
              System.arraycopy( stack.getPixels(stackIndex), 0,
                                flatArr, lev*sliceSize,
                                sliceSize);
            }

            // save it
            //
            writer.float32().writeMDArray( dsetName, arr,
                                           HDF5FloatStorageFeatures.createDeflationDelete(
                                               compressionLevel));
          }
          else if (imgColorType == ImagePlus.COLOR_RGB)
          {
            //  Save RGB as Byte Array with additional dimension
            //
            long[] channelDimsRGB = null;
            if (nLevs > 1)
            {
              channelDimsRGB = new long[4];
              channelDimsRGB[0] = nLevs;
              channelDimsRGB[1] = nRows;
              channelDimsRGB[2] = nCols;
              channelDimsRGB[3] = 3;
            }
            else
            {
              channelDimsRGB = new long[3];
              channelDimsRGB[0] = nRows;
              channelDimsRGB[1] = nCols;
              channelDimsRGB[2] = 3;
            }
            MDByteArray arr = new MDByteArray( channelDimsRGB);

            // copy data
            //
            ImageStack stack = imp.getStack();
            byte[] flatArr   = arr.getAsFlatArray();
            int sliceSize    = nRows*nCols;

            for(int lev = 0; lev < nLevs; ++lev)
            {
              int stackIndex = imp.getStackIndex(c + 1,
                                                 lev + 1,
                                                 t + 1);

              ColorProcessor cp = (ColorProcessor)(stack.getProcessor(stackIndex));
              byte[] red   = cp.getChannel(1);
              byte[] green = cp.getChannel(2);
              byte[] blue  = cp.getChannel(3);

              int offset = lev*sliceSize*3;
              for( int i=0; i < sliceSize; ++i)
              {
                flatArr[offset+3*i+0] = red[i];
                flatArr[offset+3*i+1] = green[i];
                flatArr[offset+3*i+2] = blue[i];
              }

            }

            // save it
            //
            writer.uint8().writeMDArray( dsetName, arr, HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));


          }


          //  add element_size_um attribute
          //
          writer.float32().setArrayAttr( dsetName, "element_size_um",
                                         element_size_um);


        }
      }
      writer.close();
    }


    catch (HDF5Exception err)
    {
      IJ.error("Error while saving '" + filename + "':\n"
               + err);
    }
    catch (Exception err)
    {
      IJ.error("Error while saving '" + filename + "':\n"
               + err);
    }
    catch (OutOfMemoryError o)
    {
      IJ.outOfMemory("Error while saving '" + filename + "'");
    }
  }




  //-----------------------------------------------------------------------------
   static String dsInfoToTypeString( HDF5DataSetInformation dsInfo) {
    HDF5DataTypeInformation dsType = dsInfo.getTypeInformation();
    String typeText = "";

    if (dsType.isSigned() == false) {
      typeText += "u";
    }

    switch( dsType.getDataClass())
    {
      case INTEGER:
        typeText += "int" + 8*dsType.getElementSize();
        break;
      case FLOAT:
        typeText += "float" + 8*dsType.getElementSize();
        break;
      default:
        typeText += dsInfo.toString();
    }
    return typeText;
  }

  //-----------------------------------------------------------------------------
   static int assignHDF5TypeToImagePlusBitdepth( String type, boolean isRGB) {
    int nBits = 0;
    if (type.equals("uint8")) {
      if( isRGB ) {
        nBits = 24;
      } else {
        nBits = 8;
      }
    } else if (type.equals("uint16") || type.equals("int16")) {
      nBits = 16;
    } else if (type.equals("float32") || type.equals("float64")) {
      nBits = 32;
    } else if (type.equals("int32")) {
      nBits = 16; // for 32 bit images we need to trim to 16 bits
    } else {
      IJ.error("Type '" + type + "' Not handled yet!");
    }
    return nBits;
  }


  //-----------------------------------------------------------------------------
   static String[] createNameList( String formatString, int nElements)
  {
    String[] nameList = new String[nElements];
    if( formatString.startsWith("%"))
    {
      for( int i=0; i < nElements; ++i)
      {
        nameList[i] = String.format(formatString, i);
      }
    }
    else
    {
      String[] tokens = formatString.split("[,\\s]+");
      if( tokens.length >= nElements)
      {
        nameList = tokens;
      }
      else
      {
        IJ.log( "Warning: format-list \"" + formatString + "\" contains only " +  tokens.length
                + " tokens, but yout dataset needs " + nElements + " unique entries! "
                + "Appending numbers \"" +  tokens.length + "\" to \"" + (nElements-1) + "\"!");
        for( int i = 0; i < tokens.length; ++i)
        {
          nameList[i] = tokens[i];
        }
        for( int i = tokens.length; i < nElements; ++i)
        {
          nameList[i] = String.format( "%d",i);
        }
      }
    }
    return nameList;
  }
}
