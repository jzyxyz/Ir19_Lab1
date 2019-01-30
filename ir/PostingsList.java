/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.ListIterator;



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

    public ListIterator<PostingsEntry> gIterator() {
        return list.listIterator();
    }

    public boolean hasEntryWith(int _docID){
        boolean flag = false;
        if(list.size() == 0 ) return flag;

        ListIterator<PostingsEntry> it = gIterator();
        while(it.hasNext()){
            PostingsEntry e = it.next();
            if( e.docID == _docID) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    public boolean contains(PostingsEntry e){
        boolean flag = false;
        if(list.size() == 0 ) return flag;
        ListIterator<PostingsEntry> it = gIterator();
        while(it.hasNext()){
            if( e.docID == it.next().docID) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    public PostingsList intersectWith (PostingsList other){
        PostingsList result = new PostingsList();
        int i = 0;
        int j = 0;
        while( i < size() && j < other.size()){
            PostingsEntry e_self = get(i);
            PostingsEntry e_other = other.get(j);
            if(e_self.docID == e_other.docID && !result.hasEntryWith(e_self.docID)) {
                result.addEntry(e_self);
                i++;
                j++;
            } else if (e_self.docID < e_other.docID) i++;
                    else j++;
        }
        return result;
    }

    public PostingsList phraseWith(PostingsList other){

        PostingsList result = new PostingsList();
        int i = 0;
        int j = 0;
        while( i < size() && j < other.size()){
            PostingsEntry e_self = get(i);
            PostingsEntry e_other = other.get(j);
            if(e_self.docID == e_other.docID && !result.hasEntryWith(e_self.docID)) {
                int diff = e_other.offset - e_self.offset - 1;
                if( diff == 0) {
                    result.addEntry(e_other);
                    i++;
                    j++;
                    // making up phrase
                } else if( diff > 0 ) i++;
                        else j++;
            } else if (e_self.docID < e_other.docID) i++;
                    else j++;
        }

        return result;
    }
    
    public String format() {
        String result = "";
        if(size() == 0) return null;
        for( int i=0; i<size(); i++){
            result = result + list.get(i).format() + ";";
        }
        return result;
    }
}

