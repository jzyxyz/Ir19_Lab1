/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collections;
import java.lang.Math.*;

/**
 * Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;
    PageRank pageRank;
    final double lamda = 0.92;

    /** Constructor */
    public Searcher(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    public Searcher(Index index, KGramIndex kgIndex, PageRank pageRank) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.pageRank = pageRank;
    }

    /**
     * Searches the index for postings matching the query.
     * 
     * @return A postings list representing the result of the query.
     */
    public PostingsList search(Query query, QueryType queryType, RankingType rankingType) {
        //
        // REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        PostingsList result = new PostingsList();
        Set<Integer> result_set = new HashSet<Integer>();
        int n_terms = query.size();

        ArrayList<Integer> set_sizes = new ArrayList<Integer>();
        if (n_terms == 0)
            return result;

        // map the terms to a new PostingsList array
        ArrayList<PostingsList> pl_all = new ArrayList<PostingsList>();
        for (int i = 0; i < n_terms; i++) {
            PostingsList pl_i = index.getPostings(query.getTermStringAt(i));
            pl_all.add(pl_i);
            Set<Integer> pl_i_set = pl_i.getDocIdSet();
            result_set.addAll(pl_i_set);
            set_sizes.add(pl_i_set.size());
        }
        if (result_set.size() == 0) // term not indexed
            return result;

        switch (queryType) {
        case INTERSECTION_QUERY:
            result = pl_all.get(0);
            pl_all.remove(0);
            if (n_terms == 1)
                return result.intersectWith(result);

            for (PostingsList pl : pl_all) {
                if (pl != null)
                    result = result.intersectWith(pl);
            }

            break;

        case PHRASE_QUERY:
            result = pl_all.get(0);
            pl_all.remove(0);
            if (n_terms == 1)
                return result.intersectWith(result);

            for (PostingsList pl : pl_all) {
                if (pl != null)
                    result = result.phraseWith(pl);
            }

            break;
        case RANKED_QUERY:

            result = new PostingsList();

            switch (rankingType) {
            case PAGERANK:
                for (int id : result_set) {
                    double score = pageRank.getPRScore(id);
                    result.addEntry(new PostingsEntry(id, score));
                }
                break;
            default:
                int N = 17483;
                Map<Integer, Double> scores = new HashMap<Integer, Double>();
                for (int id : result_set) {
                    int docLen = Index.docLengths.get(id);
                    scores.put(id, 0d);
                    for (int i = 0; i < n_terms; i++) {
                        if (set_sizes.get(i) > 0) {
                            String term = query.getTermStringAt(i);
                            PostingsList pl_i = pl_all.get(i);
                            int tf = pl_i.numTermOccursIn(id);
                            int df = set_sizes.get(i);
                            double idf = Math.log(N / df);
                            // 9114 coop
                            if (id == 9114 || id == 9641) {
                                System.out.println("tf for" + id + ": " + term + "   " + tf);
                                System.out.println("df for" + id + ": " + term + "   " + idf);
                            }
                            double tf_idf = tf * idf;
                            scores.put(id, tf_idf + scores.get(id));
                        }
                    }
                    scores.put(id, scores.get(id) / docLen);
                    if (rankingType == RankingType.COMBINATION) {
                        double newScore = (1 - lamda) * scores.get(id) + lamda * pageRank.getPRScore(id);
                        scores.put(id, newScore);
                    }
                    result.addEntry(new PostingsEntry(id, scores.get(id)));
                }
                break;
            }
            Collections.sort(result.getlist(), (PostingsEntry e1, PostingsEntry e2) -> e1.compareTo(e2));

        }
        // Index.showDocInfo(result);
        return result;
    }
}