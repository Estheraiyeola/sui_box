package org.example.processor;

import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.Compilation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

public class BlockchainEntityProcessorTest {
    private Path tempDir;

    @BeforeEach
    public void setUp() throws IOException {
        // Create a temporary directory for test output
        tempDir = Files.createTempDirectory("test-processor-" + UUID.randomUUID());
        Files.createDirectories(tempDir.resolve("sources"));
        Files.createDirectories(tempDir.resolve("test/templates"));
        Files.createDirectories(tempDir.resolve("org/example/templates"));
        // Set system properties to direct processor output
        System.setProperty("test.move.dir", tempDir.toString());
        System.setProperty("test.java.dir", tempDir.toString());
        System.out.println("Temporary directory: " + tempDir.toAbsolutePath());
    }

    @Test
    public void barEntityGeneratesExpectedMoveModule() throws IOException {
        JavaFileObject input = JavaFileObjects.forSourceLines(
                "test.Bar",
                "package test;",
                "import org.example.annotation.BlockchainEntity;",
                "@BlockchainEntity(module = \"foo\", struct = \"Bar\")",
                "public class Bar {",
                "    private String name;",
                "    private long count;",
                "}"
        );

        Compilation result = Compiler.javac()
                .withProcessors(new BlockchainEntityProcessor())
                .compile(input);
        assertThat(result).succeeded();

        // Verify Move file in test.move.dir/sources/
        Path moveFile = tempDir.resolve("sources/foo.move");
        System.out.println("Checking for Move file: " + moveFile.toAbsolutePath());
        if (!Files.exists(moveFile)) {
            System.out.println("Compilation diagnostics:");
            result.diagnostics().forEach(d -> System.out.println(d));
            throw new AssertionError("Move file not found at: " + moveFile);
        }
        String content = Files.readString(moveFile);

        // Expected content based on sui_box_module.move.ftl
        String expectedMoveSnippet = """
                module foo::Bar {
                    use sui::object::{Self, UID};
                    use sui::tx_context::{Self, TxContext};
                    use sui::transfer;
                    use std::string::{Self, String};
                    use std::vector;
                    use std::option::{Self, Option};

                    struct Bar has key, store {
                        id: UID,
                        name: String,
                        count: u64
                    }
                """;

        System.out.println("Expected Move snippet:\n" + expectedMoveSnippet);
        System.out.println("Generated Move content:\n" + content);

        assertThat(content, containsString("module foo::Bar"));
        assertThat(content, containsString("struct Bar has key, store"));
        assertThat(content, containsString("name: String"));
        assertThat(content, containsString("count: u64"));
    }

    @Test
    public void barEntityGeneratesExpectedJavaModel() throws IOException {
        JavaFileObject input = JavaFileObjects.forSourceLines(
                "test.Bar",
                "package test;",
                "import org.example.annotation.BlockchainEntity;",
                "@BlockchainEntity(module=\"foo\", struct=\"Bar\")",
                "public class Bar {",
                "    private String name;",
                "    private long count;",
                "}"
        );

        Compilation result = Compiler.javac()
                .withProcessors(new BlockchainEntityProcessor())
                .compile(input);
        assertThat(result).succeeded();

        // Check Java model in test.java.dir/test/templates/Bar.java
        Path javaFile = tempDir.resolve("test/templates/Bar.java");
        System.out.println("Checking for Java file: " + javaFile.toAbsolutePath());
        if (!Files.exists(javaFile)) {
            System.out.println("Compilation diagnostics:");
            result.diagnostics().forEach(d -> System.out.println(d));
            throw new AssertionError("Java file not found at: " + javaFile);
        }
        String content = Files.readString(javaFile);
        System.out.println("Generated Java content:\n" + content);

        assertThat(content, containsString("package test.templates;"));
        assertThat(content, containsString("public class Bar"));
    }
}