package de.kfzteile24.salesOrderHub.helper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;

public class FileUtil {

    public static String readResource(Class<?> clazz, String path) throws URISyntaxException, IOException {
        return java.nio.file.Files.readString(Paths.get(
                Objects.requireNonNull(clazz.getClassLoader().getResource(path))
                        .toURI()));
    }
}
