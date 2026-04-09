import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ListFilesTest {
    private static final String BASE_PATH_NAME = "src/test/resources/testcase";
    private static final Path BASE_PATH = Paths.get(BASE_PATH_NAME);
    private static final Path BASE_PATH_DIR = Paths.get(BASE_PATH_NAME + "/dir ");
    private static final Path BASE_PATH_DIR_FILE = Paths.get(BASE_PATH_NAME + "/dir /file.txt");

    @TempDir
    private Path tmpdir;

    @Test
    void filesWalkStream() throws IOException {
        try (var stream = Files.walk(BASE_PATH)) {
            List<Path> list = stream.sorted().toList();
            System.out.println("listStream = " + list);
            assertEquals(List.of(BASE_PATH, BASE_PATH_DIR, BASE_PATH_DIR_FILE), list);
        }
    }

    @Test
    void filesWalkTree() throws IOException {
        List<Path> list = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>();
        Files.walkFileTree(BASE_PATH, new SimpleFileVisitor<>() {
            @Override
            public @NonNull FileVisitResult preVisitDirectory(@NonNull Path dir, @NonNull BasicFileAttributes attrs) throws IOException {
                list.add(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NonNull FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs) throws IOException {
                list.add(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NonNull FileVisitResult visitFileFailed(@NonNull Path file, @NonNull IOException exc) throws IOException {
                exc.printStackTrace();
                exceptions.add(exc);
                return FileVisitResult.CONTINUE;
            }
        });
        Collections.sort(list);
        System.out.println("  listTree = " + list);
        assertEquals(List.of(BASE_PATH,
                Path.of("src/test/resources/testcase/dir "),
                Path.of("src/test/resources/testcase/dir /file.txt")
                ), list);
        if (!exceptions.isEmpty()) {
            System.out.println("exceptions = " + exceptions);
        }
        assertTrue(exceptions.isEmpty());
    }

    @Test
    void createDirTrailingWhitespace() throws IOException {
        Path dir = tmpdir.resolve("mydir ");
        Files.createDirectory(dir);
        assertTrue(Files.isDirectory(dir));
        Path file = dir.resolve("file.txt");
        Files.createFile(file);
        assertTrue(Files.isRegularFile(file));
        Files.delete(file);
        Files.delete(dir);
    }
}

