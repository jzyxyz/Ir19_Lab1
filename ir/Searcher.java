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


import ir.Query.QueryTerm;


/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;


    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;
    
    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex ) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType ) { 
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        PostingsList result = new PostingsList();
        int n_terms = query.size();
        if(n_terms == 0) return new PostingsList();
        System.out.println("number of terms: "+n_terms);
        PostingsList l_0 = index.getPostings(query.getTermStringAt(0));
        ListIterator<PostingsEntry> it_0 = l_0.gIterator();   

        switch(queryType){
            case INTERSECTION_QUERY:
                ArrayList<PostingsList> l_all = new ArrayList<PostingsList>();
                for(int i=0; i<n_terms; i++){
                    PostingsList l_i = index.getPostings(query.getTermStringAt(i));
                    l_all.add(l_i);
                }
                result = l_all.get(0);
                l_all.remove(0);
                for( PostingsList pl : l_all) {
                    result = result.intersectWith(pl);
                }
                break;

            case PHRASE_QUERY:
                System.out.println("phrasing");
                for (int i=1; i < n_terms; i++){
                    PostingsList l_i = index.getPostings(query.getTermStringAt(i));
                    ListIterator<PostingsEntry> it_i = l_i.gIterator();
                    PostingsList l_tmp = new PostingsList();
                    while( it_0.hasNext() && it_i.hasNext()){
                        PostingsEntry en_0 = it_0.next();
                        PostingsEntry en_i = it_i.next();
                        if( en_0.docID == en_i.docID){
                            if(en_0.offset + 1 == en_i.offset) l_tmp.addEntry(en_0);
                            else  it_0.previous();
                        } else if(en_0.docID > en_i.docID ) it_0.previous();
                            else it_i.previous();
                    }
                    l_0 = l_tmp;
                }
                result = l_0;
                break;
            case RANKED_QUERY:
                System.out.println("ranking");
                break;
                    
        }
        return result;
    }
}