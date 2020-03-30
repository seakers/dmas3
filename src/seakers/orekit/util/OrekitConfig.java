/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.util;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import org.orekit.data.DataProvidersManager;
import seakers.orekit.parallel.ParallelRoutine;

/**
 * This class configures the paths required for orekit when loading data (e.g.
 * UTC, TLE)
 *
 * @author nozomihitomi
 */
public class OrekitConfig {

    /**
     * Class is only used for utilities. No need to create multiple instances
     */
    private OrekitConfig() {
    }
    
    /**
     * Initial configuration that configures the libraries from the development
     * environment Loads in default datasets
     */
    public static void init() {
        OrekitConfig.init(1, System.getProperty("user.dir"));
    }

    /**
     * Initial configuration that configures the libraries from the development
     * environment Loads in default datasets
     * @param numThreads the number of threads to initiate for analyses
     */
    public static void init(int numThreads) {
        OrekitConfig.init(numThreads, System.getProperty("user.dir"));
    }

    /**
     * Initial configuration that configures the libraries from the development
     * environment Loads in default datasets
     *
     * @param currentDirectory string of the current directory
     * @param numThreads the number of threads to initiate for analyses
     */
    public static void init(int numThreads, String currentDirectory) throws IllegalArgumentException {
        ParallelRoutine.getInstance(numThreads);
        
        StringBuffer pathBuffer = new StringBuffer();
        final File currrentDir = new File(currentDirectory);
        appendIfExists(pathBuffer, new File(currrentDir, "resources"));
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, pathBuffer.toString());
        
        final File covDB = new File(currrentDir, "CoverageDatabase");
        if (!covDB.isDirectory()) {
            //if coverage database doesn't exist, create a new directory
            boolean success = covDB.mkdir();
            if (!success) {
                throw new IllegalArgumentException(String.format("Expected coverage database to be a directory. Found %s to not be a directory.", covDB.getAbsolutePath()));
            }
        }
        System.setProperty("orekit.coveragedatabase", covDB.getAbsolutePath());
        final File res = new File(currrentDir, "results");
        if (!res.isDirectory()) {
            //if coverage database doesn't exist, create a new directory
            boolean success = res.mkdir();
            if (!success) {
                throw new IllegalArgumentException(String.format("Expected results directory. Found %s to not be a directory.", res.getAbsolutePath()));
            }
        }
        System.setProperty("results", res.getAbsolutePath());
    }
    
    /**
     * Releases any open resources
     */
    public static void end(){
        ParallelRoutine.shutDown();
    }

    /**
     * Append a directory/zip archive to the path if it exists.
     *
     * @param path placeholder where to put the directory/zip archive
     * @param file file to try
     */
    private static void appendIfExists(final StringBuffer path, final File file) {
        if (file.exists() && (file.isDirectory() || file.getName().endsWith(".zip"))) {
            path.append(System.getProperty("path.separator"));
            path.append(file.getAbsolutePath());
        }
    }

    /**
     * Append a classpath-related directory to the path if the directory exists.
     *
     * @param path placeholder where to put the directory
     * @param directory directory to try
     */
    private static void appendIfExists(final StringBuffer path, final String directory) {
        try {
            final URL url = OrekitConfig.class.getClassLoader().getResource(directory);
            if (url != null) {
                if (path.length() > 0) {
                    path.append(System.getProperty("path.separator"));
                }
                path.append(url.toURI().getPath());
            }
        } catch (URISyntaxException use) {
            // display an error message and simply ignore the path
            System.err.println(use.getLocalizedMessage());
        }
    }
}
