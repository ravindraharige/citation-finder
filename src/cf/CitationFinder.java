package cf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;

/*

     workflow-
     1. find all the journals "cited" from set A to set B (first requirement of Peter)
     2. find all the journals "cites" from set B to set A (extension)
     3. collect all the set A & set B journals in one tree list to get a unique set of journals (say set X)
     (1-3: findSetBjournals())

     4. use this super-set of journals as input to find citations data @processInput()
     here we find citations from set A to set B (@updateSetACitedJournalsMap)
     & set B to set A (@ updateSetACitesJournalsMap) - but in both the case set A = set B = set X from step:3

     however, the citations found in input files are recorded seperately in two maps
     1. setACitedJournalsMap (set A to set B)
     2. setAJournalCitesMap (set B to set A)

     the keys of both the maps are subset of set X
     such that treeset(map1.keyset(), map2.keyset()).size() == setX.size();

     5. next,
     for each "journal" in map1 & map2; (journal are keys of map1) @filterCandidates()

     a. we find total_citations
     b. we have threshold value read from input config. so a threshold-citation-value is calculated on total_citations
     c. check if the "journal".citation >= threshold-citation-value; if YES - it goes to the map1_filtered & map2_filtered global vars

     6. merge matrix - find common journals and merge their citations

     7. find A+ journal set now, and repeat the above processing to find citations within set A+
        merge citations

     8. finally, print the data in merge-matrix

*/

/**
 * @author Ravindra Harige
 *
 * TODO: Code Refactoring
 */
public class CitationFinder extends CFLogger{



    private String inputFile = "";
    private double cited_threshold;
    private double citing_threshold;

    private double INPUT_CITED_THRESHOLD;
    private double INPUT_CITING_THRESHOLD;

    private int total_cited, total_cites;

    private TreeSet<String> setAJournals = new TreeSet<String>();
    private TreeSet<String> set_A_plus_journals = new TreeSet<String>();
    private TreeSet<String> setBJournals = new TreeSet<String>();
    private HashMap<String, HashMap<String, Integer>> setACitedJournalsMap = new HashMap<String, HashMap<String, Integer>>();
    private HashMap<String, HashMap<String, Integer>> setAJournalCitesMap = new HashMap<String, HashMap<String, Integer>>();

    private HashMap<String, HashMap<String, Integer>> setACitedJournalsMap_filtered = new HashMap<String, HashMap<String, Integer>>();
    private HashMap<String, HashMap<String, Integer>> setAJournalCitesMap_filtered = new HashMap<String, HashMap<String, Integer>>();

    private HashMap<String, HashMap<String, Integer>> setAplusCitedJournalsMap = new HashMap<String, HashMap<String, Integer>>();
    private HashMap<String, HashMap<String, Integer>> setAplusJournalCitesMap = new HashMap<String, HashMap<String, Integer>>();

    private HashMap<String, HashMap<String, Integer>> merged_matrix = new HashMap<String, HashMap<String, Integer>>();

    public static void main(String[] args) {
        new CitationFinder();
    }

    public CitationFinder() {
        initLog(Constants.log_file);
        readConfig();
        System.out.println("processing..please wait");
        processSetAJourals();
        processSetAplusJournals();
        printResults();
        closeLog();
        System.out.println("Done! [check output files in .\\data directory]");
    }

