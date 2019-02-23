import java.util.*;
import java.nio.*;
import java.io.*;
import java.lang.Math;

public class MontCarlo {

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
    public class MCReturn {
        int[] result;
        long totalVisit;

        MCReturn(int[] result, long totalVisit) {
            this.result = result;
            this.totalVisit = totalVisit;
        }
    }

    public MontCarlo(String filename, String _method, String _m) {
        int numberOfDocs = readDocs(filename);
        int m = Integer.valueOf(_m);
        int N = numberOfDocs * m;
        int method = Integer.valueOf(_method);
        int[] visits = new int[numberOfDocs];
        long totalVisit = 0; // a holder for mc3 && mc4

        switch (method) {
        case 1:
            visits = mc1(numberOfDocs, N);
            break;
        case 2:
            visits = mc2(numberOfDocs, N);
            break;
        case 4:
            MCReturn mcReturn = mc4(numberOfDocs, N);
            visits = mcReturn.result;
            totalVisit = mcReturn.totalVisit;
            break;
        default:
            MCReturn mcReturn_ = mc5(numberOfDocs, N);
            visits = mcReturn_.result;
            totalVisit = mcReturn_.totalVisit;

        }
        final int firstN = 30;
        ArrayList<PRResult> resultList = new ArrayList<PRResult>();
        for (int i = 0; i < numberOfDocs; i++) {
            String title = docName[i];
            double score = 0;
            if (method < 3) {
                // for mc 1 & mc2
                score = Double.valueOf(visits[i]) / N;
            } else {
                score = Double.valueOf(visits[i]) / totalVisit;
            }
            PRResult r = new PRResult(i, title, score);
            resultList.add(r);
        }
        Collections.sort(resultList);
        for (int i = 0; i < firstN; i++) {
            PRResult result = resultList.get(i);
            String formattedScore = String.format("%.5f", result.score);
            System.out.println(result.title + "\t" + formattedScore);

        }
    }

