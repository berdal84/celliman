package com.berdal84.celliman;

import com.berdal84.celliman.Celliman;
import net.imagej.Dataset;
import net.imagej.ImageJ;

import java.io.File;

class Main {
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        File file = new File(System.getenv("FILE_PATH"));

        if (!file.exists()) {
            ij.ui().showDialog("No FILE_PATH defined in env vars. You'll be asked to pick a file manually.");
            // ask the user for a file to open
            file = ij.ui().chooseFile(null, "open");
        }

        if (file != null) {

            // open a file with ImageJ
            final Dataset ds = ij.scifio().datasetIO().open(file.getAbsolutePath());

            // display it via ImageJ
            ij.ui().show(ds);

            // invoke the plugin
            ij.command().run(Celliman.class, true);
        }
    }
}