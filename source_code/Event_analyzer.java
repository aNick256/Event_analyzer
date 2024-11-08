// Ali Nick Maleki, MDo Lab, TU DELFT
// 25/09/2023

import ij.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.gui.*;
import ij.measure.Calibration;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import ij.ImageJ;
import ij.io.FileSaver;
import java.awt.image.BufferedImage;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.IJ;
import ij.io.FileInfo;
import ij.process.ImageStatistics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map.Entry;
import javax.swing.*;
import javax.swing.border.*;
import ij.process.ImageProcessor;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import ij.WindowManager;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.ImageListener;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;



public class Event_analyzer implements PlugIn, ActionListener, ImageListener {

    

    JPanel mainpanel;
    JButton extractButton;
    JButton resetButton;
    JButton FSButton;
    JButton clearCategoriesButton;
    JButton addCategoryButton;  // New button for adding categories
    JButton renameROIsButton;  // New button for renaming ROIs
    JButton keepOnlyEventROIsButton ; 
    JButton statisticsButton;  // New button for showing event statistics
    JButton makeKymoButton;  // New button for generating kymograph
    JPanel optionPanel;
    JPanel plusEndButtonsPanel;  // Panel to hold plus end category buttons
    JPanel minusEndButtonsPanel;  // Panel to hold minus end category buttons
    JPanel otherButtonsPanel;  // Panel to hold other category buttons
    JComboBox<String> intensityComboBox;
    ImagePanel imagePanel;
    ImagePlus imp ;
    ImagePlus existingTempKymo ;
    Dimension buttonMaxSize = new Dimension(150, 20); // You can adjust the width and height as needed
    // Add these variables as instance variables in your class
    private JTextField pixelSizeTextField;
    private JButton scaleDownButton;
    private boolean nextRoiExists = true ;
    HashMap<JButton, JSpinner> categorySpinners = new HashMap<>();

    String[] plusEndCategories = {
        "Extension",
        "Extension rescue",
        "Rescue with NS",
        "Retention with NS",
        "Stall with NS",
        "Tip tracking",
        "NS crossing",
        "Rescue without NS",
        "Retention without NS",
        "Stall without NS" ,
        "Growth",
        "Shrinkage"
    };
    String[] minusEndCategories = {
        "Extension",
        "Extension rescue",
        "Rescue with NS",
        "Retention with NS",
        "Stall with NS",
        "Tip tracking",
        "NS crossing" ,
        "Rescue without NS",
        "Retention without NS",
        "Stall without NS"  ,
        "Growth",
        "Shrinkage"
    };

    String[] otherCategories = {
        "Diffusion",
        "Interesting"
    };
    RoiManager roiManager;
    int currentRoiIndex;
    String currentCategory;  // Stores the currently selected category
    JSlider lineWidthSlider;  // Slider for adjusting line width
    JTextField lineWidthTextField;  // Text field for displaying line width value
    String selectedMethod;
    JButton multipleEventsButton; 
    JButton noEventButton; // New button for selecting multiple events
    HashMap<JButton, JCheckBox> categoryCheckBoxes;  // Stores checkbox references for each category button



    // Main method to run the plugin as a standalone application

    public static void main(String[] args) {
        ImageJ imageJ = new ImageJ();
        Event_analyzer plugin = new Event_analyzer();
        plugin.run(null);
    }

