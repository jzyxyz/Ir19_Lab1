/**
 *   Computes the Hubs and Authorities for an every document in a query-specific
 *   link graph, induced by the base set of pages.
 *
 *   @author Dmytro Kalpakchi
 */

package ir;

import java.util.*;
import java.io.*;
import java.lang.Math;

public class HITSRanker {

    /**
     * Max number of iterations for HITS
     */
    final static int MAX_NUMBER_OF_STEPS = 1000;

    private final String DIR = "./links/";
    private final String LINKFILENAME = "linksDavis.txt";
    private final String CONVERTFILENAME = "docId-linkId.txt";
    /**
     * Convergence criterion: hub and authority scores do not change more that
     * EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.001;

    /**
     * The inverted index
     */
    Index index;

    /**
     * Mapping from the titles to internal document ids used in the links file
     */

    HashMap<Integer, Integer> linkIdToDocId = new HashMap<Integer, Integer>();
    HashMap<Integer, Integer> docIdToLinkId = new HashMap<Integer, Integer>();
    /**
     * Link from this doc TO others
     */
    HashMap<Integer, HashMap<Integer, Boolean>> linkFromIdToOther = new HashMap<Integer, HashMap<Integer, Boolean>>();
    /**
     * Link FROM other docs to this
     */
    HashMap<Integer, HashMap<Integer, Boolean>> linkToIdFromOther = new HashMap<Integer, HashMap<Integer, Boolean>>();

    /**
     * Sparse vector containing hub scores
     */
    HashMap<Integer, Double> hubs;

    /**
     * Sparse vector containing authority scores
     */
    HashMap<Integer, Double> authorities;

    /* --------------------------------------------- */

    /**
     * Constructs the HITSRanker object
     * 
     * A set of linked documents can be presented as a graph. Each page is a node in
     * graph with a distinct nodeID associated with it. There is an edge between two
     * nodes if there is a link between two pages.
     * 
     * Each line in the links file has the following format:
     * nodeID;outNodeID1,outNodeID2,...,outNodeIDK This means that there are edges
     * between nodeID and outNodeIDi, where i is between 1 and K.
     * 
     * Each line in the titles file has the following format: nodeID;pageTitle
     * 
     * NOTE: nodeIDs are consistent between these two files, but they are NOT the
     * same as docIDs used by search engine's Indexer
     *
     * @param linksFilename  File containing the links of the graph
     * @param titlesFilename File containing the mapping between nodeIDs and pages
     *                       titles
     * @param index          The inverted index
     */
    public HITSRanker() {
        readDocs();
    }

    /**
     * Reads the files describing the graph of the given set of pages.
     *
     * @param linksFilename  File containing the links of the graph
     * @param titlesFilename File containing the mapping between nodeIDs and pages
     *                       titles
     */
    void readDocs() {
        String linksFilename = DIR + LINKFILENAME;
        String convertFilename = DIR + CONVERTFILENAME;
        try {
            System.err.println("Reading file... " + linksFilename);
            BufferedReader in = new BufferedReader(new FileReader(linksFilename));
            String line;
            while ((line = in.readLine()) != null) {
                int index = line.indexOf(";");
                int from = Integer.valueOf(line.substring(0, index));
                linkFromIdToOther.put(from, new HashMap<Integer, Boolean>());
                // Check all outlinks.
                StringTokenizer tok = new StringTokenizer(line.substring(index + 1), ",");
                while (tok.hasMoreTokens()) {
                    int to = Integer.valueOf(tok.nextToken());
                    if (linkToIdFromOther.get(to) == null) {
                        linkToIdFromOther.put(to, new HashMap<Integer, Boolean>());
                    }
                    linkFromIdToOther.get(from).put(to, true);
                    linkToIdFromOther.get(to).put(from, true);
                }
            }
            System.err.println("Reading file... " + convertFilename);
            in = new BufferedReader(new FileReader(convertFilename));
            while ((line = in.readLine()) != null) {
                String[] arr = line.split(";");
                linkIdToDocId.put(Integer.valueOf(arr[1]), Integer.valueOf(arr[0]));
                docIdToLinkId.put(Integer.valueOf(arr[0]), Integer.valueOf(arr[1]));
            }
            System.err.println("done. ");
        } catch (FileNotFoundException e) {
            System.err.println("File " + linksFilename + " not found!");
        } catch (IOException e) {
            System.err.println("Error reading file " + linksFilename);
        }

    }