    public int[] mc1(int numberOfDocs, int N) {

        long startTime = System.currentTimeMillis();
        Random seed = new Random();
        int visits[] = new int[numberOfDocs];
        int count = 0;

        while (count++ < N) {
            int currentPage = seed.nextInt(numberOfDocs);
            double getBored = seed.nextDouble();
            int finalPage = seed.nextInt(numberOfDocs);

            while (getBored > BORED) {
                // as long as not bored yet
                getBored = seed.nextDouble();
                int outSize = out[currentPage];
                if (outSize == 0) {
                    finalPage = seed.nextInt(numberOfDocs);
                    // a dangling node, takes a random jump
                } else {
                    HashMap<Integer, Boolean> linkMap = link.get(currentPage);
                    if (outSize == 1) {
                        finalPage = linkMap.keySet().iterator().next();
                    } else {
                        int goTo = seed.nextInt(outSize);
                        Integer[] linkTo = linkMap.keySet().toArray(new Integer[outSize]);
                        finalPage = linkTo[goTo];
                    }
                }
                currentPage = finalPage;
            }
            visits[finalPage]++;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        // System.out.println("mc1 takes \t " + elapsed + " \t ms to finish");
        return visits;
    }

    /* --------------------------------------------- */
    public int[] mc2(int numberOfDocs, int maxRuns) {
        long startTime = System.currentTimeMillis();
        Random seed = new Random();
        int visits[] = new int[numberOfDocs];
        int m = maxRuns / numberOfDocs;
        for (int i = 0; i < m; i++) {
            // m times
            for (int n = 0; n < numberOfDocs; n++) {
                // n docs
                int currentPage = n;
                int finalPage = seed.nextInt(numberOfDocs);
                while (seed.nextDouble() > BORED) {
                    // as long as not bored yet
                    int outSize = out[currentPage];
                    if (outSize == 0) {
                        finalPage = seed.nextInt(numberOfDocs);
                    } else {
                        HashMap<Integer, Boolean> linkMap = link.get(currentPage);
                        if (outSize == 1) {
                            finalPage = linkMap.keySet().iterator().next();
                        } else {
                            int goTo = seed.nextInt(outSize);
                            Integer[] linkTo = linkMap.keySet().toArray(new Integer[outSize]);
                            finalPage = linkTo[goTo];
                        }
                    }
                    currentPage = finalPage;

                }
                visits[finalPage]++;
            }

        }

        long elapsed = System.currentTimeMillis() - startTime;
        // System.out.println("mc2 takes \t " + elapsed + " \t ms to finish");

        return visits;
    }

    public MCReturn mc4(int numberOfDocs, int N) {
        long startTime = System.currentTimeMillis();
        Random seed = new Random();
        int visits[] = new int[numberOfDocs];
        long totalVisit = 0l;
        int m = N / numberOfDocs;
        for (int i = 0; i < m; i++) {
            for (int n = 0; n < numberOfDocs; n++) {
                int currentPage = n;
                double getBored;
                while (true) {
                    // initiate a infinity loop;
                    totalVisit++;
                    visits[currentPage]++;
                    getBored = seed.nextDouble();
                    if (getBored < BORED) {
                        // currentPage = seed.nextInt(numberOfDocs);
                        // continues
                        break;
                    }
                    // the user is not bored yet
                    // check if it's a dangling node here
                    int outSize = out[currentPage];
                    if (outSize == 0) {
                        // reaches a dangling node
                        break;
                    }
                    // not a dangling a node, pick a link
                    HashMap<Integer, Boolean> linkMap = link.get(currentPage);
                    if (outSize == 1) {
                        currentPage = linkMap.keySet().iterator().next();
                    } else {
                        int goToIdx = seed.nextInt(outSize);
                        Integer[] linkTo = linkMap.keySet().toArray(new Integer[outSize]);
                        currentPage = linkTo[goToIdx];
                    }
                    // the loop continues
                }
            }

        }
        MCReturn toReturn = new MCReturn(visits, totalVisit);
        long elapsed = System.currentTimeMillis() - startTime;
        // System.err.println("mc4 takes \t " + elapsed + " \t ms to finish");
        return toReturn;
    }

    public MCReturn mc5(int numberOfDocs, int N) {
        long startTime = System.currentTimeMillis();
        Random seed = new Random();
        int visits[] = new int[numberOfDocs];
        long totalVisit = 0l;
        int i = 0;
        while (i++ < N) {
            int currentPage = seed.nextInt(numberOfDocs);
            double getBored;
            while (true) {
                // initiate a infinity loop;
                totalVisit++;
                visits[currentPage]++;
                getBored = seed.nextDouble();
                if (getBored < BORED) {
                    // currentPage = seed.nextInt(numberOfDocs);
                    // continues
                    break;
                }
                // the user is not bored yet
                // check if it's a dangling node here
                int outSize = out[currentPage];
                if (outSize == 0) {
                    // reaches a dangling node
                    break;
                }
                // not a dangling a node, pick a link
                HashMap<Integer, Boolean> linkMap = link.get(currentPage);
                if (outSize == 1) {
                    currentPage = linkMap.keySet().iterator().next();
                } else {
                    int goToIdx = seed.nextInt(outSize);
                    Integer[] linkTo = linkMap.keySet().toArray(new Integer[outSize]);
                    currentPage = linkTo[goToIdx];
                }
                // the loop continues
            }

        }
        MCReturn toReturn = new MCReturn(visits, totalVisit);
        long elapsed = System.currentTimeMillis() - startTime;
        // System.err.println("mc5 takes \t " + elapsed + " \t ms to finish");
        return toReturn;
    }

    /**
     * Reads the documents and fills the data structures.
     *
     * @return the number of documents read.
     */
    int readDocs(String filename) {
        int fileIndex = 0;
        try {
            // System.err.print("Reading file... ");
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
                // System.err.print("stopped reading since documents table is full. ");
            } else {
                // System.err.print("done. ");
            }
        } catch (FileNotFoundException e) {
            System.err.println("File " + filename + " not found!");
        } catch (IOException e) {
            System.err.println("Error reading file " + filename);
        }
        System.err.println("Read " + fileIndex + " number of documents");
        return fileIndex;
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

    }

    /* --------------------------------------------- */

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("three agrs: 1.linkfile \t2.which MC to use(1/2/4/5) \t3.ratio m, where N=nm");
        } else {
            new MontCarlo(args[0], args[1], args[2]);
        }
    }
}