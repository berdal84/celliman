package com.berdal84.celliman;

import java.io.File;

import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.lut.LUTService;
import net.imagej.ops.OpService;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.ChoiceWidget;
import ij.plugin.ZProjector;
import io.scif.services.DatasetIOService;

/**
 * This example illustrates how to create an ImageJ {@link org.scijava.command.Command} plugin.
 * <p>
 * The code here is a simple Gaussian blur using ImageJ Ops.
 * </p>
 * <p>
 * You should replace the parameter fields with your own inputs and outputs,
 * and replace the {@link run} method implementation with your own logic.
 * </p>
 */
@Plugin(
    type = DynamicCommand.class,
    menuPath = "Plugins>Celliman>Run"
)
public class CellimanCommand<T extends RealType<T>> extends DynamicCommand implements Initializable {

    @Parameter
    private Number tutu;

    @Parameter
    private Dataset dataset;

    @Parameter
    private UIService ui;

    @Parameter
    private OpService op;

    @Parameter
    private LogService logger;

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
    private String staining = "Nuclear";

    @Parameter(
        label       = "Segmentation Method",
        description = "Select the segmentation method to use.",
        choices     = { "Ilastic", "Cell-Segmentation" },
        style       = ChoiceWidget.LIST_BOX_STYLE,
        callback    = "methodChanged"
    )
    private String method = "Ilastic";

    // output preview
    private ImagePlus preview;

    // -- Callback methods --

    private void stainingChanged() {
        logger.debug("staining changed: " + staining);
        this.updatePreview();
    }

    private void methodChanged() {
        logger.debug("method changed: " + method);
        this.updatePreview();
    }

    // -- Initializable methods --

    @Override
    public void initialize() {
        getInfo();
        deduceParametersFromImageName();
        updatePreview();
    }

    @Override
    public void run() {
        updatePreview();
        ui.showDialog("Celliman will close");
    }

    private void deduceParametersFromImageName() {
        final String name = dataset.getImgPlus().getName();
        final String[] tokens = name.toLowerCase().split(" ");

        // Deduce staining
        for (String each : tokens) {
            switch (each) {

                case "dapi":
                    staining = "Nuclear";
                    break;

                case "rho":
                    staining = "Cytolosic";
                    break;

            }
        }
    }

    private void updatePreview() {

        // Apply a Z-Projection with a Max Intensity mode
        final long depth = dataset.getDepth();
        if ( depth > 1 )
        {
            ui.showDialog("Z-Project for " + depth + " slices");
            ZProjector projector = new ZProjector();
            projector.setImage((ImagePlus)dataset.getImgPlus().getImg());
            final String method = ZProjector.METHODS[ZProjector.MAX_METHOD];
            projector.run(method);
            this.preview = projector.getProjection();
        }
        else if (this.preview == null)
        {
            ui.showDialog("No need to Z-Project, file has a single depth");
            this.preview = (ImagePlus)dataset.getImgPlus().getImg().copy();
        }

        // Update name
        final String name = String.format("%s (PREVIEW)", dataset.getName() );

        // Make sure preview is visible
        ui.show(this.preview);

        /*

        auto record macro (just for hints)

        imp = ZProjector.run(imp,"max");
        IJ.run(imp, "Window/Level...", "");
        IJ.run(imp, "Enhance Contrast", "saturated=0.35");
        IJ.run(imp, "Apply LUT", "");
        IJ.run(imp, "Trainable Weka Segmentation", "");
        call("trainableSegmentation.Weka_Segmentation.loadClassifier", "....../Weka Classifier.model");
        call("trainableSegmentation.Weka_Segmentation.getResult");
        IJ.run("Close");
        imp.close();
        imp.close();
        imp.close();
        IJ.run(imp, "Window/Level...", "");
        IJ.run(imp, "Enhance Contrast", "saturated=0.35");
        IJ.run(imp, "Apply LUT", "stack");
        imp = ZProjector.run(imp,"avg");
        IJ.run("Window/Level...", "");
        IJ.run(imp, "Enhance Contrast", "saturated=0.35");
        IJ.run("Close");
        imp.close();
        imp.close();
         */
    }

    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // ask the user for a file to open
        final File file = ij.ui().chooseFile(null, "open");

        if (file != null) {
            // load the dataset
            final ImagePlus[] img = Utilities.open(file.getPath());

            // show the image
            ij.ui().show(img);

            // invoke the plugin
            ij.command().run(CellimanCommand.class, true, "tutu", 42);
        }
    }
}
