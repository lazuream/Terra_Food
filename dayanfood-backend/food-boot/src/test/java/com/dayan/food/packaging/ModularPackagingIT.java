package com.dayan.food.packaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModularPackagingIT {
    @TempDir Path workspace;

    @Test
    void executableJarLoadsBusinessModulesAndRunsTheImageWorker() throws Exception {
        Path application = Path.of(System.getProperty("packaged.application")).toAbsolutePath().normalize();
        try (var archive = new JarFile(application.toFile())) {
            assertEquals("com.dayan.food.DayanFoodApplication",
                    archive.getManifest().getMainAttributes().getValue("Start-Class"));
            for (String module : new String[]{"common", "user", "catalog", "achievement", "community", "media", "agent"}) {
                assertTrue(archive.stream().anyMatch(entry -> entry.getName().startsWith("BOOT-INF/lib/food-" + module + "-")),
                        "Missing business library: " + module);
            }
            assertNotNull(archive.getEntry("BOOT-INF/classes/application.yml"));
        }
        Path output = workspace.resolve("probe.log");
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Djava.awt.headless=true",
                "-Dloader.path=" + Path.of(System.getProperty("packaged.testClasses")).toAbsolutePath().normalize(),
                "-Dloader.main=" + ModuleClasspathProbe.class.getName(),
                "-cp", application.toString(), "org.springframework.boot.loader.launch.PropertiesLauncher",
                workspace.toString()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(60, TimeUnit.SECONDS), "Packaged classpath probe timed out");
            String log = Files.readString(output);
            assertEquals(0, process.exitValue(), log);
            assertTrue(log.contains("MODULAR_CLASSPATH_OK"), log);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }
}
