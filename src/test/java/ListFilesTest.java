import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class ListFilesTest {
    private static final String BASE_PATH = "src/test/resources/testcase";
    private static final String BASE_PATH_GITKEEP = BASE_PATH + "/.gitkeep";
    private static final String BASE_PATH_DIR = BASE_PATH + "/dir ";
    private static final String BASE_PATH_DIR_FILE = BASE_PATH + "/dir /file.txt";

    @TempDir
    private Path tmpdir;

    private static boolean isWin() {
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }

    @BeforeEach
    void prepareBaseDir() throws IOException, InterruptedException {
        if (isWin()) {
            String absoluteBasePath = Path.of(BASE_PATH).toAbsolutePath().toString();
            absoluteBasePath = "\\\\?\\" + absoluteBasePath + "\\dir ";
            Runtime.getRuntime().exec(new String[]{"cmd", "/C", "mkdir \"" +  absoluteBasePath + "\""});
            Runtime.getRuntime().exec(new String[]{"cmd", "/C", "type nul > \"" + absoluteBasePath + "\\file.txt\""});
            Process dirProcess = new ProcessBuilder(
                    "cmd", "/C", "dir /S " + Path.of(BASE_PATH).toAbsolutePath()
            ).redirectErrorStream(true).start();
            System.out.println("dir: " + new String(dirProcess.getInputStream().readAllBytes(), Charset.defaultCharset()));
            dirProcess.waitFor();
        } else {
            Files.createDirectories(Path.of(BASE_PATH_DIR));
            Files.createFile(Path.of(BASE_PATH_DIR_FILE));
        }
    }

    @AfterEach
    void cleanBaseDir() throws IOException {
        if (isWin()) {
            String absoluteBasePath = Path.of(BASE_PATH).toAbsolutePath().toString();
            absoluteBasePath = "\\\\?\\" + absoluteBasePath + "\\dir ";
            Runtime.getRuntime().exec(new String[]{"cmd", "/C", "del \"" + absoluteBasePath + "\\file.txt\""});
            Runtime.getRuntime().exec(new String[]{"cmd", "/C", "rd \"" + absoluteBasePath + "\""});
        } else {
            Files.delete(Path.of(BASE_PATH_DIR_FILE));
            Files.delete(Path.of(BASE_PATH_DIR));
        }
    }

    private static void assertPathList(List<String> expected, List<String> actual) {
        assertEquals(expected.toString().replace('\\', '/'),
                actual.toString().replace('\\', '/'));
    }

    @Test
    void filesWalkStream() throws IOException {
        try (var stream = Files.walk(Path.of(BASE_PATH))) {
            List<String> list = stream.sorted().map(Path::toString).toList();
            System.out.println("listStream = " + list);
            assertPathList(List.of(BASE_PATH, BASE_PATH_GITKEEP, BASE_PATH_DIR, BASE_PATH_DIR_FILE), list);
        }
    }

    @Test
    void filesWalkTree() throws IOException {
        List<String> list = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>();
        Files.walkFileTree(Path.of(BASE_PATH), new SimpleFileVisitor<>() {
            @Override
            public @NonNull FileVisitResult preVisitDirectory(@NonNull Path dir, @NonNull BasicFileAttributes attrs) throws IOException {
                list.add(dir.toString());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NonNull FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs) throws IOException {
                list.add(file.toString());
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
        if (!exceptions.isEmpty()) {
            System.out.println("exceptions = " + exceptions);
        }
        assertPathList(List.of(BASE_PATH, BASE_PATH_GITKEEP, BASE_PATH_DIR, BASE_PATH_DIR_FILE), list);
        assertTrue(exceptions.isEmpty());
    }

    @Test
    void fileList() {
        File baseDir = new File(BASE_PATH);
        String[] files = baseDir.list();
        Arrays.sort(files);
        System.out.println("     files = " + Arrays.toString(files));
        assertArrayEquals(new String[]{".gitkeep", "dir "}, files);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void createDirTrailingWhitespace() throws IOException {
        Path dir = tmpdir.resolve("mydir "); // throws java.nio.file.InvalidPathException under Windows
        Files.createDirectory(dir);
        assertTrue(Files.isDirectory(dir));
        Path file = dir.resolve("file.txt");
        Files.createFile(file);
        assertTrue(Files.isRegularFile(file));
        Files.delete(file);
        Files.delete(dir);
    }
}

