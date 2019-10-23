import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.logging.Redwood;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Demonstrates how to first use the tagger, then use the NN dependency
 * parser. Note that the parser will not work on untagged text.
 *
 * @author Jon Gauthier
 */
public class Parser  {

    /** A logger for this class */
    private static final Redwood.RedwoodChannels log = Redwood.channels(Parser.class);

    private static String focusWordPattern = "[Ss]pecific";  // There are 3 focus words which are (S|s)pecific, (B|b)ackground staining, (C|c)ross( |-)reactiv

    private static  String modifier = "nmod"; // change here for other modifier (amod, nmod:poss, ...)

    private static String FilePath = "/home/ploy/Desktop/antibody_specificity/all_snippets.tsv"; // change here for input file path

    private static String OutputFilePath = "/home/ploy/Desktop/antibody_specificity"; // chnage here for output file path

    private Parser() {} // static main method only

    public static void main(String[] args) throws IOException, InterruptedException {
        String modelPath = DependencyParser.DEFAULT_MODEL;
        String taggerPath = "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger";

        // create a Pattern object
        Pattern pattern = Pattern.compile(focusWordPattern);

        for (int argIndex = 0; argIndex < args.length; ) {
            switch (args[argIndex]) {
                case "-tagger":
                    taggerPath = args[argIndex + 1];
                    argIndex += 2;
                    break;
                case "-model":
                    modelPath = args[argIndex + 1];
                    argIndex += 2;
                    break;
                case "-modifier":
                    modifier = args[argIndex + 1];
                    argIndex += 2;
                    break;
                default:
                    throw new RuntimeException("Unknown argument " + args[argIndex]);
            }
        }

        MaxentTagger tagger = new MaxentTagger(taggerPath);
        DependencyParser parser = DependencyParser.loadFromModelFile(modelPath);

        // Read files
        ArrayList<String> snippets = getSnippetsFromFiles();

        // Counter {word, counter}
        HashMap<String, Integer> map_passive = new HashMap<>(); // first word
        HashMap<String, Integer> map_active = new HashMap<>(); // second word

        // For progress animation
        String anim= "|/-\\";

        for (int i = 0; i < snippets.size(); i++) {
            GrammaticalStructure gs = null;

            DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(snippets.get(i)));
            for (List<HasWord> sentence : tokenizer) {
                List<TaggedWord> tagged = tagger.tagSentence(sentence);
                gs = parser.predict(tagged);

                Iterator iterator = gs.typedDependencies().iterator();

                while (iterator.hasNext()) {
                    TypedDependency t = (TypedDependency) iterator.next();
                    if (String.valueOf(t.reln()).equals(modifier)) {
                        // dep(focusWordPattern, x)
                        Matcher matcher = pattern.matcher(t.gov().value());
                        if (matcher.find()) {
                            String key = String.format("(%s, %s)", t.gov().value(), t.dep().value());
                            if (map_active.containsKey(key)) {
                                map_active.put(key, map_active.get(key) + 1);
                            }
                            else {
                                map_active.put(key, 1);
                            }
                        }
                        else {
                            // dep(x, focusWordPattern)
                            matcher = pattern.matcher(t.dep().value());
                            if (matcher.find()) {
                                String key = String.format("(%s, %s)", t.gov().value(), t.dep().value());
                                if (map_passive.containsKey(key)) {
                                    map_passive.put(key, map_passive.get(key) + 1);
                                }
                                else {
                                    map_passive.put(key, 1);
                                }
                            }
                        }
                    }
                }
            }

            String data = "\r" + anim.charAt(i % anim.length()) + " " + i + "/" + (snippets.size()-1);
            System.out.write(data.getBytes());
            Thread.sleep(10);
        }

        ArrayList<String> deps = new ArrayList<>();
        deps.add(getFileContent(map_active));
        deps.add(getFileContent(map_passive));

        int index = 1;
        for (String content: deps) {
            try {
                writeToFile(String.format("%s/%s-part%d.tsv", OutputFilePath, modifier, index), content);
                index++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static String getFileContent(HashMap<String, Integer> map) {
        String s = "";
        for (String key : map.keySet()) {
            String row = key + "\t" + map.get(key);
            s = s + row + "\n";
        }

        return s;
    }

    public static void writeToFile(String filename, String fileContent) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write(fileContent);
        writer.close();
    }

    public static ArrayList<String> getSnippetsFromFiles() {
        ArrayList<String> snippets = new ArrayList<>();
        try {
            Scanner scanner = new Scanner((new File(FilePath)));
            while (scanner.hasNextLine()) {
                String[] lines = (scanner.nextLine()).split("\t");
                snippets.add(lines[5]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return snippets;
    }

}