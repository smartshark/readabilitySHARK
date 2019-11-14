package de.ugoe.cs.read;

import java.io.File;

import de.ugoe.cs.read.exceptions.NoFileException;
import de.ugoe.cs.read.exceptions.ReadabilityParserException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReadabilityTest {

    @Test
    public void testMeanReadability() {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final File file = new File(classLoader.getResource("MyClass.java").getFile());

        double result;
        final double expected = 0.9990015029907227;

        try {
            result = ReadabilityHelper.getMeanReadability(file);
        }catch(NoFileException|IOException|ReadabilityParserException e) {
            result = 0;
        }

        assertEquals(expected, result);
    }

    @Test
    public void testBuseReadability() {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final File file = new File(classLoader.getResource("MyClass.java").getFile());

        double result;
        final double expected = 0.9990692138671875;

        try {
            result = ReadabilityHelper.getMeanReadabilityBuse(file);
        }catch(NoFileException|IOException e) {
            result = 0;
        }

        assertEquals(expected, result);
    }
}