    private void processSetAJourals(){
        findSetBJournals();
        processInput(setAJournals);
        filterCandidates();
        mergeJournalCitations();
    }
    private void findSetBJournals() {
        try {
            int lineC = 0;
            String line;
            FileInputStream fis = new FileInputStream(Constants.rootDir + File.separator + inputFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));

            // this loop basically creates a shortlist of input file records - limiting it to only the journals from SET A
            loop:
            while ((line = br.readLine()) != null) {

                line = line.toLowerCase().trim();
                String[] record = line.split(",");

                // another bug, first field empty.
                if (record[0].trim().length() == 0) {
                    continue loop;
                }

                //there was a bug in input file for entries like: [am j public health,j occup med toxicol,,2]
                if (record[2].trim().length() == 0 && record.length == 4) {
                    record[2] = record[3];
                }

                record[0] = record[0].trim();
                record[1] = record[1].trim();
                record[2] = record[2].trim();

                try {
                    if (lineC++ == 0) {
                        continue loop;
                    }

                    // shortlisting of set A journals - X cited B, where X belongs to set A
                    if (setAJournals.contains(record[0])) {
                        setBJournals.add(record[1]);
                        total_cited++;
                    }

                    // shortlisting of set A journals - B cites X, where X belongs to set A
                    if (setAJournals.contains(record[1])) {
                        setBJournals.add(record[0]);
                        total_cites++;
                    }

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    System.out.println("ERROR>>" + line);
                    continue;
                }

            }
            // File IO cleanup
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processInput(TreeSet<String> inputJournalsSet) {
        try {
            total_cited = total_cites = 0;
            int lineC = 0;
            String line;
            FileInputStream fis = new FileInputStream(Constants.rootDir + File.separator + inputFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));

            // this loop basically creates a shortlist of input file records - limiting it to only the journals from SET A
            loop:
            while ((line = br.readLine()) != null) {
                if (lineC++ == 0) {
                    continue loop;
                }

                line = line.toLowerCase().trim();
                String[] record = line.split(",");

                // another bug, first field empty.
                if (record[0].trim().length() == 0) {
                    continue loop;
                }

                //there was a bug in input file for entries like: [am j public health,j occup med toxicol,,2]
                if (record[2].trim().length() == 0 && record.length == 4) {
                    record[2] = record[3];
                }

                record[0] = record[0].trim();
                record[1] = record[1].trim();
                record[2] = record[2].trim();

                try {

                    // shortlising of set A journals - X cited B, where X belongs to set A
                    if (inputJournalsSet.contains(record[0])) {
                        updateSetACitedJournalsMap(record);
                        total_cited++;
                    }

                    // shortlising of set A journals - B cites X, where X belongs to set A
                    if (inputJournalsSet.contains(record[1])) {
                        updateSetACitesJournalsMap(record);
                        total_cites++;
                    }

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
//                    System.exit(0);
                    System.out.println("ERROR>>" + line);
//                    continue;
                }

            }

            // File IO cleanup
            br.close();
            br = null;
            fis = null;


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void filterCandidates() {
        //find total cited- citations
        int total_cited_c1 = 0;
        int total_cited_c2 = 0;

        for (String journalA : setACitedJournalsMap.keySet()) {
            int c1 = 0, c2 = 0;
            int total_cited_local = 0;
            HashMap<String, Integer> citedJournal = setACitedJournalsMap.get(journalA);

            HashMap<String, Integer> citedJournal_filtered = new HashMap<String, Integer>();

            for (String citedJournalName : citedJournal.keySet()) {
                c1++;
                Integer citation_count = citedJournal.get(citedJournalName);
                total_cited_local += citation_count.intValue();
                total_cited_c1 += c1;
            }

            cited_threshold = (INPUT_CITED_THRESHOLD / 100) * total_cited_local;

            for (String citedJournalName : citedJournal.keySet()) {
                Integer citation_count = citedJournal.get(citedJournalName);
                if (citation_count.intValue() >= cited_threshold) {
                    c2++;
                    total_cited_c2 += c2;
                    citedJournal_filtered.put(citedJournalName, citation_count);

                }
            }
            if (citedJournal_filtered.size() > 0) {
                setACitedJournalsMap_filtered.put(journalA, citedJournal_filtered);
            }

        }

        //--cites
        total_cited_c1 = 0;
        total_cited_c2 = 0;
        int k = 0;
        for (String journalA : setAJournalCitesMap.keySet()) {
            int c1 = 0, c2 = 0;
            int total_cites_local = 0;
            HashMap<String, Integer> citesJournal = setAJournalCitesMap.get(journalA);

            HashMap<String, Integer> citesJournal_filtered = new HashMap<String, Integer>();

            for (String citesJournalName : citesJournal.keySet()) {
                c1++;
                Integer citation_count = citesJournal.get(citesJournalName);
                total_cites_local += citation_count.intValue();
                total_cited_c1 += c1;
            }

            citing_threshold = (INPUT_CITING_THRESHOLD / 100) * total_cites_local;

            for (String citesJournalName : citesJournal.keySet()) {
                Integer citation_count = citesJournal.get(citesJournalName);
                if (citation_count.intValue() >= citing_threshold) {
                    c2++;
                    citesJournal_filtered.put(citesJournalName, citation_count);
                    total_cited_c2 += c2;
                }
            }
            if (citesJournal_filtered.size() > 0) {
                setAJournalCitesMap_filtered.put(journalA, citesJournal_filtered);
            } else {
                k++;
            }
            total_cited += total_cites_local;

        }
    }

    private void mergeJournalCitations(){
        merged_matrix.putAll(setACitedJournalsMap_filtered);

        //get all unique row/column headers in one list;
        TreeSet<String> uniqueKeys = new TreeSet<String>();
        uniqueKeys.addAll(setACitedJournalsMap_filtered.keySet());
        uniqueKeys.addAll(setAJournalCitesMap_filtered.keySet());

        //merge citation counts for the common keys in map1_filtered & map2_filtered
        for (String key : uniqueKeys) {
            HashMap<String, Integer> citations = null;
            if (setAJournalCitesMap_filtered.containsKey(key)) {
                citations = setAJournalCitesMap_filtered.get(key);

                if (merged_matrix.containsKey(key)) {

                    HashMap<String, Integer> mm_citations = merged_matrix.get(key);

                    for (String cc_key : citations.keySet()) {
                        if (mm_citations.containsKey(cc_key)) {
                            Integer cc_i = citations.get(cc_key);
                            Integer mm_i = mm_citations.get(cc_key);
                            mm_citations.put(cc_key, mm_i.intValue() + cc_i.intValue());
                        } else {
                            mm_citations.put(cc_key, citations.get(cc_key));
                        }
                    }
                    merged_matrix.put(key, mm_citations);
                } else { //add entries unique to map2_filtered into merged_matrix
                    merged_matrix.put(key, citations);
                }
            }
        }
    }

    //---extension
    private void processSetAplusJournals() {

        //init set A+ journals set
        set_A_plus_journals.addAll(setAJournals);
        TreeSet<String> localSetB = new TreeSet<String>();
        for (String setAjournal : merged_matrix.keySet()) {
            HashMap<String, Integer> citations = merged_matrix.get(setAjournal);
            for (String setBjournal : citations.keySet()) {
                set_A_plus_journals.add(setBjournal);
                localSetB.add(setBjournal);
            }
        }

        try {

            int lineC = 0;
            String line;
            FileInputStream fis = new FileInputStream(Constants.rootDir + File.separator + inputFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));

            // this loop basically creates a shortlist of input file records - limiting it to only the journals from SET A
            loop:
            while ((line = br.readLine()) != null) {
                if (lineC++ == 0) {
                    continue loop;
                }

                line = line.toLowerCase().trim();
                String[] record = line.split(",");

                // another bug, first field empty.
                if (record[0].trim().length() == 0) {
                    continue loop;
                }

                //there was a bug in input file for entries like: [am j public health,j occup med toxicol,,2]
                if (record[2].trim().length() == 0 && record.length == 4) {
                    record[2] = record[3];
                }

                record[0] = record[0].trim();
                record[1] = record[1].trim();
                record[2] = record[2].trim();

                try {

                    if (localSetB.contains(record[0]) && set_A_plus_journals.contains(record[1])) {
                        String jA = record[0];
                        String jB = record[1];
                        String c = record[2];
                        HashMap<String, Integer> citedJournal;
                        if (setAplusCitedJournalsMap.containsKey(jA)) {
                            citedJournal = setAplusCitedJournalsMap.get(jA);
                        } else {
                            citedJournal = new HashMap<String, Integer>();
                        }
                        if (citedJournal.containsKey(jB)) {
                            Integer i = citedJournal.get(jB);
                            citedJournal.put(jB, i.intValue() + Integer.parseInt(c));
                        } else {
                            citedJournal.put(jB, new Integer(c));
                        }
                        setAplusCitedJournalsMap.put(jA, citedJournal);
                    } else if (localSetB.contains(record[1]) && set_A_plus_journals.contains(record[0])) {
                        String jA = record[1];
                        String jB = record[0];

                        HashMap<String, Integer> journalCites;
                        if (setAplusJournalCitesMap.containsKey(jA)) {
                            journalCites = setAplusJournalCitesMap.get(jA);
                        } else {
                            journalCites = new HashMap<String, Integer>();
                        }
                        if (journalCites.containsKey(jB)) {
                            Integer i = journalCites.get(jB);
                            journalCites.put(jB, i.intValue() + 1);

                        } else {
                            journalCites.put(jB, new Integer(1));
                        }
                        setAplusJournalCitesMap.put(jA, journalCites);
                    }

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                    System.out.println("ERROR>>" + line);
                }
            }

            merged_matrix = new HashMap<String, HashMap<String, Integer>>();
            merged_matrix.putAll(setAplusCitedJournalsMap);

            //get all unique row/column headers in one list;
            TreeSet<String> uniqueKeys = set_A_plus_journals;

            //merge citation counts for the common keys
            for (String key : uniqueKeys) {
                HashMap<String, Integer> citations = null;
                if (setAplusJournalCitesMap.containsKey(key)) {
                    citations = setAplusJournalCitesMap.get(key);

                    if (merged_matrix.containsKey(key)) {

                        HashMap<String, Integer> mm_citations = merged_matrix.get(key);

                        for (String cc_key : citations.keySet()) {
                            if (mm_citations.containsKey(cc_key)) {
                                Integer cc_i = citations.get(cc_key);
                                Integer mm_i = mm_citations.get(cc_key);
                                mm_citations.put(cc_key, mm_i.intValue() + cc_i.intValue());
                            } else {
                                mm_citations.put(cc_key, citations.get(cc_key));
                            }
                        }
                        merged_matrix.put(key, mm_citations);
                    } else { //add entries unique to map2_filtered into merged_matrix
                        merged_matrix.put(key, citations);
                    }
                }
            }

            // File IO cleanup
            br.close();
            fis.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void printResults(){
        int x = 0;
        changeLog(Constants.out_file);
        plog("Journal Name (set A+), Cited by Journal (Set A+), Citation Count");

        for (String setAjournal : merged_matrix.keySet()) {
            HashMap<String, Integer> citations = merged_matrix.get(setAjournal);
            for (String setBjournal : citations.keySet()) {
                plog(setAjournal.toUpperCase() + ", " + setBjournal.toUpperCase() + ", " + citations.get(setBjournal));
                x++;
            }
        }
        System.out.println(x + " citation entries written to output file.");

    }
    private void readConfig() {
        Properties prop = new Properties();

        try {
            //load a properties file
            prop.load(new FileInputStream(Constants.config));
            Iterator ie = prop.keySet().iterator();
            while (ie.hasNext()) {
                Object o = ie.next();

                if (o.toString().equals(Constants.PROP_CITED_THRESHOLD)) {
                    INPUT_CITED_THRESHOLD = Float.parseFloat(prop.getProperty(o.toString()));
                    plog("User defined cited threshold = " + cited_threshold);
                } else if (o.toString().equals(Constants.PROP_CITING_THRESHOLD)) {
                    INPUT_CITING_THRESHOLD = Float.parseFloat(prop.getProperty(o.toString()));
                    plog("User defined citing threshold = " + citing_threshold);
                } else if (o.toString().equals(Constants.PROP_INPUT)) {
                    inputFile = prop.getProperty(o.toString());
                } else {

                    setAJournals.add(prop.getProperty(o.toString()).toLowerCase().trim());

                }
            }

        } catch (IOException ex) {
            System.out.println("Could not read input properties file! Make sure valid input.properties file is present in current directory");
            System.exit(0);
        }
    }

    private void updateSetACitedJournalsMap(String[] record) throws Exception {

        String jA = record[0];
        String jB = record[1];
        String c = record[2];

        HashMap<String, Integer> citedJournal;
        if (setACitedJournalsMap.containsKey(jA)) {
            citedJournal = setACitedJournalsMap.get(jA);
        } else {
            citedJournal = new HashMap<String, Integer>();
        }
        if (citedJournal.containsKey(jB)) {
            Integer i = citedJournal.get(jB);
            citedJournal.put(jB, i.intValue() + Integer.parseInt(c));
        } else {
            citedJournal.put(jB, new Integer(c));
        }
        setACitedJournalsMap.put(jA, citedJournal);

    }

    private void updateSetACitesJournalsMap(String[] record) throws Exception {

        String jA = record[1];
        String jB = record[0];

        // don't add record if journal cites itself
        if (jA.equals(jB)) {
            return;
        }
        HashMap<String, Integer> journalCites;
        if (setAJournalCitesMap.containsKey(jA)) {
            journalCites = setAJournalCitesMap.get(jA);
        } else {
            journalCites = new HashMap<String, Integer>();
        }
        if (journalCites.containsKey(jB)) {
            Integer i = journalCites.get(jB);
            journalCites.put(jB, i.intValue() + 1);

        } else {
            journalCites.put(jB, new Integer(1));
        }
        setAJournalCitesMap.put(jA, journalCites);
    }
}
