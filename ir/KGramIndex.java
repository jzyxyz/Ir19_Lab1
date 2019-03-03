/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import ir.Query.QueryTerm;

import java.nio.charset.StandardCharsets;

public class KGramIndex {

    /** Mapping from term ids to actual term strings */
    HashMap<Integer, String> id2term = new HashMap<Integer, String>();

    /** Mapping from term strings to term ids */
    HashMap<String, Integer> term2id = new HashMap<String, Integer>();

    /** Index from k-grams to list of term ids that contain the k-gram */
    HashMap<String, List<KGramPostingsEntry>> index = new HashMap<String, List<KGramPostingsEntry>>();

    /** The ID of the last processed term */
    int lastTermID = -1;

    /** Number of symbols to form a K-gram */
    int K = 3;

    public KGramIndex(int k) {
        K = k;
        if (k <= 0) {
            System.err.println("The K-gram index can't be constructed for a negative K value");
            System.exit(1);
        }
    }

    /** Generate the ID for an unknown term */
    private int generateTermID() {
        return ++lastTermID;
    }

    public int getK() {
        return K;
    }

    /**
     * Get intersection of two postings lists
     */
    private List<KGramPostingsEntry> intersect(List<KGramPostingsEntry> p1, List<KGramPostingsEntry> p2) {
        Set<Integer> resultSet = new HashSet<Integer>();
        List<KGramPostingsEntry> result = new ArrayList<KGramPostingsEntry>();
        int i = 0;
        int j = 0;
        while (i < p1.size() && j < p2.size()) {
            KGramPostingsEntry e_self = p1.get(i);
            KGramPostingsEntry e_other = p2.get(j);
            if (e_self.tokenID == e_other.tokenID && !resultSet.contains(e_self.tokenID)) {
                result.add(e_self);
                resultSet.add(e_self.tokenID);
                i++;
                j++;
            } else if (e_self.tokenID < e_other.tokenID)
                i++;
            else
                j++;
        }
        return result;
    }

    /** Inserts all k-grams from a token into the index. */
    public void insert(String token) {
        if (getIDByTerm(token) != null)
            // if this token is already indexed
            return;
        int newid = generateTermID();
        term2id.put(token, newid);
        id2term.put(newid, token);
        String extended = "^" + token + "$";
        int k = getK();
        for (int i = 0; i < extended.length() - k + 1; i++) {
            String kgram = extended.substring(i, i + k);
            if (index.get(kgram) == null) {
                index.put(kgram, new ArrayList<KGramPostingsEntry>());
            }
            List<KGramPostingsEntry> list = index.get(kgram);
            list.add(new KGramPostingsEntry(newid));
        }
    }

    /** Get postings for the given k-gram */
    public List<KGramPostingsEntry> getPostings(String kgram) {
        return index.get(kgram);
    }

    /** Get id of a term */
    public Integer getIDByTerm(String term) {
        return term2id.get(term);
    }

    /** Get a term by the given id */
    public String getTermByID(Integer id) {
        return id2term.get(id);
    }

    public void logInfo() {
        String[] test = { "^x" };
        for (String t : test) {
            List<KGramPostingsEntry> list = getPostings(t);
            StringBuilder sb = new StringBuilder("");
            Iterator<KGramPostingsEntry> it = list.iterator();
            while (it.hasNext()) {
                sb.append(getTermByID(it.next().tokenID)).append(": ");
            }
            System.out.println("size of kgram " + t + " is " + list.size());
            System.out.println(sb.toString());
        }

    }

    public ArrayList<String> parseToken(String _token) {
        ArrayList<String> parsed = new ArrayList<String>();
        // If it is not a wildcard
        if (_token.indexOf("*") == -1) {
            parsed.add(_token);
            return parsed;
        }
        List<List<KGramPostingsEntry>> pl_all = new ArrayList<List<KGramPostingsEntry>>();

        StringBuilder sb = new StringBuilder("");
        String extended = sb.append("^").append(_token).append("$").toString();
        Pattern p = Pattern.compile(extended.replace("*", ".*"));

        int k = getK();
        for (int i = 0; i < extended.length() - k + 1; i++) {
            String kgram = extended.substring(i, i + k);
            if (index.get(kgram) != null) {
                pl_all.add(index.get(kgram));
            }
        }

        List<KGramPostingsEntry> intersection = pl_all.get(0);

        for (int i = 0; i < pl_all.size() - 1; i++) {
            intersection = intersect(intersection, pl_all.get(i + 1));
        }

        for (KGramPostingsEntry e : intersection) {
            String term = getTermByID(e.tokenID);
            Matcher m = p.matcher(term);
            if (m.matches()) {
                System.out.println(term);
                parsed.add(term);
            }
        }
        return parsed;
    }

    public HashMap<Integer, ArrayList<String>> convertIntersectQuery(Query original) {
        HashMap<Integer, ArrayList<String>> converted = new HashMap<Integer, ArrayList<String>>();
        for (int n = 0; n < original.size(); n++) {
            converted.put(n, parseToken(original.getTermStringAt(n)));
        }
        return converted;
    }

}