    public void run(String arg) {
        imp = WindowManager.getCurrentImage(); 
        if (imp == null) {
            IJ.error("No image open");
        }
        IJ.run("Brightness/Contrast...");
        IJ.setTool("line");
        selectedMethod = "Maximum";
        mainpanel = new JPanel();
        mainpanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 5;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainpanel.add(createOptionPanel(), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 5;
        gbc.weightx = 3.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainpanel.add(createImagePanel(), gbc);

        JFrame frame = new JFrame("Event Analyzer");
        frame.getContentPane().add(mainpanel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(900, 950);
        frame.setVisible(true);

        // Initialize the ROI Manager and current ROI index
        roiManager = RoiManager.getInstance();
        if (roiManager == null) {
            roiManager = new RoiManager();
        }
        Roi currentROI = imp.getRoi();
        currentRoiIndex = roiManager.getRoiIndex(currentROI) ;
        // Register this class as an ImageListener to listen for image events
        ImagePlus.addImageListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == extractButton) {
            extractROIs();
        } else if (e.getSource() == addCategoryButton) {
            addCategory();
        } else if (e.getSource() == clearCategoriesButton) {
            clearCategories();
        } else if (e.getSource() == keepOnlyEventROIsButton) {
            selectImage(imp);
            keepOnlyEventROIs();
        } else if (e.getSource() == renameROIsButton) {
            selectImage(imp);
            renameROIs();
        } else if (e.getSource() == statisticsButton) {
            selectImage(imp);
            showEventList();
            showEventStatistics();
        } else if (e.getSource() == makeKymoButton) {
            imp = WindowManager.getCurrentImage();
            makeKymograph();
        } else if (e.getSource() == resetButton) {
            resetPlugin();
        } else if (e.getSource() == FSButton) {
            runFilamentSensor();
} else if (e.getSource() == multipleEventsButton) {
processMultipleEvents();
} else if (e.getSource() == noEventButton) {
processNoEvent(e);
} 
else {
processCategoryButtonClick(e);
}
}

@Override
public void imageOpened(ImagePlus imp) {
    // Check if the image has calibration data
    Calibration calibration = imp.getCalibration();
    if (calibration != null) {
        // Get the pixel size from the calibration and show it in the pixel size textbox
        double pixelSize = calibration.pixelWidth;
        pixelSizeTextField.setText(String.format("%.3f", pixelSize));
    }
}

@Override
public void imageUpdated(ImagePlus imp) {
        // Not needed for this implementation, but required to override the method
    }

 @Override
    public void imageClosed(ImagePlus imp) {
        // Not needed for this implementation, but required to override the method
    }

void selectImage(ImagePlus imp) {
    if (imp == null) {
        IJ.showMessage("ImagePlus is null. Cannot select the image.");
        return;
    }

    // Set the ImagePlus as the current active window
    WindowManager.setCurrentWindow(imp.getWindow());
}

public void runFilamentSensor() {
        try {
            String javaExecutable = "java"; // You might need to provide the full path to the Java executable
            String jarPath = "/Users/anmaleki/Documents/scripts/java/Main.jar";
            String modulePath = "/Users/anmaleki/Documents/scripts/java/javafx-sdk-20.0.1/lib";
            String addModules = "javafx.controls,javafx.fxml,javafx.base";
            
            ProcessBuilder processBuilder = new ProcessBuilder(
                javaExecutable,
                "--module-path", modulePath,
                "--add-modules", addModules,
                "-jar", jarPath
            );
            Process process = processBuilder.start();

            // You can optionally wait for the process to complete
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("External program executed successfully.");
            } else {
                System.out.println("External program execution failed.");
            }
            

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

void keepOnlyEventROIs() {

    RoiManager roiManager = RoiManager.getInstance();
    roiManager.runCommand(imp,"Show All with labels");
    Prefs.useNamesAsLabels = true;
    if (roiManager == null) {
        IJ.error("ROI Manager not found");
        return;
    }

    int roiCount = roiManager.getCount();
    List<Roi> roisToRemove = new ArrayList<>();

    for (int i = 0; i < roiCount; i++) {
        Roi roi = roiManager.getRoi(i);
        String name = roi.getName();
        if (name.length() > 0 && !Character.isLetter(name.charAt(0))) {
            roisToRemove.add(roi);
        }
        if (name.length() > 0 && name.startsWith("kymo_")) {
        roisToRemove.add(roi);
        }
    }


    for (Roi roi : roisToRemove) {
        int roiIndex = roiManager.getRoiIndex(roi);
        roiManager.select(roiIndex);
        roiManager.runCommand("Delete");
    }

    Roi currentROI = imp.getRoi();
    currentRoiIndex = roiManager.getRoiIndex(currentROI) ;
    imagePanel.repaint();
}

    void processMultipleEvents() {
        List<String> selectedCategories = new ArrayList<>();
        List<Integer> selectedSpinnerValues = new ArrayList<>();
        List<String> motherCategories = new ArrayList<>();

        for (Entry<JButton, JSpinner> entry : categorySpinners.entrySet()) {
            JButton categoryButton = entry.getKey();
            JSpinner spinner = entry.getValue();


            int spinnerValue = (int) spinner.getValue();
            if (spinnerValue > 0) {
                selectedCategories.add(categoryButton.getText());
                selectedSpinnerValues.add(spinnerValue);

                Container parent = categoryButton.getParent();
                Container grandParent = parent.getParent();
                String motherPanelName = grandParent.getName();
                motherCategories.add(motherPanelName);
            }
        }

        if (!selectedCategories.isEmpty()) {
            for (int i = 0; i < selectedCategories.size(); i++) {
                String category = selectedCategories.get(i);
                int spinnerValue = selectedSpinnerValues.get(i);
                String motherPanelName = motherCategories.get(i);

                for (int j = 0; j < spinnerValue; j++) {
                    saveSelectedRoi(category, motherPanelName);
                }
                roiManager = RoiManager.getInstance();
                roiManager.select(currentRoiIndex);
                Roi roi = roiManager.getRoi(currentRoiIndex);
                StringBuilder roiName = new StringBuilder();

                for (int j = 0; j < selectedCategories.size(); j++) {
                    if (j != 0) {
                        roiName.append(" + ");
                    }
                    roiName.append(motherCategories.get(j) + " " + selectedCategories.get(j));
                }
                roiManager.select(currentRoiIndex);
                roiManager.runCommand("Rename", roiName.toString() + " " + (currentRoiIndex + 1));


            }
        } else {
            JOptionPane.showMessageDialog(mainpanel, "No categories selected.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        boolean nextRoiExists = nextRoi();
        if(nextRoiExists){makeKymograph();}
        resetSpinners();
    }

    String getPanelNameForCategory(String category) {
        if (isPlusEndCategory(category)) {
            return "Plus End";
        } else if (isMinusEndCategory(category)) {
            return "Minus End";
        } else {
            return "";
        }
    }

    void clearCategories() {
        // Remove all category buttons from the panels
        plusEndButtonsPanel.removeAll();
        minusEndButtonsPanel.removeAll();
        otherButtonsPanel.removeAll();

        // Repaint the panels
        plusEndButtonsPanel.revalidate();
        plusEndButtonsPanel.repaint();
        minusEndButtonsPanel.revalidate();
        minusEndButtonsPanel.repaint();
        otherButtonsPanel.revalidate();
        otherButtonsPanel.repaint();

        currentCategory = null;

        // Reset the current ROI index
        currentRoiIndex = 0;
        resetSpinners();

        // Repaint the image panel
        imagePanel.repaint();

        // Reset any other necessary components or variables
    }

void resetPlugin() {
    // Reset any variables or settings here
    RoiManager roiManager = RoiManager.getInstance();



    // Reset the current ROI index
    currentRoiIndex = 0;

    // Get the list of all open images
    int[] openImageIDs = WindowManager.getIDList();
    if (openImageIDs != null) {
        for (int id : openImageIDs) {
            ImagePlus img = WindowManager.getImage(id);
            if (img != null) {
                int nSlices = img.getNSlices(); // Get the number of slices
                int nFrames = img.getNFrames(); // Get the number of frames
                if (nFrames <= 1) {
                    img.close(); // Close the image if it has 1 or fewer frames
                }
            }
        }
    }

    // Repaint the image panel
    imagePanel.repaint();
    imagePanel.removeAll();
    // Reset any other necessary components or variables
}



JScrollPane createImagePanel() {
    imagePanel = new ImagePanel();
    imagePanel.setBorder(new LineBorder(Color.BLACK, 1));

    // Calculate the threshold size for adding a scroll bar
    double thresholdWidth = Toolkit.getDefaultToolkit().getScreenSize().getWidth() * 2 / 3;
    double thresholdHeight = Toolkit.getDefaultToolkit().getScreenSize().getHeight() * 2 / 3;

    // Check if the image panel size exceeds the threshold, and add a scroll bar if needed
    if (imagePanel.getPreferredSize().getWidth() > thresholdWidth || imagePanel.getPreferredSize().getHeight() > thresholdHeight) {
        JScrollPane scrollPane = new JScrollPane(imagePanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }

    return new JScrollPane(imagePanel); // If no scroll bar needed, return the imagePanel itself in a JScrollPane
}

void showEventList() {

        if (imp != null) {
            String directory = imp.getOriginalFileInfo().directory;
            String eventDirectory = directory + "Kymograph";
            File eventDir = new File(eventDirectory);

            if (eventDir.exists() && eventDir.isDirectory()) {
                File[] subfolders = eventDir.listFiles((dir, name) -> name.startsWith("event_") && new File(dir, name).isDirectory());

                StringBuilder eventList = new StringBuilder();
                eventList.append("Event List\n");

                for (File subfolder : subfolders) {
                    File[] roiFiles = subfolder.listFiles((dir, name) -> name.endsWith(".roi"));
                    int eventCount = roiFiles != null ? roiFiles.length : 0;
                    String categoryName = subfolder.getName().substring(6);
                    eventList.append(categoryName).append(": ").append(eventCount).append(" events\n");
                }

                JOptionPane.showMessageDialog(mainpanel, eventList.toString(), "Event List", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(mainpanel, "No event folders found!", "Event List", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            IJ.noImage();
        }
    }


void showEventStatistics() {
    if (imp != null) {
        String directory = imp.getOriginalFileInfo().directory;
        String eventDirectory = directory + "Kymograph";
        File eventDir = new File(eventDirectory);

        if (eventDir.exists() && eventDir.isDirectory()) {
            File[] subfolders = eventDir.listFiles((dir, name) -> name.startsWith("event_") && new File(dir, name).isDirectory());

            Map<String, Integer> eventCounts = new HashMap<>();
            for (File subfolder : subfolders) {
                File[] roiFiles = subfolder.listFiles((dir, name) -> name.endsWith(".roi"));
                int eventCount = roiFiles != null ? roiFiles.length : 0;
                String categoryName = subfolder.getName().substring(6);
                eventCounts.put(categoryName, eventCount);
            }

            StringBuilder statistics = new StringBuilder();
            statistics.append("Category,Occurrences\n");

            // Add plus end categories in their original order
            for (String category : plusEndCategories) {
                String fullCategory = "Plus End " + category;
                int count = eventCounts.getOrDefault(fullCategory, 0);
                statistics.append(fullCategory).append(",").append(count).append("\n");
                eventCounts.remove(fullCategory); // Remove to track remaining categories
            }

            // Add minus end categories in their original order
            for (String category : minusEndCategories) {
                String fullCategory = "Minus End " + category;
                int count = eventCounts.getOrDefault(fullCategory, 0);
                statistics.append(fullCategory).append(",").append(count).append("\n");
                eventCounts.remove(fullCategory); // Remove to track remaining categories
            }

            // Add any remaining categories not in the predefined lists
            for (Map.Entry<String, Integer> entry : eventCounts.entrySet()) {
                String category = entry.getKey();
                if (!Arrays.asList(plusEndCategories).contains(category) && !Arrays.asList(minusEndCategories).contains(category)) {
                    statistics.append(category).append(",").append(entry.getValue()).append("\n");
                }
            }

            // Save statistics as a CSV file
            String csvFilePath = directory + "kymograph" + File.separator + "event_statistics.csv";

            // Ask the user whether to save the list or not
            int choice = JOptionPane.showConfirmDialog(mainpanel, csvFilePath + "\nDo you want to save the list?", "Event Statistics", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                // Save the list
                try (PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath))) {
                    writer.write(statistics.toString());
                    JOptionPane.showMessageDialog(mainpanel, "Event statistics saved as CSV file:\n" + csvFilePath, "Event Statistics", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    e.printStackTrace();
                    IJ.showMessage("Failed to save event list as CSV file.");
                }
            }
        } else {
            JOptionPane.showMessageDialog(mainpanel, "No event folders found!", "Event Statistics", JOptionPane.INFORMATION_MESSAGE);
        }
    } else {
        IJ.noImage();
    }
}



void extractROIs() {
        imp = WindowManager.getCurrentImage();
        if (imp == null) {
            IJ.error("No image open");
            return;
        }
        
        String imagePath = imp.getOriginalFileInfo().directory;
        String imageName = imp.getShortTitle();
        String csvPath = imagePath + "csv" + File.separator + imageName + "_filaments.csv";

        Calibration cal = imp.getCalibration();
        double pixelSize = cal.pixelWidth;

        // If calibration is not set, get the pixel size from the metadata
        if (pixelSize == 1.0) {
            String pixelSizeMetadata = (String) imp.getProperty("spatial-calibration-x");
            if (pixelSizeMetadata != null) {
                pixelSize = Double.parseDouble(pixelSizeMetadata);
            }
        }

        GenericDialog gd = new GenericDialog("Input Parameters");
        gd.addNumericField("Length Threshold (µm):", 7, 2);
        gd.addNumericField("Length Multiplier: ", 2, 2);
        gd.showDialog();
        if (gd.wasCanceled()) return;

        double lengthThresholdMicrons = gd.getNextNumber();
        double lengthMultiplier = gd.getNextNumber();
        double lengthThresholdPixels = lengthThresholdMicrons / pixelSize;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String line = null;
            roiManager = RoiManager.getInstance();
            if (roiManager == null) roiManager = new RoiManager();

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");

                // Skip if not enough data in the row
                if (data.length < 6) continue;

                // Try to parse the first column as an integer and skip the line if it fails
                try {
                    Integer.parseInt(data[0]);
                } catch (NumberFormatException ex) {
                    continue;
                }

                double length = Double.parseDouble(data[3]);
                if (length < lengthThresholdPixels) continue;
                double xCenter = Double.parseDouble(data[1]);
                double yCenter = Double.parseDouble(data[2]);
                length = length * lengthMultiplier;
                double angle = Double.parseDouble(data[4]);
                double width = Double.parseDouble(data[5]);

                
                double radAngle = -Math.toRadians(angle);
                double x1 = xCenter - length / 2 * Math.cos(radAngle);
                double y1 = yCenter - length / 2 * Math.sin(radAngle);
                double x2 = xCenter + length / 2 * Math.cos(radAngle);
                double y2 = yCenter + length / 2 * Math.sin(radAngle);

                Line roi = new Line(x1, y1, x2, y2);
                roi.setStrokeWidth(width);
                roiManager.addRoi(roi);
            }
        } catch (IOException ex) {
            IJ.error("Error reading file: " + csvPath);
        }
        renameROIs();
    }


    void processCategoryButtonClick(ActionEvent e) {
        JButton categoryButton = (JButton) e.getSource();
        String categoryName = categoryButton.getText();
        Container parent = categoryButton.getParent();
        Container grandParent = parent.getParent();
        String motherPanelName = grandParent.getName();

        // Rename the selected ROI
        if (!categoryName.equalsIgnoreCase("No event") && nextRoiExists) {
            roiManager.select(currentRoiIndex);
            Roi roi = roiManager.getRoi(currentRoiIndex);

            roiManager.runCommand("Rename",motherPanelName + " " + categoryName + " " + currentRoiIndex);
        }

        currentCategory = categoryName;

        // Get the spinner value for the category
        JSpinner spinner = categorySpinners.get(categoryButton);
        int spinnerValue = (int) spinner.getValue();

        // Save the selected ROI and generate kymographs based on the spinner value


        for (int i = 0; i < spinnerValue; i++) {
            saveSelectedRoi(categoryName, motherPanelName);

        }
        if (spinnerValue==0){
            saveSelectedRoi(categoryName, motherPanelName);
        }
        nextRoiExists = nextRoi();
        if(nextRoiExists){makeKymograph();}
        resetSpinners();
    }

void processNoEvent(ActionEvent e) {


    String categoryName = "No Event" ;
    String motherPanelName = "";

    saveSelectedRoi(categoryName, motherPanelName);

    boolean nextRoiExists = nextRoi();
    if(nextRoiExists){makeKymograph();}
    resetSpinners();

}

void saveSelectedRoi(String categoryName, String motherPanelName) {
    existingTempKymo = WindowManager.getImage("temp_kymo");
    selectImage(imp);
    String directory = imp.getOriginalFileInfo().directory;
    String roiDirectory = directory + "Kymograph" + File.separator + "event_" + motherPanelName + " " + categoryName; // Add stack name to ROI directory

    if (imp != null && roiManager != null && currentRoiIndex >= 0 && currentRoiIndex < roiManager.getCount()) {
        selectImage(imp);
        File roiDir = new File(roiDirectory);
        if (!roiDir.exists()) {
            roiDir.mkdirs();
        }
        Roi roi = roiManager.getRoi(currentRoiIndex);
        
        // Generate unique filename for ROI
        String baseRoiFilename = imp.getShortTitle() + "_" + motherPanelName + "_" + categoryName + "_" + (currentRoiIndex + 1);
        String roiFilename = roiDirectory + File.separator + baseRoiFilename + ".roi";
        int counter = 1;
        while (new File(roiFilename).exists()) {
            roiFilename = roiDirectory + File.separator + baseRoiFilename + "_" + counter + ".roi";
            counter++;
        }
        
        roiManager.select(currentRoiIndex);
        roiManager.runCommand("Save", roiFilename);

        if (existingTempKymo != null && !categoryName.equalsIgnoreCase("No event")) {
            selectImage(imp);
            String baseKymoPath = roiDirectory + File.separator + imp.getShortTitle() + "_Kymo_" + (currentRoiIndex + 1);
            String kymoPath = baseKymoPath + ".tif";
            counter = 1;
            while (new File(kymoPath).exists()) {
                kymoPath = baseKymoPath + "_" + counter + ".tif";
                counter++;
            }
            
            IJ.saveAs(existingTempKymo, "Tiff", kymoPath);
            existingTempKymo.setTitle("temp_kymo");
        }
    }
}



void addCategory() {
    String categoryName = JOptionPane.showInputDialog(mainpanel, "Enter category name:", "Add Category", JOptionPane.PLAIN_MESSAGE);
    if (categoryName != null && !categoryName.isEmpty()) {
        // Check if the category already exists
        if (isCategoryExists(categoryName)) {
            JOptionPane.showMessageDialog(mainpanel, "Category already exists!", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            // Create a new category button
            JButton categoryButton = new JButton(categoryName);
            categoryButton.addActionListener(this);

            // Create a new spinner with default value 0
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
            spinner.setValue(0);
            // Create a panel to hold the button and spinner
            JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.add(categoryButton, BorderLayout.CENTER);
            buttonPanel.add(spinner, BorderLayout.EAST);

            // Name the panel with the category name for easy identification
            buttonPanel.setName(categoryName);

            // Add the button panel to the appropriate parent panel
            if (isPlusEndCategory(categoryName)) {
                plusEndButtonsPanel.add(buttonPanel);
            } else if (isMinusEndCategory(categoryName)) {
                minusEndButtonsPanel.add(buttonPanel);
            } else {
                otherButtonsPanel.add(buttonPanel);
            }

            // Map the button to its corresponding spinner
            categorySpinners.put(categoryButton, spinner);

            // Revalidate and repaint the main panel to reflect changes
            mainpanel.revalidate();
            mainpanel.repaint();
        }
    }
}

void resetSpinners() {
    for (JSpinner spinner : categorySpinners.values()) {
        spinner.setValue(0);
    }
}

    boolean isCategoryExists(String category) {
        Component[] components;

        components = plusEndButtonsPanel.getComponents();
        for (Component component : components) {
            if (component instanceof JButton) {
                JButton button = (JButton) component;
                if (button.getText().equalsIgnoreCase(category)) {
                    return true;
                }
            }
        }

        components = minusEndButtonsPanel.getComponents();
        for (Component component : components) {
            if (component instanceof JButton) {
                JButton button = (JButton) component;
                if (button.getText().equalsIgnoreCase(category)) {
                    return true;
                }
            }
        }

        components = otherButtonsPanel.getComponents();
        for (Component component : components) {
            if (component instanceof JButton) {
                JButton button = (JButton) component;
                if (button.getText().equalsIgnoreCase(category)) {
                    return true;
                }
            }
        }

        return false;
    }

    boolean isPlusEndCategory(String category) {
        for (String plusEndCategory : plusEndCategories) {
            if (plusEndCategory.equalsIgnoreCase(category)) {
                return true;
            }
        }
        return false;
    }

    boolean isMinusEndCategory(String category) {
        for (String minusEndCategory : minusEndCategories) {
            if (minusEndCategory.equalsIgnoreCase(category)) {
                return true;
            }
        }
        return false;
    }

    void renameROIs() {

        if (imp != null && roiManager != null) {
            Roi[] rois = roiManager.getRoisAsArray();
            if (rois.length > 0) {
                String baseName = "kymo";
                if (baseName != null && !baseName.isEmpty()) {
                    for (int i = 0; i < rois.length; i++) {
                        roiManager.select(i);
                        String newName = baseName + "_" + (i + 1);
                        roiManager.runCommand("Rename", newName);
                    }
                    roiManager.runCommand("Update");
                }
            } else {
                JOptionPane.showMessageDialog(mainpanel, "No ROIs to rename!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    boolean nextRoi() {
        RoiManager roiManager = RoiManager.getInstance();

        if (imp != null && roiManager != null) {
            if (currentRoiIndex < roiManager.getCount()) {
                Roi roi = roiManager.getRoi(currentRoiIndex);
                currentRoiIndex++;
                roiManager.select(currentRoiIndex);
                return true;
            } else {
                JOptionPane.showMessageDialog(mainpanel, "No more ROIs!", "Error", JOptionPane.ERROR_MESSAGE);
                return false ;
            }
        }
        else {
            return false ;
        }

    }

    JButton createExtractButton() {
        extractButton = new JButton("Extract ROIs");
        extractButton.addActionListener(this);
        return extractButton;
    }

      JButton createFSButton() {
        FSButton = new JButton("Open Filament Sensor");
        FSButton.addActionListener(this);
        return FSButton;
    }

    JButton createNextButton() {
        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(this);
        return nextButton;
    }

    JButton createRenameROIsButton() {
        renameROIsButton = new JButton("Rename ROIs");
        renameROIsButton.addActionListener(this);
        return renameROIsButton;
    }


    JButton createShowStatisticsButton() {
        statisticsButton = new JButton("Show event statistics");
        statisticsButton.addActionListener(this);
        return statisticsButton;
    }

    JButton createMakeKymoButton() {
        makeKymoButton = new JButton("Make Kymo");
        makeKymoButton.addActionListener(this);
        return makeKymoButton;
    }

    JButton createResetButton() {
        resetButton = new JButton("Reset");
        resetButton.addActionListener(this);
        return resetButton;
    }

    JButton createAddCategoryButton() {
        addCategoryButton = new JButton("Add Category");
        addCategoryButton.addActionListener(this);
        return addCategoryButton;
    }

    JButton createClearCategoriesButton() {
        clearCategoriesButton = new JButton("Clear Categories");
        clearCategoriesButton.addActionListener(this);
        return clearCategoriesButton;
    }

    JButton createKeepOnlyEventROIsButton() {
    keepOnlyEventROIsButton = new JButton("Keep only event ROIs");
    keepOnlyEventROIsButton.addActionListener(this);
    return keepOnlyEventROIsButton;
    }

JSlider createLineWidthSlider() {
    lineWidthSlider = new JSlider(JSlider.HORIZONTAL, 1, 20, 3);
    lineWidthSlider.setMinorTickSpacing(1);
    lineWidthSlider.setMajorTickSpacing(5);
    lineWidthSlider.setPaintTicks(true);
    lineWidthSlider.setPaintLabels(true);
    lineWidthSlider.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
            int value = lineWidthSlider.getValue();
            lineWidthTextField.setText(String.valueOf(value));
        }
    });
    return lineWidthSlider;
}




JTextField createLineWidthTextField() {
    lineWidthTextField = new JTextField(1);
    lineWidthTextField.setHorizontalAlignment(JTextField.CENTER);
    lineWidthTextField.setEditable(true);
    int value = lineWidthSlider.getValue();
    lineWidthTextField.setText(String.valueOf(value));

    // Set preferred size to reduce the width
    Dimension preferredSize = lineWidthTextField.getPreferredSize();
    preferredSize.width = 25; // Adjust the desired width here
    lineWidthTextField.setPreferredSize(preferredSize);

    lineWidthSlider.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
            int value = lineWidthSlider.getValue();
            lineWidthTextField.setText(String.valueOf(value));
        }
    });

    return lineWidthTextField;
}

JPanel createOptionPanel() {
    JPanel optionPanel = new JPanel();
    optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.Y_AXIS));
    optionPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
    optionPanel.setPreferredSize(new Dimension(400, 900));

    // Create the "Tools" panel with FlowLayout
    JPanel toolsPanel = new JPanel();
    toolsPanel.setBorder(new TitledBorder("Tools"));
    //toolsPanel.setLayout(new BoxLayout(toolsPanel, BoxLayout.Y_AXIS));

       
    JButton FSButton = createFSButton();
    FSButton.setLayout(new FlowLayout(FlowLayout.LEFT));
    FSButton.setAlignmentX(Component.LEFT_ALIGNMENT);    

    // Create the panel to hold the buttons with BoxLayout
    JPanel buttonsPanel = new JPanel();
    buttonsPanel.setLayout(new GridLayout(0, 2, 4, 4)); // 0 rows, 2 columns, 10px horizontal gap, 10px vertical gap
    //buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
    buttonsPanel.add(FSButton);
    // Create the panel to hold the pixel size controls
    JPanel pixelSizePanel = new JPanel();
    pixelSizePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    pixelSizePanel.setBorder(new EmptyBorder(0, 10, 0, 0)); // Add left margin

    // Create the label for the pixel size controls
    JLabel pixelSizeLabel = new JLabel("Pixel Size (\u00B5m):"); // Unicode for micron symbol
    pixelSizeLabel.setPreferredSize(new Dimension(130, 30)); // Set preferred size for the label

    // Create the pixel size text field
    pixelSizeTextField = new JTextField(4);
    pixelSizeTextField.setEditable(true);


    // Create the 1.5x button
    scaleDownButton = new JButton("1.5x");
    scaleDownButton.setPreferredSize(new Dimension(60, 30));
    scaleDownButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get the current pixel size from the text field
            String pixelSizeText = pixelSizeTextField.getText();
            double currentPixelSize = Double.parseDouble(pixelSizeText);

            // Calculate the new pixel size by dividing the current pixel size by 1.5
            double newPixelSize = currentPixelSize / 1.5;

            // Update the text field with the new pixel size
            pixelSizeTextField.setText(String.format("%.3f", newPixelSize));
            imp = WindowManager.getCurrentImage();
            // Apply the new pixel size to the image
            if (imp != null) {
                Calibration calibration = imp.getCalibration();
                if (calibration != null) {
                    calibration.pixelWidth = newPixelSize;
                    calibration.pixelHeight = newPixelSize;
                    IJ.run(imp, "Set Scale...", "distance=1 known="+newPixelSize+" unit=micron");
                    imp.updateAndDraw();
                }
            }
        }
    });

    // Add the components to the pixel size panel
    pixelSizePanel.add(pixelSizeLabel);
    pixelSizePanel.add(pixelSizeTextField);
    pixelSizePanel.add(scaleDownButton);


    // Create the "Extract ROIs" button
    JButton extractButton = createExtractButton();
    extractButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    buttonsPanel.add(extractButton);

    // Create the "Make Kymo" button
    JButton makeKymoButton = createMakeKymoButton();
    makeKymoButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    buttonsPanel.add(makeKymoButton);

    // Create the "Rename ROIs" button
    JButton renameROIsButton = createRenameROIsButton();
    renameROIsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    buttonsPanel.add(renameROIsButton);

    // Create the "Show event statistics" button
    JButton showStatisticsButton = createShowStatisticsButton();
    showStatisticsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    buttonsPanel.add(showStatisticsButton);

    // Create the "Reset" button
    JButton resetButton = createResetButton();
    resetButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    buttonsPanel.add(resetButton);

    // Create the "Add Category" button
    JButton addCategoryButton = createAddCategoryButton();
    addCategoryButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    buttonsPanel.add(addCategoryButton);

    // Create the "Clear Categories" button
    JButton clearCategoriesButton = createClearCategoriesButton();
    clearCategoriesButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    buttonsPanel.add(clearCategoriesButton);

    // Create the "Keep Only Event ROIs" button
    JButton keepOnlyEventROIsButton = createKeepOnlyEventROIsButton();
    keepOnlyEventROIsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    buttonsPanel.add(keepOnlyEventROIsButton);


    // Set maximum size for all buttons in buttonsPanel
    Dimension buttonMaxSize = new Dimension(150, 30); // You can adjust the width and height as needed
    for (Component component : buttonsPanel.getComponents()) {
        if (component instanceof JButton) {
            JButton button = (JButton) component;
            button.setMaximumSize(buttonMaxSize);
            button.setMargin(new Insets(5, 10, 5, 10)); // Set margins (top, left, bottom, right)
        }
    }

    // Create the panel to hold the line width controls
    JPanel lineWidthPanel = new JPanel();
    lineWidthPanel.setLayout(new BoxLayout(lineWidthPanel, BoxLayout.X_AXIS));
    lineWidthPanel.setBorder(new EmptyBorder(0, 10, 0, 0)); // Add left margin
        





    // Create the label for the line width controls
    JLabel lineWidthLabel = new JLabel("Line Width:");

        // Create the line width slider and text field
    JSlider lineWidthSlider = createLineWidthSlider();
    lineWidthSlider.setMaximumSize(new Dimension(250, 50));
    JTextField lineWidthTextField = createLineWidthTextField();
    lineWidthTextField.setPreferredSize(new Dimension(100, 30)); // Set preferred size for the text field
    lineWidthTextField.setMaximumSize(new Dimension(100, 30)); // Set maximum size for the text field
    lineWidthLabel.setPreferredSize(new Dimension(100, 30)); // Set preferred size for the label
    lineWidthPanel.add(lineWidthLabel);
    lineWidthPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Add some spacing between label and slider
    lineWidthPanel.add(lineWidthSlider);
    lineWidthPanel.add(lineWidthTextField);
    lineWidthPanel.setMaximumSize(new Dimension(350, 50)); // Set maximum size for the text field
    lineWidthPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

    // Create the panel to hold the combo box
    JPanel comboBoxPanel = new JPanel();
    comboBoxPanel.setLayout(new BoxLayout(comboBoxPanel, BoxLayout.X_AXIS));

    // Create the label for the drop-down list
    JLabel intensityLabel = new JLabel("Kymograph Generation Method:");
    comboBoxPanel.add(intensityLabel);

    // Create the drop-down list and add options
    JComboBox<String> intensityComboBox = new JComboBox<>(new String[]{"Maximum", "Average"});
    // Set "Maximum" as the default selected item
    intensityComboBox.setSelectedItem("Maximum");
    // Add an action listener to the combo box
    intensityComboBox.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get the selected item from the combo box
            selectedMethod = (String) intensityComboBox.getSelectedItem();

        }
    });
    intensityComboBox.setPreferredSize(new Dimension(50, 30)); // Set preferred size for the combo box
    intensityComboBox.setMaximumSize(new Dimension(100, 30)); // Set maximum size for the combo box
    comboBoxPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Add some spacing between label and combo box
    comboBoxPanel.add(intensityComboBox);




    JPanel lineboxPanel = new JPanel();
    lineboxPanel.setLayout(new BoxLayout(lineboxPanel, BoxLayout.Y_AXIS));
    lineWidthPanel.setLayout(new FlowLayout(FlowLayout.CENTER));



        //lineboxPanel.add(emptyPanel);
        lineboxPanel.add(comboBoxPanel);
        lineboxPanel.add(lineWidthPanel);
        // Place the lineboxPanel to the right (EAST) of the main container using BorderLayout
    JPanel mainContainer = new JPanel(new BorderLayout());
    //mainContainer.add(imagePanel, BorderLayout.CENTER);
    mainContainer.setLayout(new FlowLayout(FlowLayout.LEFT));
    JPanel Toools = new JPanel(new BorderLayout());
    Toools.setLayout(new FlowLayout(FlowLayout.LEFT));

    mainContainer.add(lineboxPanel);
    toolsPanel.add(buttonsPanel);
    toolsPanel.add(pixelSizePanel);
    Toools.add(toolsPanel);
    optionPanel.add(Toools) ;
    optionPanel.add(pixelSizePanel);
    optionPanel.add(mainContainer);
        

    



    // Create the panel to hold plus end and minus end panels side by side
    JPanel plusMinusEndPanel = new JPanel();
    plusMinusEndPanel.setLayout(new BoxLayout(plusMinusEndPanel, BoxLayout.X_AXIS));
    plusMinusEndPanel.setBorder(new TitledBorder("Categories"));
    plusMinusEndPanel.setAlignmentX(Component.LEFT_ALIGNMENT);


     // Create the panel to hold plus end category buttons
    plusEndButtonsPanel = new JPanel();
    plusEndButtonsPanel.setLayout(new BoxLayout(plusEndButtonsPanel, BoxLayout.Y_AXIS));
    plusEndButtonsPanel.setBorder(new TitledBorder("Plus End"));
    plusEndButtonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    plusEndButtonsPanel.setName("Plus End");
    

    // Create the panel to hold minus end category buttons
    minusEndButtonsPanel = new JPanel();
    minusEndButtonsPanel.setLayout(new BoxLayout(minusEndButtonsPanel, BoxLayout.Y_AXIS));
    minusEndButtonsPanel.setBorder(new TitledBorder("Minus End"));
    minusEndButtonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    minusEndButtonsPanel.setName("Minus End");
    
    
    plusMinusEndPanel.add(plusEndButtonsPanel);
    plusMinusEndPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Add some spacing between panels
    plusMinusEndPanel.add(minusEndButtonsPanel);
    plusMinusEndPanel.setLayout(new FlowLayout(FlowLayout.LEFT));


    

    // Create the panel to hold other category buttons
    otherButtonsPanel = new JPanel() ;
    otherButtonsPanel.setLayout(new GridLayout(0, 2, 4, 4));
    otherButtonsPanel.setBorder(new TitledBorder("Other"));
    otherButtonsPanel.setLayout(new BoxLayout(otherButtonsPanel, BoxLayout.X_AXIS));
    otherButtonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    otherButtonsPanel.setName("");

    JPanel othercatPanel1 = new JPanel(new BorderLayout());
    othercatPanel1.setLayout(new BoxLayout(otherButtonsPanel, BoxLayout.X_AXIS));
    othercatPanel1.setLayout(new FlowLayout(FlowLayout.LEFT));

        // Create the panel for the "Multiple Events" button
    JPanel multipleEventsPanel = new JPanel();
    multipleEventsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    multipleEventsButton = new JButton("Multiple Events");
    multipleEventsButton.addActionListener(this);
    multipleEventsPanel.add(multipleEventsButton);

    noEventButton = new JButton("No Event");
    noEventButton.addActionListener(this);
    multipleEventsPanel.add(noEventButton) ;
    

    JPanel CtgPanel = new JPanel();
    CtgPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    buttonMaxSize = new Dimension(250, 25); // You can adjust the width and height as needed

    for (String category : plusEndCategories) {
        JButton categoryButton = new JButton(category);
        categoryButton.addActionListener(this);
        categoryButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Create a new spinner
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(categoryButton, BorderLayout.CENTER);
        buttonPanel.add(spinner, BorderLayout.EAST);
        buttonPanel.setName(category);
        plusEndButtonsPanel.add(buttonPanel);

        categorySpinners.put(categoryButton, spinner);
        if (category=="Growth" || category=="Shrinkage"){}

        else if (!category.contains("without NS") || category.equals("NS crossing")  ) {
            categoryButton.setBackground(Color.CYAN);
        }
    }

    // Add the default minus end categories with spinners
    for (String category : minusEndCategories) {
        JButton categoryButton = new JButton(category);
        categoryButton.addActionListener(this);
        categoryButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Create a new spinner
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(categoryButton, BorderLayout.CENTER);
        buttonPanel.add(spinner, BorderLayout.EAST);
        buttonPanel.setName(category);
        minusEndButtonsPanel.add(buttonPanel);

        categorySpinners.put(categoryButton, spinner);
        if (category=="Growth" || category=="Shrinkage"){}

        else if(!category.contains("without NS") || category.equals("NS crossing") ) {
            categoryButton.setBackground(Color.CYAN);
        }
    }

    // Set maximum size for all buttons in plusEndButtonsPanel
    for (Component component : plusEndButtonsPanel.getComponents()) {
        if (component instanceof JPanel) {
            JPanel panel = (JPanel) component;
            Component buttonComponent = panel.getComponent(0);
            if (buttonComponent instanceof JButton) {
                JButton button = (JButton) buttonComponent;
                button.setMaximumSize(buttonMaxSize);
                button.setMargin(new Insets(5, 10, 5, 10)); // Set margins (top, left, bottom, right)
            }
        }
    }
    // Set maximum size for all buttons in minusEndButtonsPanel
    for (Component component : minusEndButtonsPanel.getComponents()) {
        if (component instanceof JPanel) {
            JPanel panel = (JPanel) component;
            Component buttonComponent = panel.getComponent(0);
            if (buttonComponent instanceof JButton) {
                JButton button = (JButton) buttonComponent;
                button.setMaximumSize(buttonMaxSize);
                button.setMargin(new Insets(5, 10, 5, 10)); // Set margins (top, left, bottom, right)
            }
        }
    }

    // Add the default other categories with spinners
    for (String category : otherCategories) {
        JButton categoryButton = new JButton(category);
        categoryButton.addActionListener(this);
        categoryButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Create a new spinner
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(categoryButton, BorderLayout.CENTER);
        buttonPanel.add(spinner, BorderLayout.EAST);
        otherButtonsPanel.add(buttonPanel);

        categorySpinners.put(categoryButton, spinner);
    }


    intensityComboBox.setMaximumSize(new Dimension(400, 200));
    // Set maximum size for all buttons in otherButtonsPanel
    for (Component component : otherButtonsPanel.getComponents()) {
        if (component instanceof JPanel) {
            JPanel panel = (JPanel) component;
            Component buttonComponent = panel.getComponent(0);
            if (buttonComponent instanceof JButton) {
                JButton button = (JButton) buttonComponent;
                button.setMaximumSize(buttonMaxSize);
                button.setMargin(new Insets(5, 10, 5, 10)); // Set margins (top, left, bottom, right)
            }
        }
    }
    othercatPanel1.add(otherButtonsPanel);
    CtgPanel.add(plusMinusEndPanel);
    optionPanel.add(CtgPanel);
    optionPanel.add(othercatPanel1);
    //optionPanel.add(otherButtonsPanel);
    optionPanel.add(multipleEventsPanel);  

    return optionPanel;
}


