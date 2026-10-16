package com.dayan.user;
import org.junit.jupiter.api.Test;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;
class ServicePackagingIT {
    @Test void executableContainsOnlyItsOwnBusinessImplementations() throws Exception {
        try (var jar=new JarFile(System.getProperty("packaged.application"))) {
            for (String library:new String[]{"food-user-0.0.1-SNAPSHOT.jar", "food-achievement-0.0.1-SNAPSHOT.jar"})
                assertNotNull(jar.getEntry("BOOT-INF/lib/"+library),"Missing library: "+library);
            for (String library:new String[]{"food-catalog-0.0.1-SNAPSHOT.jar", "food-community-0.0.1-SNAPSHOT.jar", "food-media-0.0.1-SNAPSHOT.jar", "food-agent-0.0.1-SNAPSHOT.jar", "food-user-client-0.0.1-SNAPSHOT.jar"})
                assertNull(jar.getEntry("BOOT-INF/lib/"+library),"Leaked business implementation: "+library);
        }
    }
}
