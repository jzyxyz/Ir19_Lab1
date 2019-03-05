/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.util.*;
import java.util.stream.*;
import java.lang.Math;
import java.util.AbstractMap.SimpleEntry;

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
            return this.score < ((KGramStat) other).score ? 1 : -1;
        }

        public String toString() {
            return token + " : " + String.format("%.5g", score);
        }
    }

    /**
     * The threshold for Jaccard coefficient; a candidate spelling correction should
     * pass the threshold in order to be accepted
     */
    private static final double JACCARD_THRESHOLD = 0.6;

    /**
     * The threshold for edit distance for a candidate spelling correction to be
     * accepted.
     */
    private static final int MAX_EDIT_DISTANCE = 5;

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

    private Map.Entry<String, List<KGramStat>> correct(Map.Entry<String, Integer> e) {
        List<KGramStat> list = new ArrayList<KGramStat>();
        String token = e.getKey();
        if (e.getValue() == 0) {
            // the 6 most likely, hard coded here
            list = findAltsFor(token, 6);
        } else {
            list.add(new KGramStat(token, 1.0));
        }
        Map.Entry<String, List<KGramStat>> mapped = new AbstractMap.SimpleEntry<String, List<KGramStat>>(token, list);
        return mapped;
    }

    public List<KGramStat> findAltsFor(String misspelled, int firstK) {
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

        Set<String> keys = edit_passed.keySet();

        Map<String, Double> combined = keys.stream()
                .collect(Collectors.toMap(k -> k, k -> jc_passed.get(k) / edit_passed.get(k)));

        List<KGramStat> list = sortHashMapByValue(combined).entrySet().stream()
                .map(e -> new KGramStat(e.getKey(), e.getValue())).collect(Collectors.toList());

        firstK = Math.min(firstK, list.size());

        List<KGramStat> cut = list.stream().limit(firstK).collect(Collectors.toList());
        return cut;
    }

    /**
     * Checks spelling of all terms in <code>query</code> and returns up to
     * <code>limit</code> ranked suggestions for spelling correction.
     */
    public String[] check(Query query, int limit) {

        Map<String, Integer> hits = query.queryterm.stream().collect(Collectors.toMap(qt -> qt.term,
                qt -> index.getPostings(qt.term) == null ? 0 : index.getPostings(qt.term).size()));

        Map<String, List<KGramStat>> qCorrections = hits.entrySet().stream().map(e -> correct(e))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        // qCorrections.values().forEach(l -> l.forEach(s ->
        // System.out.println(s.toString())));

        List<KGramStat> merge = mergeCorrections(qCorrections);
        merge.stream().map(kgs -> kgs.toString()).forEach(System.out::println);

        List<String> list = merge.stream().map(kgs -> kgs.token).collect(Collectors.toList());

        String[] arr = list.toArray(new String[list.size()]);
        int l = limit > arr.length ? arr.length : limit;
        return Arrays.copyOfRange(arr, 0, l);
    }

    /**
     * Merging ranked candidate spelling corrections for all query terms available
     * in <code>qCorrections</code> into one final merging of query phrases. Returns
     * up to <code>limit</code> corrected phrases.
     */
    private List<KGramStat> mergeCorrections(Map<String, List<KGramStat>> qCorrections) {
        List<List<KGramStat>> flat = qCorrections.values().stream().collect(Collectors.toList());
        Optional<List<KGramStat>> merge = flat.stream().reduce((accu, cur) -> mergeList(accu, cur));
        List<KGramStat> list = new ArrayList<KGramStat>();
        if (merge.isPresent()) {
            list.addAll(merge.get());
        }
        return list;
    }

    private List<KGramStat> mergeList(List<KGramStat> l1, List<KGramStat> l2) {
        List<KGramStat> merge = l1.stream().map(kgs -> mergeStat(kgs, l2)).flatMap(l -> l.stream())
                .collect(Collectors.toList());
        Collections.sort(merge);
        int use = Math.min(merge.size(), 4);
        return merge.stream().limit(use).collect(Collectors.toList());
    }

    private KGramStat mergeStat(KGramStat s1, KGramStat s2, PostingsList last) {
        String concat = s1.token + " " + s2.token;
        double score;
        if (last != null) {
            PostingsList pl = last.intersectWith(index.getPostings(s2.token));
            int hits = pl == null ? 1 : pl.size();
            score = (s1.score + s2.score) * Math.log(hits);
        } else {
            score = s1.score + s2.score;
        }
        return new KGramStat(concat, score);
    }

    private List<KGramStat> mergeStat(KGramStat kgs, List<KGramStat> l) {
        String[] arr = kgs.token.trim().split("\\s+");
        List<PostingsList> plist = Stream.of(arr).map(s -> index.getPostings(s)).collect(Collectors.toList());
        PostingsList result = plist.get(0);

        if (arr.length > 1) {
            plist.remove(0);
            for (PostingsList pl : plist) {
                result = result.intersectWith(pl);
            }
        }
        PostingsList last = result;
        // System.out.println("the size of last pl " + last.size());
        return l.stream().map(_kgs -> mergeStat(kgs, _kgs, last)).collect(Collectors.toList());
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
