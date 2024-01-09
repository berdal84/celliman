package com.berdal84.celliman;

import java.io.File;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
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
    type = Command.class,
    menuPath = "Plugins>Celliman"
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
    private CommandService command;

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
    private ImgPlus<?> preview;

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
        updatePreview();
    }

    @Override
    public void run() {
        updatePreview();
        ui.showDialog("Celliman is Happy");
    }

    public void updatePreview() {
        final ImgPlus<?> image = dataset.getImgPlus();

        // Make a temporary copy to store a preview
        if (this.preview == null) {
            this.preview = image.copy();
            this.preview.setName("Celliman PREVIEW");
            // Make sure preview is visible
            ui.show(this.preview);
        }

        // Temporarily show a dialog
        switch (staining) {
            case "Nuclear":
                ui.showDialog("You choose Nuclear");
                break;
            case "Cytosolic":
                ui.showDialog("You choose Cytosolic");
        }

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
            ij.command().run(CellimanCommand.class, true, "tutu=42");
        }
    }
}
