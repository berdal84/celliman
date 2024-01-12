package com.berdal84.celliman;

import java.awt.*;
import java.io.File;

import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;
import io.scif.formats.ImageIOFormat;
import net.imagej.*;
import net.imagej.display.WindowService;
import net.imagej.lut.LUTService;
import net.imagej.ops.Initializable;
import net.imagej.ops.OpService;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;

import org.scijava.command.*;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.ChoiceWidget;

import javax.imageio.ImageTranscoder;

@Plugin(
    type = DynamicCommand.class,
    menuPath = "Plugins>Celliman>Run for current Dataset"
)
public class CellimanCommand<T extends RealType<T>> extends DynamicCommand implements Initializable {

    @Parameter
    private Dataset dataset;

    @Parameter
    private ConvertService convertService;
    @Parameter
    private ImageJService imageJService;

    @Parameter
    private ImgPlusService imgPlusService;

    private ImagePlus input;

    @Parameter
    private UIService ui;

    @Parameter
    private OpService op;

    @Parameter
    private LogService logger;

    @Parameter
    private WindowService window;

    @Parameter
    private CommandService command;

    @Parameter
    private LUTService lut;

    @Parameter(
        label       = "Which staining is it?",
        description = "Staining type must be manually defined in order to run the image analysis",
        choices     = { "Nuclear", "Cytosolic" },
        style       = ChoiceWidget.LIST_BOX_STYLE,
        callback    = "stainingChanged"
    )
    private String staining;

    @Parameter(
        label       = "Segmentation Method",
        description = "Select the segmentation method to use.",
        choices     = { "Ilastic", "Cell-Segmentation" },
        style       = ChoiceWidget.LIST_BOX_STYLE,
        callback    = "methodChanged"
    )
    private String method;



    private ImagePlus output;

    // -- Callback methods --

    private void stainingChanged() {
        logger.debug("staining changed: " + staining);
    }

    private void methodChanged() {
        logger.debug("method changed: " + method);
    }

    // -- Initializable methods --

    @Override
    public void initialize() {
        ui.showDialog("Celliman will start");
        this.deduceParameters(dataset.getName());
        input = convertService.convert(dataset.getImgPlus(), ImagePlus.class);
        if ( input == null ) {
            logger.error("Unable to convert to ImagePlus");
            this.cancel();
        }
    }

    private void deduceParameters(final String name) {
        logger.debug("Deducing parameters from name ...");
        final String[] tokens = name.toLowerCase().split(" ");

        // Deduce staining
        for (String each : tokens) {
            switch (each) {

                case "dapi":
                    setInput("staining", "Nuclear");
                    break;

                case "rho":
                    setInput("staining", "Cytolosic");
                    break;

            }
        }
    }

    @Override
    public void preview() {

        logger.debug("Check if preview exists ...");
        if ( output == null )
        {
            logger.debug("Duplicating input ...");
            output = input.duplicate();
            ui.show("[[ PREVIEW ]]", this.output);
        } else {
            output.setImage(input);
            logger.debug("Already exists");
        }

        // Apply a Z-Projection with a Max Intensity mode
        logger.debug("Handle depth ...");
        final long depth = output.getNDimensions();
        if ( depth > 1 )
        {
            logger.debug("Z-Project for " + depth + " slices");
            ZProjector zp = new ZProjector( output );
            zp.setMethod(ZProjector.MAX_METHOD);
            zp.doProjection();
        }
        else
        {
            logger.debug("No need to Z-Project, file has a single depth");
        }


        ImageProcessor ip = output.getProcessor();
        ip.autoThreshold();

        switch ( staining ) {
            case "Nuclear":
                ip.invertLut();
                break;
            default:
                ip.flipHorizontal();
        }
    }

    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        File file = new File(System.getenv("FILE_PATH"));

        if( !file.exists() ) {
            ij.ui().showDialog("No FILE_PATH defined in env vars. You'll be asked to pick a file manually.");
            // ask the user for a file to open
            file = ij.ui().chooseFile(null, "open");
        }

        if (file != null) {

            // open a file with ImageJ
            final Dataset ds = ij.scifio().datasetIO().open( file.getAbsolutePath() );

            // display it via ImageJ
            ij.ui().show(ds);

            // invoke the plugin
            ij.command().run(CellimanCommand.class, true);
        }
    }
}
