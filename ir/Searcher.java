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

import com.sun.xml.internal.bind.v2.runtime.reflect.ListIterator;

import ir.Query.QueryTerm;
import sun.jvm.hotspot.runtime.posix.POSIXSignals;

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

        String s = "";
        switch(QueryType) {
            case INTERSECTION_QUERY:
                PostingsList result = new PostingsList();
                ArrayList<PostingsList> postingsListArr = new ArrayList<PostingsList>(); 
                for (QueryTerm t : termArr){
                    postingsListArr.add(index.getPostings(t.term));
                }
                ListIterator<PostingsEntry> itr = postingsListArr.get(0).gIterator();
                while(itr.hasNext()){
                    PostingsEntry curEntry = itr.next();
                    for (int i=1; i<postingsListArr.size(); i++) {
                        ListIterator<PostingsEntry> itr_i = postingsListArr.get(i).gIterator();
                        PostingsEntry cmpEntry = itr_i.next();
                        if(curEntry.docID == cmpEntry.docID){
                            result.addEntry(new PostingsEntry(curEntry.docID, curEntry.offset));
                        }
                    }    
                }
                break;
            default: 
                for(QueryTerm t : query.queryterm) {
                    s = s + " " + t.term;
                    System.out.println("the term to be indexed is " + s);
                    return index.getPostings(s);
                }   
        }    
    }
}