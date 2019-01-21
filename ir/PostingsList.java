/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;

import com.sun.xml.internal.bind.v2.runtime.reflect.ListIterator;

public class PostingsList {
    
    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();


    /** Number of postings in this list. */
    public int size() {
    return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) {
    return list.get( i );
    }

    // 
    //  YOUR CODE HERE
    //
    public void addEntry(PostingsEntry entry) {
        list.add(entry);
    }

    public ListIterator<PostingsEntry> getIterator() {
        return list.listIterator(0);
    }
}

