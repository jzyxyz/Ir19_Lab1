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

    /** Constructor */
    public Searcher(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
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
        // PostingsList ranked_result = new PostingsList();
        Set<Integer> result_set = new HashSet<Integer>();
        int n_terms = query.size();

        ArrayList<Integer> set_sizes = new ArrayList<Integer>();
        if (n_terms == 0)
            return result;

        // map the terms to a new PostingsList array
        ArrayList<PostingsList> pl_all = new ArrayList<PostingsList>();
        for (int i = 0; i < n_terms; i++) {
            PostingsList pl_i = index.getPostings(query.getTermStringAt(i));
            if (pl_i != null) {
                pl_all.add(pl_i);
                Set<Integer> pl_i_set = pl_i.getDocIdSet();
                result_set.addAll(pl_i_set);
                set_sizes.add(pl_i_set.size());
            }
        }
        if (pl_all.size() == 0) // term not indexed
            return result;

        switch (queryType) {
        case INTERSECTION_QUERY:
            result = pl_all.get(0);
            pl_all.remove(0);
            if (n_terms == 1)
                return result.intersectWith(result);

            for (PostingsList pl : pl_all) {
                result = result.intersectWith(pl);
            }

            break;

        case PHRASE_QUERY:
            result = pl_all.get(0);
            pl_all.remove(0);
            if (n_terms == 1)
                return result.intersectWith(result);

            for (PostingsList pl : pl_all) {
                result = result.phraseWith(pl);
            }

            break;
        case RANKED_QUERY:

            result = new PostingsList();
            int N = 17483;
            Map<Integer, Double> scores = new HashMap<Integer, Double>();
            for (int id : result_set) {
                int docLen = Index.docLengths.get(id);
                // System.out.println(docLen);
                scores.put(id, 0d);
                for (int i = 0; i < n_terms; i++) {
                    String term = query.getTermStringAt(i);
                    PostingsList term_pl = pl_all.get(i);
                    int tf = term_pl.numTermOccursIn(id);
                    // int df = term_pl.getDocIdSet().size();
                    int df = set_sizes.get(i);
                    double idf = Math.log(N / df);
                    double tf_idf = tf * idf;
                    scores.put(id, tf_idf + scores.get(id));
                }
                scores.put(id, scores.get(id) / docLen);
                result.addEntry(new PostingsEntry(id, scores.get(id)));
            }
            Collections.sort(result.getlist(), (PostingsEntry e1, PostingsEntry e2) -> e1.compareTo(e2));
            break;

        }
        // Index.showDocInfo(result);
        return result;
    }
}