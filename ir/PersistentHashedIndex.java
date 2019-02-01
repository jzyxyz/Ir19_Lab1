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
    public static final String INDEXDIR = "./index_small";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The data file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    public static long NUM_COLLISIONS_ONCE = 0;
    public static long NUM_COLLISIONS_MUL = 0;
    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;

    public static final int ENTRYSIZE = 20;
    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String, PostingsList> index = new HashMap<String, PostingsList>();

    // ===================================================================

    public long extraHash(String str) {
        long hash = 5381;
        int M = 33;
        for (int i = 0; i < str.length(); i++) {
            hash = hash * M + str.charAt(i);
            hash = hash < 0 ? -hash : hash;
        }
        return hash;
    }

    /**
     * A helper class representing one entry in the dictionary hashtable.
     */
    public class Entry {

        long ptr;
        int increament;
        long hash;

        public Entry(long _ptr, int _inc, long _hash) {
            ptr = _ptr;
            increament = _inc;
            hash = _hash;
        }

        public byte[] format() {
            // formate the Entry to BtyeArr 8B+4B+STRING(MAX_24B)
            ByteBuffer buffer = ByteBuffer.allocate(ENTRYSIZE);
            buffer.putLong(ptr);
            buffer.putInt(increament);
            buffer.putLong(hash);
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
            if (dictionaryFile.readInt() != 0)
                NUM_COLLISIONS_ONCE++;
            while (dictionaryFile.readInt() != 0) {
                dictionaryFile.seek(pre++);
                NUM_COLLISIONS_MUL++;
                if (pre == TABLESIZE)
                    pre = 0;
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
    void writeEntry(Entry entry, long _finalHash) {

        long ptr = _finalHash * ENTRYSIZE;
        try {
            dictionaryFile.seek(ptr);
            dictionaryFile.write(entry.format());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Reads an entry from the dictionary file.
     *
     * @param _finalHash The place in the dictionary file where to start reading.
     */
    Entry readEntry(long _finalHash) {

        long dataptr = 0;
        int dataInc = 0;
        long hash = 0;
        long ptr = _finalHash * ENTRYSIZE;
        try {
            dictionaryFile.seek(ptr);
            dataptr = dictionaryFile.readLong();
            dictionaryFile.seek(ptr + 8);
            dataInc = dictionaryFile.readInt();
            dictionaryFile.seek(ptr + 12);
            hash = dictionaryFile.readLong();

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("dataptr " + dataptr + " incre " + dataInc + " hash " + hash);
        Entry result = new Entry(dataptr, dataInc, hash);
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
        // int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list
            index.forEach((term, pl) -> {
                int inc = writeData(pl.format(), free);

                Entry newEntry = new Entry(free, inc, extraHash(term));
                free += inc;

                writeEntry(newEntry, finalHash(term));
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println("One time collisions is " + NUM_COLLISIONS_ONCE + " .");
        System.err.println("Multi-time collisions is " + NUM_COLLISIONS_MUL + " .");
    }

    // ==================================================================

    /**
     * Returns the postings for a specific term, or null if the term is not in the
     * index.
     */
    public PostingsList getPostings(String token) {

        PostingsList result = new PostingsList();
        long pre = preHash(token);
        long extra = extraHash(token);
        int offset = 0;
        Entry dictEntry = readEntry(pre);
        if (dictEntry.increament == 0)
            return new PostingsList();

        while (dictEntry.hash != extra) {
            if (offset > 101) {
                System.out.println("have not found a match after trying 100 new entries");
                return new PostingsList();
            }
            dictEntry = readEntry(pre + ++offset);
        }
        String dataStr = readData(dictEntry.ptr, dictEntry.increament);
        String[] dataStrArr = dataStr.split(";");
        for (String s : dataStrArr) {
            if (s.length() > 0) {
                String[] arr = s.split(":");
                PostingsEntry newPE = new PostingsEntry(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]));
                result.addEntry(newPE);
            }
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
