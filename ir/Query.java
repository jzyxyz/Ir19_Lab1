/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.*;
import java.nio.charset.*;
import java.io.*;

/**
 * A class for representing a query as a list of words, each of which has an
 * associated weight.
 */
public class Query {

    /**
     * Help class to represent one query term, with its associated weight.
     */
    class QueryTerm {
        String term;
        double weight;

        QueryTerm(String t, double w) {
            term = t;
            weight = w;
        }
    }

    /**
     * Representation of the query as a list of terms with associated weights. In
     * assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();

    /**
     * Relevance feedback constant alpha (= weight of original query terms). Should
     * be between 0 and 1. (only used in assignment 3).
     */
    double alpha = 0.15;

    /**
     * Relevance feedback constant beta (= weight of query terms obtained by
     * feedback from the user). (only used in assignment 3).
     */
    double beta = 1.0 - alpha;

    /**
     * Creates a new empty Query
     */
    public Query() {
    }

    /**
     * Creates a new Query from a string of words
     */
    public Query(String queryString) {
        StringTokenizer tok = new StringTokenizer(queryString);
        while (tok.hasMoreTokens()) {
            queryterm.add(new QueryTerm(tok.nextToken(), 1.0));
        }
    }

    /**
     * Returns the number of terms
     */
    public int size() {
        return queryterm.size();
    }

    /**
     * Returns the Manhattan query length
     */
    public double length() {
        double len = 0;
        for (QueryTerm t : queryterm) {
            len += t.weight;
        }
        return len;
    }

    /**
     * Returns a copy of the Query
     */
    public Query copy() {
        Query queryCopy = new Query();
        for (QueryTerm t : queryterm) {
            queryCopy.queryterm.add(new QueryTerm(t.term, t.weight));
        }
        return queryCopy;
    }

    public String getTermStringAt(int pos) {
        return queryterm.get(pos).term;
    }

    public double getTermWeightAt(int pos) {
        return queryterm.get(pos).weight;
    }

    public void logInfo() {
        StringBuilder sb = new StringBuilder("");
        for (QueryTerm qt : queryterm) {
            sb.append(qt.term).append(": ").append(qt.weight).append("\n");
        }
        System.out.println(sb.toString());
    }

    /**
     * Sort a hash map by values in the descending order
     *
     * @param map A hash map to sorted
     *
     * @return A hash map sorted by values
     */
    public HashMap<String, Double> sortHashMapByValue(HashMap<String, Double> map) {
        if (map == null) {
            return null;
        } else {
            List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(map.entrySet());

            Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
                public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                    return (o2.getValue()).compareTo(o1.getValue());
                }
            });

            HashMap<String, Double> res = new LinkedHashMap<String, Double>();
            for (Map.Entry<String, Double> el : list) {
                res.put(el.getKey(), el.getValue());
            }
            return res;
        }
    }

    /**
     * Expands the Query using Relevance Feedback
     *
     * @param results       The results of the previous query.
     * @param docIsRelevant A boolean array representing which query results the
     *                      user deemed relevant.
     * @param engine        The search engine object
     */
    public void relevanceFeedback(PostingsList results, boolean[] docIsRelevant, Engine engine) {

        System.out.println("relevance feedback starts");

        HashMap<String, Double> weightMap = new HashMap<String, Double>();
        for (QueryTerm qt : queryterm) {
            double idf = engine.tfidf.idf(qt.term);
            if (idf > 0.0) {
                weightMap.put(qt.term, idf);
            }
        }

        // normalize and alpha the original query
        double len = length();
        for (String term : weightMap.keySet()) {
            weightMap.put(term, alpha * weightMap.get(term) / len);

        }

        for (int i = 0; i < docIsRelevant.length; i++) {
            if (docIsRelevant[i] == true) {
                int labeled = results.get(i).docID;
                // System.out.println("labeled id:" + labeled);
                HashSet<String> docTokenSet = Index.docTokens.get(labeled);
                for (String token : docTokenSet) {
                    double tf_idf = engine.tfidf.tf_idf(token, labeled);
                    double newWeight;
                    if (weightMap.get(token) == null) {
                        newWeight = tf_idf * beta;
                        // System.out.println("add `" + token + "` with " + newWeight);
                    } else {
                        newWeight = weightMap.get(token) + beta * tf_idf;
                        // System.out.println("update `" + token + "` to " + newWeight);
                    }
                    weightMap.put(token, newWeight);
                }
            }
        }

        // HashMap<String, Double> sorted = sortHashMapByValue(weightMap);
        // int size = sorted.size();
        // int i = 0;
        // queryterm.clear();
        // for (String term : sorted.keySet()) {
        // queryterm.add(new QueryTerm(term, sorted.get(term)));
        // if (i++ > Math.floor(size / 3))
        // break;
        // }
        len = length();
        // System.out.println(len);

        for (String term : weightMap.keySet()) {
            weightMap.put(term, weightMap.get(term) / len);
        }

        queryterm.clear();
        double threshold = 0.010;

        for (String term : weightMap.keySet()) {
            if (weightMap.get(term) > threshold) {
                queryterm.add(new QueryTerm(term, weightMap.get(term)));
            }
        }

        System.out.println("new query size " + queryterm.size());
        // logInfo();
    }
}
