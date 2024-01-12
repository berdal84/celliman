package com.berdal84.celliman;

import java.io.File;

import net.imagej.*;
import net.imagej.display.WindowService;
import net.imagej.lut.LUTService;
import net.imagej.ops.Initializable;
import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;

import net.imglib2.type.numeric.real.DoubleType;
import org.jetbrains.annotations.NotNull;
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
    private Dataset input;

    @Parameter
    private DatasetService datasetService;

    @Parameter
    private UIService ui;

    @Parameter
    private OpService op;

    @Parameter
    private LogService logger;

    @Parameter
    private WindowService window;

    @Parameter
    private ImgPlusService ijpservice;

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
    private Dataset preview;

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
        if (input != null) {
            getInfo();
            DialogPrompt.Result result = ui.showDialog(
                    "Do you want Celliman to try to deduce parameters from dataset?",
                    QUESTION_MESSAGE,
                    YES_NO_OPTION
            );
            if( result == YES_OPTION) {
                deduceParameters(input);
            }
        } else {
            logger.warn("No dataset, skip end of initialize()");
        }
        ui.showDialog("Celliman is ready");
    }

    private void deduceParameters(final Dataset dataset) {
        logger.debug("Deducing parameters from dataset ...");
        final String name = dataset.getImgPlus().getName();
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
        final ImgPlus<? extends RealType<?>> inputImgPlus = input.getImgPlus();

        logger.debug("Check if preview exists ...");
        if ( this.preview == null )
        {
            logger.debug("Duplicating input ...");
            this.preview = input.duplicate();

            // Update name
            final String name = String.format("%s (PREVIEW)", input.getName() );
            this.preview.setName(name);

            ui.show(this.preview);
        } else {
            logger.debug("Already exists");
        }

        // Apply a Z-Projection with a Max Intensity mode
        logger.debug("Handle depth ...");
        final long depth = input.getDepth();
        if ( depth > 1 )
        {
            logger.debug("Z-Project for " + depth + " slices");
            Op maxOp = op.op("stats.max", inputImgPlus);
            op.run("project", this.preview, inputImgPlus, maxOp, 2);
        }
        else
        {
            logger.debug("No need to Z-Project, file has a single depth");
        }

        logger.debug("Create a blank image at the same same as image");
        final IterableInterval<DoubleType> blank = op.create().img(preview.getImgPlus());

        logger.debug("Fill in an image with a formula");

        try {
            String formula = getFormula();
            final IterableInterval<DoubleType> sinusoid = op.image().equation(blank, formula);

            // Show the result
            logger.debug("Show the result");
            ui.show(sinusoid);
        } catch (Exception e) {
            ui.showDialog("Unable to run the operation " + e.toString());
        }

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

    @NotNull
    private String getFormula() {
        String formula;
        switch (staining) {
            case "Nuclear":
                formula = "10 * (Math.cos(0.3*p[0]) + Math.sin(0.3*p[1]))";
                break;
            default:
                formula = "20 * (Math.cos(0.3*p[0]) + Math.sin(0.3*p[1]))";
                break;
        }
        return formula;
    }

    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        File file = new File("/home/berenger/Bureau/06-23-23 RCC_D5Z P4 RIV31 D203 DAPI AP2a 488 SOX9 555 TileScan 1_s07_z3_ch00.tif");

        if( !file.exists() ) {
            // ask the user for a file to open
            file = ij.ui().chooseFile(null, "open");
        }

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
