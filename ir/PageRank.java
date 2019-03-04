package ir;

import java.util.*;
import java.io.*;

public class PageRank {
    public HashMap<Integer, Double> prScoreMap = new HashMap<Integer, Double>();
    private String DIR = "./pagerank/";
    private String FILENAME = "dataPRScore.txt";

    PageRank() {
        String path = DIR + FILENAME;
        try {
            System.err.print("Reading the PR score file... ");
            BufferedReader in = new BufferedReader(new FileReader(path));
            String line;
            while ((line = in.readLine()) != null) {
                String[] lineArr = line.split(";");
                // System.out.println(lineArr.length);

                int idx = Integer.valueOf(lineArr[0]);
                double score = Double.valueOf(lineArr[1]);
                prScoreMap.put(idx, score);
            }
            in.close();
        } catch (FileNotFoundException e) {
            System.err.println("File " + FILENAME + " not found! in " + "DIR");
        } catch (IOException e) {
            System.err.println("Error reading file " + FILENAME);
        }
        System.err.println("Read the pangrank from files successfully ! ");
    }

    public double getPRScore(int docId) {
        return prScoreMap.get(docId);
    }

    public PostingsList rank(Set<Integer> result_set) {
        PostingsList result = new PostingsList();
        for (int id : result_set) {
            double score = getPRScore(id);
            result.addEntry(new PostingsEntry(id, score));
        }
        return result;
    }
}