private void setMaxButtonSize(JButton button) {
    button.setMaximumSize(buttonMaxSize);
    button.setPreferredSize(buttonMaxSize);
    button.setMinimumSize(buttonMaxSize);
}

private void setFixedButtonSize(JButton button, Dimension size) {
    button.setMinimumSize(size);
    button.setPreferredSize(size);
    button.setMaximumSize(size);
}

private JPanel createCategoryPanel(String title, String[] categories) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(new TitledBorder(title));
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);

    Dimension buttonSize = new Dimension(150, 30);

    for (String category : categories) {
        JButton categoryButton = new JButton(category);
        setFixedButtonSize(categoryButton, buttonSize);
        categoryButton.addActionListener(this);

        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        spinner.setMaximumSize(new Dimension(50, buttonSize.height));

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(categoryButton, BorderLayout.CENTER);
        buttonPanel.add(spinner, BorderLayout.EAST);
        buttonPanel.setName(category);
        panel.add(buttonPanel);

        categorySpinners.put(categoryButton, spinner);

        if (!category.contains("without NS") || category.equals("NS crossing")) {
            categoryButton.setBackground(Color.CYAN);
        }
    }

    return panel;
}


    // Update the makeKymograph() method to use the value from the slide bar for lineWidth

void makeKymograph() {
    
    if (existingTempKymo != null) {
        existingTempKymo.close();
    }


    selectImage(imp);
    if (imp == null) {
        IJ.noImage();
        return;
    }

    Roi roi = imp.getRoi();
    if (roi == null ) {
        IJ.showMessage("Line ROI required");
        return;
    }

    // Get the line width from the slide bar
    int lineWidth = Integer.parseInt(lineWidthTextField.getText());
    IJ.run(imp, "Line Width...", "line=" + lineWidth);
    roi = imp.getRoi();
    roi.setStrokeWidth(lineWidth);
    roi.setStrokeColor(new Color(0, 255, 255, 75));
    imp.updateAndDraw();

    // Run the KymoResliceWide command with the selected method
    IJ.run(imp, "KymoResliceWide", "line_width=" + lineWidth + " intensity=" + selectedMethod);
    IJ.run("Maximize");
    // Retrieve the resulting image
    ImagePlus kymograph = WindowManager.getCurrentImage();

    if (kymograph != null) {
        kymograph.setTitle("temp_kymo");
        // Copy metadata of the original file to the kymograph
        kymograph.copyScale(imp);
        kymograph.setCalibration(imp.getCalibration());
        FileInfo sourceFileInfo = imp.getOriginalFileInfo();
        FileInfo destinationFileInfo = kymograph.getOriginalFileInfo();
        if (destinationFileInfo == null) {
            destinationFileInfo = new FileInfo();
        }
        destinationFileInfo.fileName = sourceFileInfo.fileName;
        destinationFileInfo.directory = sourceFileInfo.directory;
        destinationFileInfo.url = sourceFileInfo.url;
        destinationFileInfo.fileFormat = sourceFileInfo.fileFormat;
        destinationFileInfo.fileType = sourceFileInfo.fileType;
        destinationFileInfo.width = kymograph.getWidth();
        destinationFileInfo.height = kymograph.getHeight();
        destinationFileInfo.nImages = kymograph.getStackSize();
        destinationFileInfo.gapBetweenImages = sourceFileInfo.gapBetweenImages;
        destinationFileInfo.pixelWidth = sourceFileInfo.pixelWidth;
        destinationFileInfo.pixelHeight = sourceFileInfo.pixelHeight;
        destinationFileInfo.unit = sourceFileInfo.unit;
        destinationFileInfo.valueUnit = sourceFileInfo.valueUnit;

        // Set the modified FileInfo to the kymograph
        kymograph.setFileInfo(destinationFileInfo);

        // Remove the existing ImageCanvases from imagePanel
        Component[] components = imagePanel.getComponents();
        for (Component component : components) {
            if (component instanceof ImageCanvas) {
                imagePanel.remove(component);
            }
        }

        // Create a panel to hold the kymograph channels
        JPanel kymographPanel = new JPanel();
        kymographPanel.setLayout(new FlowLayout());

  // Create an array to store the minimum and maximum pixel values for each channel
    double[] minPixelValues = new double[kymograph.getNChannels()];
    double[] maxPixelValues = new double[kymograph.getNChannels()];

    // Calculate the minimum and maximum pixel values for each channel
    for (int channel = 1; channel <= kymograph.getNChannels(); channel++) {
        ImageProcessor channelProcessor = kymograph.getStack().getProcessor(channel);
        ImageStatistics stats = channelProcessor.getStatistics();
        minPixelValues[channel - 1] = stats.min;
        maxPixelValues[channel - 1] = stats.max;
    }



    // Process and display each channel separately
    for (int channel = 1; channel <= kymograph.getNChannels(); channel++) {
        // Get the processor for the current channel
        ImageProcessor channelProcessor = kymograph.getStack().getProcessor(channel);

        // Create a new ImagePlus for the current channel
        ImagePlus channelImage = new ImagePlus("Channel " + channel, channelProcessor.duplicate());

        // Set the brightness and contrast of the channel's ImageProcessor
        channelImage.getProcessor().setMinAndMax(minPixelValues[channel - 1], maxPixelValues[channel - 1]);

        // Create an ImageCanvas for the current channel
        ImageCanvas channelCanvas = new ImageCanvas(channelImage);

        // Add the channel canvas to the kymograph panel
        kymographPanel.add(channelCanvas);
    }

        // Remove the existing panel from the imagePanel
        imagePanel.removeAll();

        // Add the new kymograph panel to the imagePanel
        imagePanel.add(kymographPanel);

        // Refresh the image panel
        imagePanel.revalidate();
        imagePanel.repaint();

            // Set kymograph window location to the rightmost side of the screen
    ImageWindow kymoWindow = kymograph.getWindow();
    if (kymoWindow != null) {
        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int kymoWidth = kymoWindow.getWidth();
        int xPosition = screenWidth - kymoWidth;
        kymoWindow.setLocation(xPosition, 0); // Adjust the y-position as needed
    }
    } else {
        IJ.showMessage("Kymograph could not be generated.");
    }
}




    class ImagePanel extends JPanel {

        Image image;

        void setImage(Image image) {
            this.image = image;
            repaint();
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                int x = (getWidth() - image.getWidth(null)) / 2;
                int y = (getHeight() - image.getHeight(null)) / 2;
                g.drawImage(image, x, y, this);
            }
        }

    }
}
