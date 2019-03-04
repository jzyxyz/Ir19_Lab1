package ir;

import java.lang.Math;
import java.util.*;

import ir.Index;

public class TFIDF {

    Index index;

    TFIDF(Index index) {
        this.index = index;
    }

    int tf(String term, int id) {
        if (index.getPostings(term) == null) {
            return 0;
        }
        return index.getPostings(term).numTermOccursIn(id);
    }

    double idf(String term) {
        if (index.getPostings(term) == null) {
            return 0.0;
        }
        int N = index.docLengths.size();
        int df = index.getPostings(term).getDocIdSet().size();
        return Math.log(N / df);
    }

    double idf(String term, int docId) {
        return idf(term) / Index.docLengths.get(docId);
    }

    double tf_idf(String term, int docId) {
        return tf(term, docId) * idf(term, docId);
    }

    public PostingsList rank(Set<Integer> result_set, HashMap<Integer, ArrayList<String>> converted) {
        int N = index.docLengths.size();
        PostingsList result = new PostingsList();
        Map<Integer, Double> scores = new HashMap<Integer, Double>();
        for (int id : result_set) {
            scores.put(id, 0d);
            for (Integer idx : converted.keySet()) {
                for (String opt : converted.get(idx)) {
                    // double weight = query.getTermWeightAt(i);
                    double weight = 1.0;
                    double tf_idf = tf_idf(opt, id) * weight;
                    scores.put(id, tf_idf + scores.get(id));
                }
            }
            int docLen = Index.docLengths.get(id);
            scores.put(id, scores.get(id) / docLen);
            result.addEntry(new PostingsEntry(id, scores.get(id)));
        }
        return result;
    }

}