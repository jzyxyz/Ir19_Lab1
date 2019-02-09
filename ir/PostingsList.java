/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.ListIterator;

public class PostingsList {

    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();

    /** Number of postings in this list. */
    public int size() {
        return list.size();
    }

    public ArrayList<PostingsEntry> getlist() {
        return list;
    }

    public int numTermOccursIn(int _docID) {
        int result = 0;
        ListIterator<PostingsEntry> it = gIterator();
        while (it.hasNext()) {
            if (it.next().docID == _docID)
                result++;
        }
        return result;
    }

    public Set<Integer> getDocIdSet() {
        Set<Integer> resultSet = new HashSet<Integer>();
        ListIterator<PostingsEntry> it = gIterator();
        while (it.hasNext()) {
            resultSet.add(it.next().docID);
        }
        return resultSet;
    }

    /** Returns the ith posting. */
    public PostingsEntry get(int i) {
        return list.get(i);
    }

    //
    // YOUR CODE HERE
    //
    public void addEntry(PostingsEntry entry) {
        list.add(entry);
    }

    public ListIterator<PostingsEntry> gIterator() {
        return list.listIterator();
    }

    public boolean hasEntryWith(int _docID) {
        boolean flag = false;
        if (list.size() == 0)
            return flag;

        ListIterator<PostingsEntry> it = gIterator();
        while (it.hasNext()) {
            PostingsEntry e = it.next();
            if (e.docID == _docID) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    public boolean contains(PostingsEntry e) {
        boolean flag = false;
        if (list.size() == 0)
            return flag;
        ListIterator<PostingsEntry> it = gIterator();
        while (it.hasNext()) {
            if (e.docID == it.next().docID) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    public PostingsList intersectWith(PostingsList other) {
        Set<Integer> resultSet = new HashSet<Integer>();
        PostingsList result = new PostingsList();
        int i = 0;
        int j = 0;
        while (i < size() && j < other.size()) {
            PostingsEntry e_self = get(i);
            PostingsEntry e_other = other.get(j);
            if (e_self.docID == e_other.docID && !resultSet.contains(e_self.docID)) {
                result.addEntry(e_self);
                resultSet.add(e_self.docID);
                i++;
                j++;
            } else if (e_self.docID < e_other.docID)
                i++;
            else
                j++;
        }
        return result;
    }

    public PostingsList phraseWith(PostingsList other) {
        Set<Integer> resultSet = new HashSet<Integer>();
        PostingsList result = new PostingsList();
        int i = 0;
        int j = 0;
        while (i < size() && j < other.size()) {
            PostingsEntry e_self = get(i);
            PostingsEntry e_other = other.get(j);
            if (e_self.docID == e_other.docID && !resultSet.contains(e_self.docID)) {
                int diff = e_other.offset - e_self.offset - 1;
                if (diff == 0) {
                    resultSet.add(e_self.docID);
                    result.addEntry(e_other);
                    i++;
                    j++;
                    // making up phrase
                } else if (diff > 0)
                    i++;
                else
                    j++;
            } else if (e_self.docID < e_other.docID)
                i++;
            else
                j++;
        }

        return result;
    }

    public String format() {
        StringBuilder result = new StringBuilder("");
        for (int i = 0; i < size(); i++) {
            result.append(list.get(i).format());
        }
        return result.toString();
    }

}
