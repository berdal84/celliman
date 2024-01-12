package com.berdal84.celliman;

import java.io.File;

import ij.ImagePlus;
import ij.WindowManager;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.display.WindowService;
import net.imagej.lut.LUTService;
import net.imagej.ops.Initializable;
import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imglib2.type.numeric.RealType;

import org.scijava.command.*;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.UIService;
import org.scijava.widget.ChoiceWidget;

import static org.scijava.ui.DialogPrompt.MessageType.QUESTION_MESSAGE;
import static org.scijava.ui.DialogPrompt.OptionType.YES_NO_OPTION;
import static org.scijava.ui.DialogPrompt.Result.YES_OPTION;

@Plugin(
    type = DynamicCommand.class,
    menuPath = "Plugins>Celliman>Run for current Dataset"
)
public class CellimanCommand<T extends RealType<T>> extends DynamicCommand implements Initializable {

    @Parameter
    private Dataset dataset;

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

    // output preview
    private ImgPlus<?> preview;

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
        if (dataset != null) {
            getInfo();
            DialogPrompt.Result result = ui.showDialog(
                    "Do you want Celliman to try to deduce parameters from dataset?",
                    QUESTION_MESSAGE,
                    YES_NO_OPTION
            );
            if( result == YES_OPTION) {
                deduceParameters(dataset);
            }
        } else {
            logger.warn("No dataset, skip end of initialize()");
        }
        ui.showDialog("Celliman is ready");
    }

    private void deduceParameters(final Dataset _dataset) {
        final String name = _dataset.getImgPlus().getName();
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
        final ImgPlus<?> image = dataset.getImgPlus();

        if ( this.preview != null ) {
            window.remove(this.preview.getName());
        }

        this.preview = image.copy();

        // Apply a Z-Projection with a Max Intensity mode
        final long depth = dataset.getDepth();
        if ( depth > 1 )
        {
            ui.showDialog("Z-Project for " + depth + " slices");
            Op maxOp = op.op("stats.max", image);
            op.run("project", this.preview, image, maxOp, 2);
        }
        else
        {
            ui.showDialog("No need to Z-Project, file has a single depth");
        }

        // Update name
        final String name = String.format("%s (PREVIEW)", dataset.getName() );
        this.preview.setName(name);

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
            final Dataset dataset = ij.scifio().datasetIO().open(file.getPath());

            // show the image
            ij.ui().show(dataset);

            // invoke the plugin
            ij.command().run(CellimanCommand.class, true);
        }
    }
}
