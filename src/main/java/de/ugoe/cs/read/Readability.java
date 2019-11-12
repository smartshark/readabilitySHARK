package de.ugoe.cs.read;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

// import it.unimol.readability.metric.CombinedFeatureCalculator;
import de.ugoe.cs.read.exceptions.NoFileException;
import de.ugoe.cs.read.exceptions.ReadabilityParserException;
import it.unimol.readability.metric.FeatureCalculator;
import it.unimol.readability.metric.runnable.SerializedReadability;
import raykernel.apps.readability.eval.Main;

/**
 * Wraps rsm.jar for readability analysis.
 *
 * Needs working WordNet install with all features:
 *
 * This does not work:
 * System.setProperty("wordnet.database.dir", "/home/atx/WordNet-3.0/dict");
 *
 * This works:
 * download Full WordNET and cp WordNet-3.0/dict/* /usr/share/wordnet/
 *
 * Path /usr/share/wordnet is probably hardcoded in rsm.jar.
 */
public class Readability {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");


    /**
     * Helper function to slugify given names for readability features.
     * Source: https://stackoverflow.com/questions/1657193/java-code-library-for-generating-slugs-for-use-in-pretty-urls/1657250#1657250
     *
     * @param input
     * @return slugified feature name
     */
    public static String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }


    public static String readFile(File file) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        String source = new String(encoded, StandardCharsets.UTF_8);

        return source;
    }


    public static File normalizeLineEnding(File file) throws IOException {
        String fileString = readFile(file);

        fileString = fileString.replaceAll("\\r\\n", "\n");
        fileString = fileString.replaceAll("\\r", "\n");

        File normalized = File.createTempFile("normalized-", ".java");
        normalized.deleteOnExit();
        DataOutputStream os = new DataOutputStream(new FileOutputStream(normalized));
        os.write(fileString.getBytes());

        return normalized;
    }

    public static double getMeanReadabilityBuse(File file) throws NoFileException, IOException {
        if(file == null) {
            throw new NoFileException("File is null");
        }

        // we also need to check for 0
        if(file.length() == 0) {
            return Double.NaN;
        }

        return Main.getReadability(readFile(file));
    }

    /**
     * Wraps mean readability main method of rem.jar.
     * As that only uses System.out.println we replace the default out stream and read the data that way.
     *
     * @param file
     * @return mean readability
     * @throws NoFileException, ReadabilityParseError
     */
    public static double getMeanReadability(File file) throws NoFileException, ReadabilityParserException, IOException {
        // todo for method-level granularity: implement java method descriptors like in sourcemeter or in jacoco:
        // https://github.com/jacoco/jacoco/blob/0bcd964b0b784a25c4fee410a0c163399384b69f/org.jacoco.report/src/org/jacoco/report/JavaNames.java
        // definition here: https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3
        // method descriptors to link to SourceMeter output currently not implemented in javaparser/symbolsolver: https://github.com/javaparser/javasymbolsolver/issues/141

        /* output from rsm.jar:
        [INFO] RiTa.WordNet.version [033]
        Class mean readability:0.998885452747345
        or in case no class can be found
        Snippet readability:0.9284707307815552
        if a file is empty
        0
         */

        if(file == null) {
            throw new NoFileException("File is null");
        }

        // we also need to check for 0
        if(file.length() == 0) {
            return Double.NaN;
        }

        // re-set the stdout stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream old = System.out;
        System.setOut(ps);

        // reset the stderr stream
        ByteArrayOutputStream errba = new ByteArrayOutputStream();
        PrintStream errps = new PrintStream(errba);
        PrintStream errold = System.err;
        System.setErr(errps);

        File normalized = normalizeLineEnding(file);
        String[] args = {normalized.getAbsolutePath()};
        SerializedReadability.main(args);

        // flush output streams
        System.out.flush();
        System.err.flush();

        // re-set to original streams
        System.setOut(old);
        System.setErr(errold);

        // it is either mean class or snippet readability
        String[] capture = baos.toString().split("Class mean readability:");
        String[] capture2 = baos.toString().split("Snippet readability:");

        double result;
        if(capture.length == 2) {
            result = Double.parseDouble(capture[1]);
        } else {
            if(capture2.length == 2) {
                result = Double.parseDouble(capture2[1]);
            }else {
                throw new ReadabilityParserException(baos.toString());
            }
        }

        return result;
    }


    /**
     * Loads the File into a String and calculates the readability features.
     *
     * @param file
     * @return Map<String, Double>
     * @throws NoFileException
     */
    public static Map<String, Double> getReadabilityFeatures(File file) throws NoFileException, IOException {
        if(file == null) {
            throw new NoFileException("File is null");
        }

        String source = readFile(file);

        Map<String, Double> result = getReadabilityFeatures(source);

        return result;
    }


    /**
     * Extract Features with FeatureCalculator
     *
     * The names are slugified and returned as a map with their values (can be NaN).
     * we do not use the CombinedFeatureCalculator for now.
     *
     * @param source
     * @return
     */
    public static Map<String, Double> getReadabilityFeatures(String source) {
        Map<String, Double> result = new HashMap<String, Double>();
        // CombinedFeatureCalculator cfc = new CombinedFeatureCalculator();
        for(FeatureCalculator fc : FeatureCalculator.getFeatureCalculators()) {
            fc.setSource(source);
            double value = fc.calculate();
            String name = fc.getName();
            result.put(Readability.toSlug(name), value);
            // if(!Double.isNaN(value)) {
            //     cfc.plugin(fc, 1);
            // }
        }

        // cfc.setSource(source);
        // double value2 = cfc.calculate();
        // String name2 = cfc.getName();
        //System.out.println(name2 + " : " + value2);

        return result;
    }

}
