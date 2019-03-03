package ir;

import java.lang.Math;
import java.util.HashSet;

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
        int N = Index.docNames.keySet().size();
        if (index.getPostings(term) == null) {
            return 0.0;
        }
        int df = index.getPostings(term).getDocIdSet().size();
        return Math.log(N / df);
    }

    double idf(String term, int docId) {
        return idf(term) / Index.docLengths.get(docId);
    }

    double tf_idf(String term, int docId) {
        return tf(term, docId) * idf(term, docId);
    }

}