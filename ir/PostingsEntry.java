/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.*;
import java.io.Serializable;

public class PostingsEntry implements Serializable {

    public int docID;
    public int offset;
    public double score = 0.0;

    public static Comparator byDocIdAndOffset = new Comparator() {
        public int compare(Object o1, Object o2) {
            Integer id1 = ((PostingsEntry) o1).docID;
            Integer id2 = ((PostingsEntry) o2).docID;

            int intComp = id1.compareTo(id2);
            if (intComp != 0) {
                return intComp;
            }
            Integer offset1 = ((PostingsEntry) o1).offset;
            Integer offset2 = ((PostingsEntry) o2).offset;

            return offset1.compareTo(offset2);
        }
    };

    public static Comparator<PostingsEntry> byScore = new Comparator<PostingsEntry>() {
        public int compare(PostingsEntry e1, PostingsEntry e2) {
            return Double.compare(e2.score, e1.score);
        }
    };
    // /**
    // * PostingsEntries are compared by their score (only relevant in ranked
    // * retrieval).
    // *
    // * The comparison is defined so that entries will be put in descending order.
    // */
    // public int compareTo(PostingsEntry other) {
    // return Double.compare(other.score, score);
    // }

    public PostingsEntry(int _docID, int _offset) {
        docID = _docID;
        offset = _offset;
    }

    public PostingsEntry(int _docID, double _score) {
        docID = _docID;
        offset = 0;
        score = _score;
    }

    public String format() {
        StringBuilder result = new StringBuilder("");
        result.append(docID);
        result.append(":");
        result.append(offset);
        result.append("\n");
        return result.toString();
    }

}
