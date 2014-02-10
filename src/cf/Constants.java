package cf;/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.File;

/**
 *
 * @author Ravindra Harige
 */
public class Constants {
    public static String PROP_CITING_THRESHOLD  = "citing_threshold";
    public static String PROP_CITED_THRESHOLD  = "cited_threshold";
    public static String PROP_INPUT = "inputfile";
    public static final String config   = "input.properties";
    public static final String outFile  = "output.txt";
    public static final String rootDir  = "data";
    public static final String log_file = rootDir+File.separator+"log.txt";
    public static final String out_file = rootDir+File.separator+"output.txt";
}
