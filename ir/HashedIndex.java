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
        //
        // YOUR CODE HERE
        //
        PostingsEntry newEntry = new PostingsEntry(docID, offset);
        PostingsList newList = new PostingsList();
        if (index.containsKey(token)) {
            //if this token is already in the table, 
            //get the oldlist and add the current entry 
            PostingsList oldList = new PostingsList();
            oldList = getPostings(token);
            oldList.addEntry(newEntry);
            newList = oldList;
        } else {
            //Otherwise add a entry to the empty PostingsList
            newList.addEntry(newEntry);
        }
        index.put(token, newList);   
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
