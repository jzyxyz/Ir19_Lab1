/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  


package ir;

import java.util.HashMap;
import java.util.Iterator;



/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {


    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    /**
     *  Inserts this token in the hashtable.
     */
    public void insert( String token, int docID, int offset ) {

        PostingsEntry newEntry = new PostingsEntry(docID, offset);
        if(!index.containsKey(token)){
            index.put(token, new PostingsList());
        }
        index.get(token).addEntry(newEntry);   
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        //
        // REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        if (!index.containsKey(token)) return null;
        else return index.get(token);
    }

    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
