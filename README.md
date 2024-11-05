# HDF5 Plugin for ImageJ and Fiji

[![](https://github.com/fiji/HDF5_Vibez/actions/workflows/build-main.yml/badge.svg)](https://github.com/fiji/HDF5_Vibez/actions/workflows/build-main.yml)

This plugin is an HDF5 reader/writer plugin for ImageJ and Fiji. It is a continuation of the [original code by Olaf Ronneberger](https://lmb.informatik.uni-freiburg.de/resources/opensource/imagej_plugins/hdf5.html) and others.

HDF5 is a data format for storing extremely large and complex data collections. For more information see the [official website](https://www.hdfgroup.org/solutions/hdf5). The plugin uses the [jhdf5 library from ETH SIS](https://unlimited.ethz.ch/spaces/JHDF/pages/92865195/JHDF+HDF5+for+Java) for reading and writing HDF5 files.

## Features

The HDF5 plugin for ImageJ and Fiji provides the following features:

* Loading 2D - 5D datasets
* Loading and combining mulitple 2d/3D datasets to 3D/4D/5D Hyperstacks
* Writing Hyperstacks to multiple 3D datasets
* scriptable load and save commands

## Example Data Sets

* [pollen.h5](https://lmb.informatik.uni-freiburg.de/resources/opensource/imagej_plugins/samples/pollen.h5) (3D confocal data of a pollen grain: 8 bit gray, approx. 16MB

## Load data sets

1.  Select "__File -- Import -- HDF5...__". The file selector will pop up. Double click the file you want to load.
2.  The "__Select data sets__" dialog will open:
    ![](https://lmb.informatik.uni-freiburg.de/resources/opensource/imagej_plugins/select_data_sets2.png)
3.  select one or more datasets. Multiple selections can be done by
    *   mouseclick on first and Shift+mouseclick on last item
    *   mousedown on first and drag to last item
    *   CTRL+mouseclick to select / deselect individual items
    *   CTRL+A selects all items
4.  chose how they should be loaded or combined to a hyperstack.
    *   Load as ... individual stacks will create an individual window for each selected data set
    *   Load as ... individual hyperstacks (custom layout) will create a new hyperstack for each selected dataset. The data set layout has to be specified in the textfield below. HDF5 uses C-style / Java-style indexing of the array, i.e., the slowest changing dimension comes first (see size in the table). Typical storage orders are:
        * "yx": 2D image
        * "zyx": 3D image
        * "tyx": 2D movie
        * "tzyx": 3D movie
        * "cyx": 2D multi-channel image
        * "tczyx": 3D multi-channel move
        * ...
        Of course, any other permutation of the letters y,x,z,t,c is allowed.
    * __Combine to ... hyperstack (multichannel)__ loads the selected 2D/3D data sets and combines them to a multi-channel hyperstack
    * __Combine to ... hyperstack (time series)__ loads the selected 2D/3D data sets and combines them to a time-series hyperstack
    * __Combine to ... hyperstack (multichannel time series)__ loads the selected 2D/3D data sets and combines them to a multichannel time-series hyperstack. You have to specify the Number of channels of the resulting hyperstack. The number of time points is then determined from the number of selected data sets divided by the number of channels

## Save data sets

1.  Select "File -- Save As -- HDF5 (new or replace)..." to create a new HDF5 file or "File -- Save As -- HDF5 (append)..." to append the dataset(s) to an existing HDF5 file. The file selector will pop up. Select the file name.
2.  The Save Dialog will open to select the data set layout
    ![](https://lmb.informatik.uni-freiburg.de/resources/opensource/imagej_plugins/save_dialog.png)
3.  Compression Level allow to select the compression of the data set. The compression is lossless, i.e. it works like a zip-archive. Possible compression levels are
    *   no compression,
    *   1 (fastest, larger file)
    *   2
    *   ...
    *   9 (slowest, smallest file)
4.  __Presets:__ allows to select presets for the data set layout. There is no official standard, how to name the datasets. For general purpose data we usually name it as "/t0/channel0", "t0/channel1", ... which is the "Standard" Preset.
5.  __Dataset Names Template__ specifies the template string for the data set names. The placeholders __{t}__ and __{c}__ will be replaced for each timepoint/channel combination with the strings specified in the following two textfields.
6.  __Replace {t} with:__ and __Replace {c} with:__ specifies the enconding of time points and channels in the filename. Possible entries area printf-style format string or a list of strings (one entry per line), e.g.,
    *   __%d__ for number style like 1,2,3,...
    *   __%.03d__ for zero-padded-numbers with 3 digits: 001, 002, 003, ...
    *   __nuclei__
        __cellborder__
        __pattern__         for named channels
7. The __Update Preview__ button shows the __Resulting Mapping:__ of the hyperstack time points and channels to the HDF5 data set names

## Internals

The HDF5 plugin saves and loads the pixel/voxel size in micrometer of the image in the attribute __"element_size_um"__. It has always 3 components in the order z,y,x (accordingly to the c-style indexing). Other meta data is not saved/loaded

## Wish list for next version

* Support for single data sets with more than 2GB size (will require a slice-wise or block-wise loadin/saving)
* disable the Log Window
* load a sub cube of the data set (e.g. for large 5D arrays stored in a single dataset)
