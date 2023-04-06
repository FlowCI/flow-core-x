package com.flowci.parser;

import com.flowci.parser.v1.YmlParserTest;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestUtil {

    public static String loadContent(String resource) throws IOException {
        ClassLoader classLoader = YmlParserTest.class.getClassLoader();
        URL url = classLoader.getResource(resource);
        return Files.readString(Path.of(url.getFile()));
    }
}
