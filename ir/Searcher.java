/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.*;
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
    TFIDF tfidf;
    final double lamda = 0.92;

    public Searcher(Index index, KGramIndex kgIndex, PageRank pageRank, HITSRanker hitsRanker, TFIDF tfidf) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.pageRank = pageRank;
        this.hitsRanker = hitsRanker;
        this.tfidf = tfidf;
    }

    public HashMap<Integer, ArrayList<PostingsList>> mapToPostingsList(HashMap<Integer, ArrayList<String>> converted) {
        HashMap<Integer, ArrayList<PostingsList>> pl_all = new HashMap<Integer, ArrayList<PostingsList>>();
        for (Integer idx : converted.keySet()) {
            ArrayList<String> options = converted.get(idx);
            ArrayList<PostingsList> pl_cur = new ArrayList<PostingsList>();
            for (String opt : options) {
                // System.out.print(opt + ": ");
                if (index.getPostings(opt) != null)
                    pl_cur.add(index.getPostings(opt));
            }
            System.out.println("for token at " + idx + " found " + options.size() + " options");
            if (pl_cur.size() > 0)
                pl_all.put(idx, pl_cur);
        }
        return pl_all;
    }

    public void reduceToUnion(HashMap<Integer, ArrayList<PostingsList>> pl_all, boolean nodup) {
        for (Integer idx : pl_all.keySet()) {
            ArrayList<PostingsList> arr = pl_all.get(idx);
            PostingsList unioned = new PostingsList();
            for (PostingsList pl : arr) {
                unioned.concat(pl);
            }
            unioned.sortByDocIdAndOffset();
            if (nodup == true) {
                unioned = unioned.intersectWith(unioned); // remove duplicate docIDs
            }
            arr.clear();
            arr.add(unioned);
            System.out.println("Unioned PostingsList for " + idx + " : " + unioned.size());
        }
    }

    public Set<Integer> flatMapToSet(HashMap<Integer, ArrayList<PostingsList>> pl_all) {
        Set<Integer> result_set = new HashSet<Integer>();
        for (Integer idx : pl_all.keySet()) {
            Set<Integer> pl_i_set = pl_all.get(idx).get(0).getDocIdSet();
            result_set.addAll(pl_i_set);
        }
        System.out.println("total result set size: " + result_set.size());
        return result_set;
    }

    /**
     * Searches the index for postings matching the query.
     * 
     * @return A postings list representing the result of the query.
     */
    public PostingsList search(Query query, QueryType queryType, RankingType rankingType) {
        PostingsList result = new PostingsList();
        int n_terms = query.size();

        if (n_terms == 0)
            return null;

        HashMap<Integer, ArrayList<String>> converted = kgIndex.toLinkedQuery(query);
        HashMap<Integer, ArrayList<PostingsList>> pl_all = mapToPostingsList(converted);
        System.out.println("query hash map size: " + converted.size());
        System.out.println("mapped pl hash map size: " + pl_all.size());

        if (pl_all.size() < 1)
            return null;

        switch (queryType) {
        case INTERSECTION_QUERY:

            reduceToUnion(pl_all, true);
            result = pl_all.get(0).get(0);
            pl_all.remove(0);
            for (Integer idx : pl_all.keySet()) {
                result = result.intersectWith(pl_all.get(idx).get(0));
            }
            break;

        case PHRASE_QUERY:

            reduceToUnion(pl_all, false);
            result = pl_all.get(0).get(0);
            if (n_terms == 1) {
                result = result.intersectWith(result);
            } else {
                pl_all.remove(0);
                for (Integer idx : pl_all.keySet()) {
                    result = result.phraseWith(pl_all.get(idx).get(0));
                }
            }
            break;

        case RANKED_QUERY:
            reduceToUnion(pl_all, true);
            Set<Integer> result_set = flatMapToSet(pl_all);
            switch (rankingType) {
            case HITS:
                System.out.println("invoke HITS ranker");
                result = hitsRanker.rank(result_set);
                break;

            case PAGERANK:
                result = pageRank.rank(result_set);
                break;

            case TF_IDF:
                result = tfidf.rank(result_set, converted);
                break;

            default:
                PostingsList tf = tfidf.rank(result_set, converted);
                PostingsList pr = pageRank.rank(result_set);
                for (int i = 0; i < tf.size(); i++) {
                    double score = (1 - lamda) * tf.get(i).score + lamda * pr.get(i).score;
                    result.addEntry(new PostingsEntry(tf.get(i).docID, score));
                }
            }
            result.sortByScore();
        }
        // Index.showDocInfo(result);
        // kgIndex.logInfo();
        return result;
    }
}