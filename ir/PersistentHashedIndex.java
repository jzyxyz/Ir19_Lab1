/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */

package ir;

import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;

/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The data file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;

    public static final int ENTRYSIZE = 40;
    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String, PostingsList> index = new HashMap<String, PostingsList>();

    // ===================================================================

    /**
     * A helper class representing one entry in the dictionary hashtable.
     */
    public class Entry {

        long ptr;
        int increament;
        String term;

        public Entry(long _ptr, int _increament, String _s) {
            ptr = _ptr;
            increament = _increament;
            term = _s;
        }

        public byte[] format() {
            // formate the Entry to BtyeArr 8B+4B+STRING(MAX_24B)
            ByteBuffer buffer = ByteBuffer.allocate(ENTRYSIZE);
            buffer.putLong(ptr);
            buffer.putInt(increament);
            if (term.getBytes().length > 24) {
                buffer.putInt(24);
                buffer.put(term.substring(term.length() - 7, term.length() - 2).getBytes());
            } else {
                buffer.putInt(term.getBytes().length);
                buffer.put(term.getBytes());
            }
            return buffer.array();
        }

    }

    // ==================================================================
    public long preHash(String str) {
        long hash = 5381;
        int M = 33;
        for (int i = 0; i < str.length(); i++) {
            hash = hash * M + str.charAt(i);
            hash = hash < 0 ? -hash : hash;
        }
        hash = hash % TABLESIZE;
        return hash;
    }

    public long finalHash(String str) {
        long pre = preHash(str);
        try {
            dictionaryFile.seek(pre);
            while (dictionaryFile.readInt() != 0) {
                pre++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pre;
    }

    /**
     * Constructor. Opens the dictionary file and the data file. If these files
     * don't exist, they will be created.
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME, "rw");
            dictionaryFile.seek((TABLESIZE + 1) * ENTRYSIZE);// write an EOF tag
            dictionaryFile.writeInt(999);
            dictionaryFile.seek(1000l);
            System.out.println("------------------------");
            System.out.println(dictionaryFile.readInt());

            dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME, "rw");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes data to the data file at a specified place.
     *
     * @return The number of bytes written.
     */
    int writeData(String dataString, long ptr) {
        try {
            dataFile.seek(ptr);
            byte[] data = dataString.getBytes();
            dataFile.write(data);
            return data.length;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Reads data from the data file
     */
    String readData(long ptr, int size) {
        try {
            dataFile.seek(ptr);
            byte[] data = new byte[size];
            dataFile.readFully(data);
            return new String(data);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ==================================================================
    //
    // Reading and writing to the dictionary file.

    /*
     * Writes an entry to the dictionary hash table file.
     *
     * @param entry The key of this entry is assumed to have a fixed length
     * 
     * @param ptr The place in the dictionary file to store the entry
     */
    void writeEntry(Entry entry, long ptr) {

        try {
            dictionaryFile.seek(ptr * ENTRYSIZE);
            dictionaryFile.write(entry.format());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Reads an entry from the dictionary file.
     *
     * @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry(long ptr) {

        long dataptr;
        int dataInc;
        int termLen = 0;
        String term;
        byte[] dictItem = new byte[ENTRYSIZE];
        try {
            dictionaryFile.seek(ptr * ENTRYSIZE);
            dictionaryFile.readFully(dictItem);

        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteBuffer buf_8 = ByteBuffer.allocate(8);
        ByteBuffer buf_4 = ByteBuffer.allocate(4);
        ByteBuffer buf_4alt = ByteBuffer.allocate(4);
        System.out.println(1);
        buf_8.put(Arrays.copyOfRange(dictItem, 0, 8));
        buf_8.flip();
        dataptr = buf_8.getLong();
        System.out.println(2);
        buf_4.put(Arrays.copyOfRange(dictItem, 8, 12));
        buf_4.flip();
        dataInc = buf_4.getInt();
        System.out.println(3);
        buf_4alt.put(Arrays.copyOfRange(dictItem, 12, 16));
        buf_4alt.flip();
        termLen = buf_4alt.getInt();
        System.out.println(4);
        ByteBuffer buf_s = ByteBuffer.allocate(termLen);
        buf_s.put(Arrays.copyOfRange(dictItem, 16, 16 + termLen));
        buf_s.flip();
        term = new String(buf_s.array());
        System.out.println("dataptr " + dataptr + " incre " + dataInc + " term " + term);
        Entry result = new Entry(dataptr, dataInc, term);
        return result;
    }

    // ==================================================================

    /**
     * Writes the document names and document lengths to file.
     *
     * @throws IOException { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream(INDEXDIR + "/docInfo");
        for (Map.Entry<Integer, String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
    }

    /**
     * Reads the document names and document lengths from file, and put them in the
     * appropriate data structures.
     *
     * @throws IOException { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File(INDEXDIR + "/docInfo");
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put(new Integer(data[0]), data[1]);
                docLengths.put(new Integer(data[0]), new Integer(data[2]));
            }
        }
        freader.close();
    }

    /**
     * Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list
            index.forEach((term, pl) -> {
                int inc = writeData(pl.format(), free);

                Entry newEntry = new Entry(free, inc, term);
                free += inc;

                writeEntry(newEntry, finalHash(term));
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println(collisions + " collisions.");
    }

    // ==================================================================

    /**
     * Returns the postings for a specific term, or null if the term is not in the
     * index.
     */
    public PostingsList getPostings(String token) {

        PostingsList result = new PostingsList();
        long hash = preHash(token);
        int offset = 0;
        Entry dictEntry = readEntry(hash);
        if (dictEntry.increament == 0)
            return new PostingsList();
        while (!token.equals(dictEntry.term)) {
            dictEntry = readEntry(hash + ++offset);
        }
        String dataStr = readData(dictEntry.ptr, dictEntry.increament);
        String[] dataStrArr = dataStr.split(";");
        for (String s : dataStrArr) {
            String[] arr = s.split(":");
            PostingsEntry newPE = new PostingsEntry(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]));
            result.addEntry(newPE);
        }

        return result;
    }

    /**
     * Inserts this token in the main-memory hashtable.
     */
    public void insert(String token, int docID, int offset) {
        PostingsEntry newEntry = new PostingsEntry(docID, offset);
        if (!index.containsKey(token)) {
            index.put(token, new PostingsList());
        }
        index.get(token).addEntry(newEntry);
    }

    /**
     * Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println(index.keySet().size() + " unique words");
        System.err.print("Writing index to disk...");
        writeIndex();
        System.err.println("done!");
    }
}
