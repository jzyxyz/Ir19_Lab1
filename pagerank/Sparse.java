import java.util.*;
import java.nio.*;
import java.io.*;
import java.lang.Math;

public class Sparse {

    /**
     * Maximal number of documents. We're assuming here that we don't have more docs
     * than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     * Mapping from document names to document numbers.
     */
    HashMap<String, Integer> docNumber = new HashMap<String, Integer>();

    /**
     * Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**
     * A memory-efficient representation of the transition matrix. The outlinks are
     * represented as a HashMap, whose keys are the numbers of the documents linked
     * from.
     * <p>
     *
     * The value corresponding to key i is a HashMap whose keys are all the numbers
     * of documents j that i links to.
     * <p>
     *
     * If there are no outlinks from i, then the value corresponding key i is null.
     */
    HashMap<Integer, HashMap<Integer, Boolean>> link = new HashMap<Integer, HashMap<Integer, Boolean>>();

    /**
     * The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     * The probability that the surfer will be bored, stop following links, and take
     * a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     * Convergence criterion: Transition probabilities do not change more that
     * EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

    /* --------------------------------------------- */

    public Sparse(String filename) {
        int noOfDocs = readDocs(filename);
        iterate(noOfDocs, 1000);
    }

    /* --------------------------------------------- */

    /**
     * Reads the documents and fills the data structures.
     *
     * @return the number of documents read.
     */
    int readDocs(String filename) {
        int fileIndex = 0;
        try {
            System.err.print("Reading file... ");
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = in.readLine()) != null && fileIndex < MAX_NUMBER_OF_DOCS) {
                int index = line.indexOf(";");
                String title = line.substring(0, index);
                Integer fromdoc = docNumber.get(title);
                // Have we seen this document before?
                if (fromdoc == null) {
                    // This is a previously unseen doc, so add it to the table.
                    fromdoc = fileIndex++;
                    docNumber.put(title, fromdoc);
                    docName[fromdoc] = title;
                }
                // Check all outlinks.
                StringTokenizer tok = new StringTokenizer(line.substring(index + 1), ",");
                while (tok.hasMoreTokens() && fileIndex < MAX_NUMBER_OF_DOCS) {
                    String otherTitle = tok.nextToken();
                    Integer otherDoc = docNumber.get(otherTitle);
                    if (otherDoc == null) {
                        // This is a previousy unseen doc, so add it to the table.
                        otherDoc = fileIndex++;
                        docNumber.put(otherTitle, otherDoc);
                        docName[otherDoc] = otherTitle;
                    }
                    // Set the probability to 0 for now, to indicate that there is
                    // a link from fromdoc to otherDoc.
                    if (link.get(fromdoc) == null) {
                        link.put(fromdoc, new HashMap<Integer, Boolean>());
                    }
                    if (link.get(fromdoc).get(otherDoc) == null) {
                        link.get(fromdoc).put(otherDoc, true);
                        out[fromdoc]++;
                    }
                }
            }
            if (fileIndex >= MAX_NUMBER_OF_DOCS) {
                System.err.print("stopped reading since documents table is full. ");
            } else {
                System.err.print("done. ");
            }
        } catch (FileNotFoundException e) {
            System.err.println("File " + filename + " not found!");
        } catch (IOException e) {
            System.err.println("Error reading file " + filename);
        }
        System.err.println("Read " + fileIndex + " number of documents");
        return fileIndex;
    }

    public static boolean hasConverged(double[] A, double[] B) {
        int dimension = A.length;
        double result = 0.0;
        for (int i = 0; i < dimension; i++) {
            double diff = A[i] - B[i];
            result += diff * diff;
        }
        // System.out.println("distance is " + result + ".....");
        return Math.sqrt(result) < EPSILON ? true : false;
    }

    public class PRResult implements Comparable<PRResult> {
        public final int index;
        public final String title;
        public final double score;

        public PRResult(int _index, String _title, double _score) {
            index = _index;
            title = _title;
            score = _score;
        }

        @Override
        public int compareTo(PRResult other) {
            return -1 * Double.valueOf(score).compareTo(other.score);
        }
    }

    /* --------------------------------------------- */

    /*
     * Chooses a probability vector a, and repeatedly computes aP, aP^2, aP^3...
     * until aP^i = aP^(i+1).
     */
    void iterate(int numberOfDocs, int maxIterations) {
        // Convert the link to a prob. sparse matrix(hash map)
        double[] a = new double[numberOfDocs];
        double[] a_ = new double[numberOfDocs];
        a_[0] = 1;
        int nI = 0;
        System.out.println("-------------------");
        while (!hasConverged(a, a_)) {
            long startTime = System.currentTimeMillis();

            a = a_.clone();
            System.out.println(nI + "\t:iteration");
            if (nI++ > maxIterations)
                break;
            for (int currState = 0; currState < numberOfDocs; currState++) {
                a_[currState] = 0;
                for (int otherState = 0; otherState < numberOfDocs; otherState++) {
                    int outSize = out[otherState];
                    // System.out.println(outSize + "\t:outsize");

                    double randomJumpScore = 1.0 / numberOfDocs;
                    double finalScore = 0.0;
                    if (outSize == 0) {
                        // if there is not a single link from otherState to else
                        finalScore = randomJumpScore;
                        // a_[currState] += a[currState] * randomJumpScore;
                    } else {
                        HashMap<Integer, Boolean> linkMap = link.get(otherState);
                        // System.out.println(linkMap.size() + "\t:link map size");
                        double linkScore = 1.0 / outSize;
                        double NOT_BORED = 1.0 - BORED;
                        if (linkMap.containsKey(currState)) {
                            // if there is a link from otherState to currState
                            finalScore = NOT_BORED * linkScore + BORED * randomJumpScore;
                            // a_[currState] += finalScore * a[currState];
                        } else {
                            // if there are links from otherState to the rest, but not to the current
                            finalScore = BORED * randomJumpScore;

                        }
                    }
                    a_[currState] += finalScore * a[otherState];
                }
            }
            long elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("This iteration takes: \t" + elapsedTime);

        }

        System.out.println("Number of Iterations: " + nI);
        final int firstN = 30;
        ArrayList<PRResult> resultList = new ArrayList<PRResult>();
        for (int i = 0; i < numberOfDocs; i++) {
            String title = docName[i];
            PRResult p = new PRResult(i, title, a_[i]);
            resultList.add(p);
        }
        FileWriter writer = null;
        try {
            writer = new FileWriter("output.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (PRResult result : resultList) {
            String score = String.format("%.5f", result.score);
            StringBuilder sb = new StringBuilder();
            sb.append(result.index).append(":").append(result.title).append(":").append(score).append("\n");
            try {
                writer.write(sb.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Collections.sort(resultList, (p1, p2) -> p1.compareTo(p2));
        for (int i = 0; i < firstN; i++) {
            PRResult result = resultList.get(i);
            String value = String.format("%.5f", result.score);
            System.out.println(result.title + ":" + value);

        }

    }

    /* --------------------------------------------- */

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Please give the name of the link file");
        } else {
            new Sparse(args[0]);
        }
    }
}