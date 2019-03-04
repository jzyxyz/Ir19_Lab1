/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.*;

public class PostingsList {

    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();

    /** Number of postings in this list. */
    public int size() {
        return list.size();
    }

    public PostingsList() {
    }

    public ArrayList<PostingsEntry> getlist() {
        return list;
    }

    public void concat(PostingsList other) {
        ArrayList<PostingsEntry> otherList = other.getlist();
        list.addAll(otherList);
    }

    public void sortByScore() {
        Collections.sort(list, PostingsEntry.byScore);
    }

    public void sortByDocIdAndOffset() {
        Collections.sort(list, PostingsEntry.byDocIdAndOffset);
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

    public void addEntry(PostingsEntry entry) {
        list.add(entry);
    }

    public ListIterator<PostingsEntry> gIterator() {
        return list.listIterator();
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
