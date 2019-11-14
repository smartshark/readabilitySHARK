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
public final class ReadabilityHelper {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private ReadabilityHelper() {
        throw new AssertionError("Instantiating utility class...");
    }

    /**
     * Helper function to slugify given names for readability features.
     * Source: https://stackoverflow.com/questions/1657193/java-code-library-for-generating-slugs-for-use-in-pretty-urls/1657250#1657250
     *
     * @param input
     * @return slugified feature name
     */
    public static String toSlug(final String input) {
        final String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        final String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        final String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }


    public static String readFile(final File file) throws IOException {
        final byte[] encoded = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("PMD.CloseResource")
    public static File normalizeLineEnding(final File file) throws IOException {
        String fileString = readFile(file);

        fileString = fileString.replaceAll("\\r\\n", "\n");
        fileString = fileString.replaceAll("\\r", "\n");

        final File normalized = File.createTempFile("normalized-", ".java");
        normalized.deleteOnExit();
        final DataOutputStream dos = new DataOutputStream(Files.newOutputStream(normalized.toPath()));
        dos.write(fileString.getBytes());
        dos.close();
        return normalized;
    }

    public static double getMeanReadabilityBuse(final File file) throws NoFileException, IOException {
        if(file == null) {
            throw new NoFileException("File is null");
        }

        // we also need to check for 0
        double ret = Double.NaN;
        if(file.length() > 0) {
            ret = Main.getReadability(readFile(file));
        }

        return ret;
    }

    /**
     * Wraps mean readability main method of rem.jar.
     * As that only uses System.out.println we replace the default out stream and read the data that way.
     *
     * @param file
     * @return mean readability
     * @throws NoFileException, ReadabilityParseError
     */
    @SuppressWarnings({"PMD.OnlyOneReturn", "PMD.AvoidLiteralsInIfCondition", "PMD.CloseResource"})
    public static double getMeanReadability(final File file) throws NoFileException, ReadabilityParserException, IOException {
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

        double result = Double.NaN;

        // we also need to check for 0
        if(file.length() == 0) {
            return result;
        }

        // re-set the stdout stream
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream nps = new PrintStream(baos);
        final PrintStream old = System.out;
        System.setOut(nps);

        // reset the stderr stream
        final ByteArrayOutputStream errba = new ByteArrayOutputStream();
        final PrintStream errps = new PrintStream(errba);
        final PrintStream errold = System.err;
        System.setErr(errps);

        final File normalized = normalizeLineEnding(file);
        final String[] args = {normalized.getAbsolutePath()};
        SerializedReadability.main(args);

        // flush output streams
        System.out.flush();
        System.err.flush();

        // re-set to original streams
        System.setOut(old);
        System.setErr(errold);

        // close temporary streams
        errps.close();
        nps.close();

        // it is either mean class or snippet readability
        final String[] capture = baos.toString().split("Class mean readability:");
        final String[] capture2 = baos.toString().split("Snippet readability:");

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
    public static Map<String, Double> getReadabilityFeatures(final File file) throws NoFileException, IOException {
        if(file == null) {
            throw new NoFileException("File is null");
        }

        final String source = readFile(file);

        return getReadabilityFeatures(source);
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
    public static Map<String, Double> getReadabilityFeatures(final String source) {
        final Map<String, Double> result = new HashMap<>();
        // CombinedFeatureCalculator cfc = new CombinedFeatureCalculator();
        for(final FeatureCalculator fc : FeatureCalculator.getFeatureCalculators()) {
            fc.setSource(source);
            final double value = fc.calculate();
            final String name = fc.getName();
            result.put(ReadabilityHelper.toSlug(name), value);
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
