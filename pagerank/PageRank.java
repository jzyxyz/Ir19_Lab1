import java.util.*;
import java.io.*;
import java.lang.Math;

public class PageRank {

    public class Pair implements Comparable<Pair> {
        public final int index;
        public final double value;

        public Pair(int i, double v) {
            index = i;
            value = v;
        }

        @Override
        public int compareTo(Pair other) {
            return -1 * Double.valueOf(value).compareTo(other.value);
        }
    }

    /**
     * Maximal number of documents. We're assuming here that we don't have more docs
     * than we can keep in main memory;
     */
    final static int MAX_NUMBER_OF_DOCS = 1000;

    final static int firstN = 30;

    /**
     * Mapping from document names to document numbers.
     */
    Hashtable<String, Integer> docNumber = new Hashtable<String, Integer>();

    /**
     * Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**
     * The transition matrix. p[i][j] = the probability that the random surfer
     * clicks from page i to page j.
     */
    double[][] p = new double[MAX_NUMBER_OF_DOCS][MAX_NUMBER_OF_DOCS];

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
     * In the initializaton phase, we use a negative number to represent that there
     * is a direct link from a document to another.
     */
    final static double LINK = -1.0;

    /**
     * Convergence criterion: Transition probabilities do not change more that
     * EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

    public static double distance(double[] A, double[] B) {
        int dimension = A.length;
        double result = 0.0;
        for (int i = 0; i < dimension; i++) {
            double diff = A[i] - B[i];
            result += diff * diff;
        }
        result = Math.sqrt(result);
        // System.out.println("distance is " + result + ".....");
        return result;
    }

    /* --------------------------------------------- */

    public PageRank(String filename) {
        int noOfDocs = readDocs(filename);
        initiateProbabilityMatrix(noOfDocs);
        iterate(noOfDocs, 100);
    }

    /* --------------------------------------------- */

    /**
     * Reads the documents and fills the data structures. When this method finishes
     * executing, <code>p[i][j] = LINK</code> if there is a direct link from i to j,
     * and <code>p[i][j] = 0</code> otherwise.
     * <p>
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
                    // Set the probability to LINK for now, to indicate that there is
                    // a link from d to otherDoc.
                    if (p[fromdoc][otherDoc] >= 0) {
                        p[fromdoc][otherDoc] = LINK;
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

    /* --------------------------------------------- */

    /*
     * Initiates the probability matrix.
     */
    void initiateProbabilityMatrix(int numberOfDocs) {

        for (int i = 0; i < numberOfDocs; i++) {
            if (out[i] == 0) {
                for (int j = 0; j < numberOfDocs; j++) {
                    p[i][j] = 1.0 / numberOfDocs;
                }
            } else {
                for (int j = 0; j < numberOfDocs; j++) {
                    if (p[i][j] < 0) {
                        p[i][j] = BORED / numberOfDocs;
                        p[i][j] += (1.0 - BORED) / out[i];
                    } else {
                        p[i][j] = BORED / numberOfDocs;
                    }
                }
            }
        }

    }

    /* --------------------------------------------- */

    /*
     * Chooses a probability vector a, and repeatedly computes aP, aP^2, aP^3...
     * until aP^i = aP^(i+1).
     */
    void iterate(int numberOfDocs, int maxIterations) {

        // YOUR CODE HERE
        int nIt = 0;
        double[] x = new double[numberOfDocs];
        double[] x_ = new double[numberOfDocs];
        x[0] = 1.0; // assume at the initial state
        double diff;
        while (++nIt < maxIterations) {
            diff = 0.;
            x_ = new double[numberOfDocs];
            for (int i = 0; i < numberOfDocs; i++) {
                for (int j = 0; j < numberOfDocs; j++) {
                    x_[i] += x[j] * p[j][i];
                }
                diff += Math.abs(x[i] - x_[i]);
            }
            x = x_.clone();
            if (diff < EPSILON)
                break;
        }
        System.out.println("Number of Iterations: " + nIt);

        ArrayList<Pair> resultList = new ArrayList<Pair>();
        for (int i = 0; i < numberOfDocs; i++) {
            Pair p = new Pair(i, x_[i]);
            resultList.add(p);
        }
        Collections.sort(resultList, (p1, p2) -> p1.compareTo(p2));
        for (int i = 0; i < firstN; i++) {
            Pair pair = resultList.get(i);
            String original = docName[pair.index];
            String value = String.format("%.5f", pair.value);
            System.out.println(original + ":" + value);

        }

    }

    /* --------------------------------------------- */

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Please give the name of the link file");
        } else {
            new PageRank(args[0]);
        }
    }
}
