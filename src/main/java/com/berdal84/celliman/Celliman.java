package com.berdal84.celliman;

import ij.ImagePlus;
import net.imagej.*;
import net.imagej.display.ImageDisplayService;
import net.imagej.display.WindowService;
import net.imagej.ops.Initializable;
import net.imagej.ops.OpService;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.ChoiceWidget;

@Plugin(
    type = DynamicCommand.class,
    menuPath = "Plugins>Celliman>Run for current Dataset"
)
public class Celliman<T extends RealType<T>> extends DynamicCommand implements Initializable {

    @Parameter
    private Dataset inDataset;

    @Parameter
    private ImageJService imageJService;

    @Parameter
    private ImgPlusService imgPlusService;

    private ImagePlus input;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;

    @Parameter
    private LogService logger;

    @Parameter
    private ImageDisplayService imageDisplayService;

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
    private ImgPlus<T> originalImage;

    // -- Callback methods --

    private void stainingChanged() {
        logger.debug("staining changed: " + staining);
    }

    private void methodChanged() {
        logger.debug("method changed: " + method);
    }

    @Override
    public void initialize() {
        uiService.showDialog("Celliman will start");
        getInfo();
        originalImage = (ImgPlus<T>) inDataset.getImgPlus(); // IntelliJ highlight the cast in yellow, but it works fine at runtime

        preview();
    }

    @Override
    public void preview() {
        double sigma = 5.0;
        if( staining != null && staining.equals("Nuclear")) {
            sigma = 50.0;
        }
        RandomAccessibleInterval<T> result = opService.filter().gauss(originalImage, sigma);
        imageDisplayService.getActiveImageDisplay().close();
        uiService.show("PREVIEW", result);
    }
}
