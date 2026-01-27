package net.talaatharb.workday;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/**
 * Test to verify the MVC architecture setup with proper package structure.
 */
class ProjectArchitectureTest {
    
    private static final String BASE_PACKAGE_PATH = "src/main/java/net/talaatharb/workday";
    
    @Test
    void testRequiredPackagesExist() {
        String[] requiredPackages = {
            "ui/controllers",
            "facade",
            "service",
            "mapper",
            "repository",
            "model",
            "event",
            "config",
            "utils"
        };
        
        for (String packagePath : requiredPackages) {
            Path path = Paths.get(BASE_PACKAGE_PATH, packagePath);
            assertTrue(Files.exists(path) && Files.isDirectory(path),
                "Package '" + packagePath + "' should exist at: " + path);
        }
    }
    
    @Test
    void testPackageInfoFilesExist() {
        String[] packagesWithInfo = {
            "repository",
            "model",
            "event"
        };
        
        for (String packagePath : packagesWithInfo) {
            Path packageInfoPath = Paths.get(BASE_PACKAGE_PATH, packagePath, "package-info.java");
            assertTrue(Files.exists(packageInfoPath),
                "package-info.java should exist for: " + packagePath);
        }
    }
    
    @Test
    void testApplicationContextExists() {
        Path contextPath = Paths.get(BASE_PACKAGE_PATH, "config", "ApplicationContext.java");
        assertTrue(Files.exists(contextPath),
            "ApplicationContext.java should exist in config package");
    }
    
    @Test
    void testPomXmlContainsMavenAndJavaFXDependencies() throws Exception {
        Path pomPath = Paths.get("pom.xml");
        assertTrue(Files.exists(pomPath), "pom.xml should exist");
        
        String pomContent = Files.readString(pomPath);
        
        assertTrue(pomContent.contains("org.openjfx"), 
            "pom.xml should contain JavaFX dependencies");
        assertTrue(pomContent.contains("javafx-controls") || pomContent.contains("javafx-fxml"),
            "pom.xml should contain JavaFX controls or fxml dependency");
        assertTrue(pomContent.contains("org.projectlombok"),
            "pom.xml should contain Lombok dependency");
        assertTrue(pomContent.contains("org.mapstruct"),
            "pom.xml should contain MapStruct dependency");
    }
}
