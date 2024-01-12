
package com.berdal84.celliman;

import ij.ImagePlus;
import loci.formats.FormatException;
import loci.plugins.in.DisplayHandler;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;

import java.io.IOException;

class Utilities {

    static ImagePlus[] open(String path) throws FormatException, IOException {

        ImporterOptions options = new ImporterOptions();
        options.setId(path);
        options.setOpenAllSeries(true);
        options.setSplitChannels(false);
        options.setWindowless(true);

        ImportProcess process = new ImportProcess(options);
        process.execute();

        DisplayHandler displayHandler = new DisplayHandler(process);
        displayHandler.displayOriginalMetadata();
        displayHandler.displayOMEXML();


        ImagePlusReader reader = new ImagePlusReader(process);
        ImagePlus[] imps = reader.openImagePlus();

        if (!process.getOptions().isVirtual())
        {
            process.getReader().close();
        }

        return imps;
    }
}