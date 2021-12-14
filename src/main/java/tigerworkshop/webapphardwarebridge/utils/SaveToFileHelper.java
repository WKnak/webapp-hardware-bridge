package tigerworkshop.webapphardwarebridge.utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.responses.PrintDocument;
import tigerworkshop.webapphardwarebridge.services.DocumentService;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.print.PrinterAbortException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

public class SaveToFileHelper {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String SAVE_TO_DISK_PRINTER_KEY = "Save to File";
    public static final String SAVE_TO_DISK_PRINTER_DESCRIPTION = "[ " + SAVE_TO_DISK_PRINTER_KEY +" ]";

    public static final HashMap<String, String> recentSavedFolders = new HashMap<String, String>();
    public static String RECENT_FOLDER_KEY = "_RECENT_FOLDER_KEY";

    /**
     *  Save to File - saves the original file to disk
     */
    public static void showSaveToFileDialog(PrintDocument printDocument) throws Exception {

        String rememberPrinterSaveLocation = printDocument.getType();

        JFrame parentFrame = new JFrame();

        String url = printDocument.getUrl();
        String filename = url.substring(url.lastIndexOf("/") + 1);

        String suggestedFileExtension = FilenameUtils.getExtension(filename);
        String suggestedFileName = Strings.isNotBlank(filename) ? filename : printDocument.getType();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a destination");

        if(Strings.isNotBlank(suggestedFileExtension)) {
            FileNameExtensionFilter filter = new FileNameExtensionFilter(suggestedFileExtension.toUpperCase() + " file", suggestedFileExtension.toLowerCase());
            fileChooser.setFileFilter(filter);
        } else {
            FileNameExtensionFilter filter = new FileNameExtensionFilter("All file types", " file", ".*");
            fileChooser.setFileFilter(filter);
        }

        fileChooser.setSelectedFile(new File(suggestedFileName));

        if (recentSavedFolders.containsKey(rememberPrinterSaveLocation)) {
            // found a recent folder associated with print type
            File initialDir = new File(recentSavedFolders.get(rememberPrinterSaveLocation));
            if (initialDir.exists()) {
                fileChooser.setCurrentDirectory(initialDir);
            }
        } else if (recentSavedFolders.containsKey(RECENT_FOLDER_KEY)) {
            // use the last used folder, if exists
            File initialDir = new File(recentSavedFolders.get(RECENT_FOLDER_KEY));
            if (initialDir.exists()) {
                fileChooser.setCurrentDirectory(initialDir);
            }
        }

        int userSelection = fileChooser.showSaveDialog(parentFrame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File destinationFile = fileChooser.getSelectedFile();
            File parentFolder = destinationFile.getParentFile();

            String localTempFilename = DocumentService.getPathFromUrl(printDocument.getUrl());
            File tempLocalFile = new File(localTempFilename);

            // verify if the local temp file still exists
            if(tempLocalFile.exists()) {
                // add print type recent folder
                recentSavedFolders.put(rememberPrinterSaveLocation, parentFolder.getAbsolutePath());
                // also add last folder
                recentSavedFolders.put(RECENT_FOLDER_KEY, parentFolder.getAbsolutePath());

                // todo: persist recent recentSavedFolders to user preference

                try {
                    Files.copy(tempLocalFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    LoggerFactory.getLogger(SaveToFileHelper.class).info("Document " + filename + " saved to file " + destinationFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if( userSelection == JFileChooser.CANCEL_OPTION ) {
            throw new PrinterAbortException();
        } else {
            throw new Exception("Error saving file to file");
        }
    }
}
