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
    HITSRanker hitsRanker;
    final double lamda = 0.92;

    // /** Constructor */
    // public Searcher(Index index, KGramIndex kgIndex) {
    // this.index = index;
    // this.kgIndex = kgIndex;
    // }

    public Searcher(Index index, KGramIndex kgIndex, PageRank pageRank, HITSRanker hitsRanker) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.pageRank = pageRank;
        this.hitsRanker = hitsRanker;
    }

    /**
     * Searches the index for postings matching the query.
     * 
     * @return A postings list representing the result of the query.
     */
    public PostingsList search(Query query, QueryType queryType, RankingType rankingType) {
        PostingsList result = new PostingsList();
        Set<Integer> result_set = new HashSet<Integer>();
        int n_terms = query.size();

        if (n_terms == 0)
            return result;
        // ArrayList<Integer> set_sizes = new ArrayList<Integer>();
        // // map the terms to a new PostingsList array
        // for (int i = 0; i < n_terms; i++) {
        // PostingsList pl_i = index.getPostings(query.getTermStringAt(i));
        // pl_all.add(pl_i);
        // if (pl_i != null) {
        // Set<Integer> pl_i_set = pl_i.getDocIdSet();
        // result_set.addAll(pl_i_set);
        // set_sizes.add(pl_i_set.size());
        // } else {
        // set_sizes.add(0);
        // }
        // }
        // if (result_set.size() == 0) // term not indexed
        // return result;

        switch (queryType) {
        case INTERSECTION_QUERY:

            HashMap<Integer, ArrayList<String>> converted = kgIndex.convertIntersectQuery(query);
            HashMap<Integer, ArrayList<PostingsList>> pl_all = new HashMap<Integer, ArrayList<PostingsList>>();

            for (Integer idx : converted.keySet()) {
                ArrayList<String> options = converted.get(idx);
                ArrayList<PostingsList> pl_cur = new ArrayList<PostingsList>();
                for (String opt : options) {
                    // System.out.print(opt + ": ");
                    if (index.getPostings(opt) != null)
                        pl_cur.add(index.getPostings(opt));
                }
                System.out.println("---------------" + idx + "------" + options.size());

                if (pl_cur.size() > 0)
                    pl_all.put(idx, pl_cur);
            }

            System.out.println("query hash map size: " + converted.size());
            System.out.println("mapped pl hash map size: " + pl_all.size());

            for (Integer idx : pl_all.keySet()) {
                ArrayList<PostingsList> arr = pl_all.get(idx);
                Set<Integer> unionSet = new HashSet<Integer>();
                for (PostingsList pl : arr) {
                    unionSet.addAll(pl.getDocIdSet());
                }
                arr.clear();
                PostingsList unioned = new PostingsList(unionSet);
                arr.add(unioned);
                System.out.println("Unioned----------" + idx + "------" + unioned.size());
            }

            result = pl_all.get(0).get(0);
            result = result.intersectWith(result);
            pl_all.remove(0);
            for (Integer idx : pl_all.keySet()) {
                result = result.intersectWith(pl_all.get(idx).get(0));
            }

            break;

        case PHRASE_QUERY:
            // result = pl_all.get(0);
            // pl_all.remove(0);
            // if (n_terms == 1)
            // return result.intersectWith(result);

            // for (PostingsList pl : pl_all) {
            // if (pl != null)
            // result = result.phraseWith(pl);
            // }

            break;
        case RANKED_QUERY:

            // result = new PostingsList();

            // switch (rankingType) {
            // case HITS:
            // System.out.println("invoke HITS ranker");
            // result = hitsRanker.rank(result_set);
            // break;
            // case PAGERANK:
            // for (int id : result_set) {
            // double score = pageRank.getPRScore(id);
            // result.addEntry(new PostingsEntry(id, score));
            // }
            // break;
            // default:
            // int N = Index.docLengths.keySet().size();
            // Map<Integer, Double> scores = new HashMap<Integer, Double>();
            // for (int id : result_set) {
            // int docLen = Index.docLengths.get(id);
            // scores.put(id, 0d);
            // for (int i = 0; i < n_terms; i++) {
            // if (set_sizes.get(i) > 0) {
            // double weight = query.getTermWeightAt(i);
            // PostingsList pl_i = pl_all.get(i);
            // int tf = pl_i.numTermOccursIn(id);
            // int df = set_sizes.get(i);
            // double idf = Math.log(N / df);
            // double tf_idf = tf * idf * weight;
            // scores.put(id, tf_idf + scores.get(id));
            // }
            // }
            // scores.put(id, scores.get(id) / docLen);
            // // If it's combination
            // if (rankingType == RankingType.COMBINATION) {
            // double newScore = (1 - lamda) * scores.get(id) + lamda *
            // pageRank.getPRScore(id);
            // scores.put(id, newScore);
            // }
            // result.addEntry(new PostingsEntry(id, scores.get(id)));
            // }
            break;
        // }
        // Collections.sort(result.getlist(), (PostingsEntry e1, PostingsEntry e2) ->
        // e1.compareTo(e2));

        }
        // Index.showDocInfo(result);
        // kgIndex.logInfo();
        return result;
    }
}