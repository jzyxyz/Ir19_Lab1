/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.util.*;
import java.util.stream.Collectors;
import java.lang.Math;

public class SpellChecker {
    /** The regular inverted index to be used by the spell checker */
    Index index;

    /** K-gram index to be used by the spell checker */
    KGramIndex kgIndex;

    /**
     * The auxiliary class for containing the value of your ranking function for a
     * token
     */
    class KGramStat implements Comparable {
        double score;
        String token;

        KGramStat(String token, double score) {
            this.token = token;
            this.score = score;
        }

        public String getToken() {
            return token;
        }

        public int compareTo(Object other) {
            if (this.score == ((KGramStat) other).score)
                return 0;
            return this.score < ((KGramStat) other).score ? -1 : 1;
        }

        public String toString() {
            return token + ";" + score;
        }
    }

    /**
     * The threshold for Jaccard coefficient; a candidate spelling correction should
     * pass the threshold in order to be accepted
     */
    private static final double JACCARD_THRESHOLD = 0.75;

    /**
     * The threshold for edit distance for a candidate spelling correction to be
     * accepted.
     */
    private static final int MAX_EDIT_DISTANCE = 4;

    public SpellChecker(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     * Computes the Jaccard coefficient for two sets A and B, where the size of set
     * A is <code>szA</code>, the size of set B is <code>szB</code> and the
     * intersection of the two sets contains <code>intersection</code> elements.
     */
    private double jaccard(int szA, Integer szB, int intersection) {
        return intersection * 1.0 / szA + szB - intersection;
    }

    private int Minimum(int a, int b, int c) {
        int mi;

        mi = a;
        if (b < mi) {
            mi = b;
        }
        if (c < mi) {
            mi = c;
        }
        return mi;

    }

    /**
     * Computing Levenshtein edit distance using dynamic programming. Allowed
     * operations are: => insert (cost 1) => delete (cost 1) => substitute (cost 2)
     */
    private int editDistance(String s, String t) {

        int d[][]; // matrix
        int n; // length of s
        int m; // length of t
        int i; // iterates through s
        int j; // iterates through t
        char s_i; // ith character of s
        char t_j; // jth character of t
        int cost; // cost

        n = s.length();
        m = t.length();
        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        d = new int[n + 1][m + 1];
        for (i = 0; i <= n; i++) {
            d[i][0] = i;
        }
        for (j = 0; j <= m; j++) {
            d[0][j] = j;
        }
        for (i = 1; i <= n; i++) {
            s_i = s.charAt(i - 1);
            for (j = 1; j <= m; j++) {
                t_j = t.charAt(j - 1);
                if (s_i == t_j) {
                    cost = 0;
                } else {
                    cost = 2;
                }
                d[i][j] = Minimum(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost);

            }
        }
        return d[n][m];

    }

    /**
     * Checks spelling of all terms in <code>query</code> and returns up to
     * <code>limit</code> ranked suggestions for spelling correction.
     */
    public String[] check(Query query, int limit) {

        String misspelled = query.getTermStringAt(0);
        int szQ = misspelled.length() + 3 - kgIndex.getK();
        // misspelled=kgIndex.extend(misspelled);
        ArrayList<String> q_kgrams = kgIndex.getKGram(misspelled);

        List<Integer> unioned = q_kgrams.stream().map(kg -> kgIndex.getPostings(kg)).flatMap(List::stream)
                .map(e -> e.tokenID).collect(Collectors.toList());

        Map<Integer, Long> _jc_count = unioned.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        Map<Integer, Integer> jc_count = _jc_count.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> Math.toIntExact(e.getValue())));

        Map<Integer, Double> jc_score = jc_count.entrySet().stream().collect(
                Collectors.toMap(e -> e.getKey(), e -> jaccard(szQ, kgIndex.id2kgnum.get(e.getKey()), e.getValue())));

        Map<String, Double> jc_passed = jc_score.entrySet().stream().filter(e -> e.getValue() > JACCARD_THRESHOLD)
                .collect(Collectors.toMap(e -> kgIndex.getTermByID(e.getKey()), e -> e.getValue()));

        Map<String, Integer> edit_dis = jc_passed.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> editDistance(misspelled, e.getKey())));
        Map<String, Integer> edit_passed = edit_dis.entrySet().stream().filter(e -> e.getValue() < MAX_EDIT_DISTANCE)
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        Set<String> set = edit_passed.keySet();

        // for (String s : set) {
        // System.out.println(s + " : " + String.format("%.5g", jc_passed.get(s)) + " :
        // " + edit_passed.get(s));
        // }

        Map<String, Double> combined = set.stream()
                .collect(Collectors.toMap(k -> k, k -> jc_passed.get(k) / edit_passed.get(k)));

        set = sortHashMapByValue(combined).keySet();
        String[] arr = set.toArray(new String[set.size()]);
        int l = limit > arr.length ? arr.length : limit;
        return Arrays.copyOfRange(arr, 0, l);
    }

    /**
     * Merging ranked candidate spelling corrections for all query terms available
     * in <code>qCorrections</code> into one final merging of query phrases. Returns
     * up to <code>limit</code> corrected phrases.
     */
    private List<KGramStat> mergeCorrections(List<List<KGramStat>> qCorrections, int limit) {
        //
        // YOUR CODE HERE
        //
        return null;
    }

    /**
     * Sort a hash map by values in the descending order
     *
     * @param map A hash map to sorted
     *
     * @return A hash map sorted by values
     */
    public HashMap<String, Double> sortHashMapByValue(Map<String, Double> map) {
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
}