    /**
     * Perform HITS iterations until convergence
     *
     * @param titles The titles of the documents in the root set
     */
    private void iterate(HashSet<Integer> baseSet) {
        HashMap<Integer, Double> _hubs = new HashMap<Integer, Double>();
        HashMap<Integer, Double> _authorities = new HashMap<Integer, Double>();
        authorities = new HashMap<Integer, Double>();
        hubs = new HashMap<Integer, Double>();
        for (Integer id : baseSet) {
            authorities.put(id, 1.0);
            hubs.put(id, 1.0);
            _authorities.put(id, 0.0);
            _hubs.put(id, 0.0);
        }
        int count = 0;
        while (count++ < MAX_NUMBER_OF_STEPS) {
            boolean authOK = hasConverged(authorities, _authorities);
            boolean hubsOK = hasConverged(hubs, _hubs);
            if (authOK && hubsOK)
                break;
            if (!authOK) {
                _authorities = copy(authorities);
                for (Integer authId : authorities.keySet()) {
                    double newAuth = 0d;
                    HashMap<Integer, Boolean> hubMap = linkToIdFromOther.get(authId);
                    if (hubMap != null) {
                        for (Integer hubId : hubMap.keySet()) {
                            if (hubs.get(hubId) != null)
                                newAuth += hubs.get(hubId);
                        }
                    }
                    authorities.put(authId, newAuth);
                }
                normalize(authorities);
            }
            if (!hubsOK) {
                _hubs = copy(hubs);
                for (Integer hubId : hubs.keySet()) {
                    double newHub = 0d;
                    HashMap<Integer, Boolean> authMap = linkFromIdToOther.get(hubId);
                    if (authMap != null) {
                        for (Integer authId : authMap.keySet()) {
                            if (authorities.get(authId) != null)
                                newHub += authorities.get(authId);
                        }
                    }
                    hubs.put(hubId, newHub);
                }
                normalize(hubs);
            }
        }

    }

    private void normalize(HashMap<Integer, Double> map) {
        double sum = 0d;
        for (Integer id : map.keySet()) {
            sum += map.get(id) * map.get(id);
        }
        sum = Math.sqrt(sum);
        for (Integer id : map.keySet()) {
            map.put(id, map.get(id) / sum);
        }
    }

    private HashMap<Integer, Double> copy(HashMap<Integer, Double> map) {
        HashMap<Integer, Double> copy = new HashMap<Integer, Double>();
        for (Integer key : map.keySet()) {
            copy.put(key, map.get(key));
        }
        return copy;
    }

    private boolean hasConverged(HashMap<Integer, Double> map1, HashMap<Integer, Double> map2) {
        double sum = 0d;
        for (Integer key : map1.keySet()) {
            sum += (map1.get(key) - map2.get(key)) * (map1.get(key) - map2.get(key));
        }
        return Math.sqrt(sum) < EPSILON ? true : false;
    }

    /**
     * Rank the documents in the subgraph induced by the documents present in the
     * postings list `post`.
     *
     * @param post The list of postings fulfilling a certain information need
     *
     * @return A list of postings ranked according to the hub and authority scores.
     */
    PostingsList rank(Set<Integer> rootSet) {
        System.out.println("root set size: " + rootSet.size());
        PostingsList result = new PostingsList();
        HashSet<Integer> baseSet = new HashSet<Integer>();
        for (Integer idxID : rootSet) {
            int linkId = docIdToLinkId.get(idxID);
            baseSet.add(linkId);
            if (linkFromIdToOther.get(linkId) != null)
                baseSet.addAll(linkFromIdToOther.get(linkId).keySet());
            if (linkToIdFromOther.get(linkId) != null)
                baseSet.addAll(linkToIdFromOther.get(linkId).keySet());
        }
        System.out.println("base set size: " + baseSet.size());
        // initiate from the root set to the base set, then iterate
        iterate(baseSet);
        HashMap<Integer, Double> sortedHubs = sortHashMapByValue(hubs);
        HashMap<Integer, Double> sortedAuthorities = sortHashMapByValue(authorities);
        if (sortedHubs != null) {
            int i = 0;
            for (Map.Entry<Integer, Double> e : sortedHubs.entrySet()) {
                int linkId = e.getKey();
                if (linkIdToDocId.get(linkId) == null) {
                    // System.out.println("no linkid to doc id has been found");
                    i++;
                    continue;
                }
                int docId = linkIdToDocId.get(linkId);
                double hubScore = e.getValue();
                double authScore = sortedAuthorities.get(linkId);
                double score = 0d;
                if (hubScore > 0.1)
                    hubScore *= 2.0;
                if (authScore > 0.1)
                    authScore *= 2.0;
                score = hubScore + authScore;
                if (rootSet.contains(docId))
                    score += 0.5;
                result.addEntry(new PostingsEntry(docId, score));
            }
            System.out.println("found " + i + " non-match ids");

        }
        return result;
    }

    /**
     * Sort a hash map by values in the descending order
     *
     * @param map A hash map to sorted
     *
     * @return A hash map sorted by values
     */
    public HashMap<Integer, Double> sortHashMapByValue(HashMap<Integer, Double> map) {
        if (map == null) {
            return null;
        } else {
            List<Map.Entry<Integer, Double>> list = new ArrayList<Map.Entry<Integer, Double>>(map.entrySet());

            Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
                public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                    return (o2.getValue()).compareTo(o1.getValue());
                }
            });

            HashMap<Integer, Double> res = new LinkedHashMap<Integer, Double>();
            for (Map.Entry<Integer, Double> el : list) {
                res.put(el.getKey(), el.getValue());
            }
            return res;
        }
    }

    /**
     * Write the first `k` entries of a hash map `map` to the file `fname`.
     *
     * @param map   A hash map
     * @param fname The filename
     * @param k     A number of entries to write
     */
    void writeToFile(HashMap<Integer, Double> map, String fname, int k) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fname));

            if (map != null) {
                int i = 0;
                for (Map.Entry<Integer, Double> e : map.entrySet()) {
                    i++;
                    writer.write(e.getKey() + ": " + String.format("%.5g%n", e.getValue()));
                    if (i >= k)
                        break;
                }
            }
            writer.close();
        } catch (IOException e) {
        }
    }

}