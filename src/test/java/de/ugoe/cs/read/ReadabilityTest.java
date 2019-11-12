package de.ugoe.cs.read;

import java.io.File;

import de.ugoe.cs.read.exceptions.NoFileException;
import de.ugoe.cs.read.exceptions.ReadabilityParserException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReadabilityTest {

    @Test
    void testMeanReadability() {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("MyClass.java").getFile());

        double result;
        double expected = 0.998885452747345;

        try {
            result = Readability.getMeanReadability(file);
        }catch(NoFileException e) {
            result = 0;
        }catch(IOException e1) {
            result = 0;
        }catch(ReadabilityParserException e2) {
            result = 0;
        }

        assertEquals(expected, result);
    }

    @Test
    void testBuseReadability() {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("MyClass.java").getFile());

        double result;
        double expected = 0.9990423917770386;

        try {
            result = Readability.getMeanReadabilityBuse(file);
        }catch(NoFileException e) {
            result = 0;
        }catch(IOException e1) {
            result = 0;
        }

        assertEquals(expected, result);
    }
}