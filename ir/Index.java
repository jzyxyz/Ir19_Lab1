/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * Defines some common data structures and methods that all types of index
 * should implement.
 */
public interface Index {

    /** Mapping from document identifiers to document names. */
    public HashMap<Integer, String> docNames = new HashMap<Integer, String>();

    /** Mapping from document identifier to document length. */
    public HashMap<Integer, Integer> docLengths = new HashMap<Integer, Integer>();

    /** Inserts a token into the index. */
    public void insert(String token, int docID, int offset);

    /** Returns the postings for a given term. */
    public PostingsList getPostings(String token);

    /** This method is called on exit. */
    public void cleanup();

    public HashMap<Integer, HashSet<String>> docTokens = new HashMap<Integer, HashSet<String>>();

    public static void showDocInfo(PostingsList pl) {
        ListIterator<PostingsEntry> it = pl.gIterator();
        System.out.println("*****************INFO*********************");
        int i = 0;
        while (it.hasNext() && i < 50) {
            i++;
            PostingsEntry e = it.next();
            System.out.println(docNames.get(e.docID));
        }
        System.out.println("---------Relevance Feedback done!-------------");
        // 3229
        i = 0;
        while (it.hasNext()) {
            PostingsEntry e = it.next();
            i++;
            if (e.docID == 3229)
                System.out.println(i);
        }
    }
}
