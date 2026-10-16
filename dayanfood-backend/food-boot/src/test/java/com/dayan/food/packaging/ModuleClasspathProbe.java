package com.dayan.food.packaging;

import com.dayan.food.image.ImageSubprocess;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Runs both on the reactor classpath and through the packaged Boot classloader, without a database. */
public final class ModuleClasspathProbe {
    private static final Set<String> MAPPERS = Set.of(
            "AchievementMapper", "AppUserMapper", "ChallengeRedemptionMapper", "DiscoveryVersionMapper",
            "EtchingDesignMapper", "FavoriteMapper", "FoodCheckinMapper", "FoodCommentMapper",
            "FoodIdempotencyMapper", "FoodLikeMapper", "FoodMapper", "FoodTagMapper", "ImageAssetMapper",
            "ProfileStatsMapper", "RegionMapper", "UserReviewItemMapper", "WishlistMapper");

    private ModuleClasspathProbe() {}

    public static void verifyResources() throws Exception {
        var resolver = new PathMatchingResourcePatternResolver();
        var properties = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("backend-defaults.yml")).getFirst();
        var resources = resolver.getResources((String) properties.getProperty("mybatis.mapper-locations"));
        Set<String> names = Arrays.stream(resources)
                .map(resource -> resource.getFilename().replace(".xml", ""))
                .collect(Collectors.toSet());
        require(names.equals(MAPPERS) && resources.length == MAPPERS.size(),
                "Missing or duplicated mapper resources: " + names);

        Configuration configuration = new Configuration();
        for (var resource : resources) {
            try (var input = resource.getInputStream()) {
                new XMLMapperBuilder(input, configuration, resource.toString(), configuration.getSqlFragments()).parse();
            }
        }
        // Force deferred result maps and cross-namespace includes to resolve as well.
        configuration.getMappedStatementNames();
        for (String mapper : MAPPERS) {
            Class<?> type = Class.forName("com.dayan.food.mapper." + mapper);
            require(configuration.hasMapper(type), "Mapper not bound: " + mapper);
            for (var method : type.getDeclaredMethods()) {
                if (!method.isDefault() && !java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    require(configuration.hasStatement(type.getName() + "." + method.getName()),
                            "SQL statement missing: " + mapper + "." + method.getName());
                }
            }
        }
        require(resolver.getResources("classpath*:db/migration/V*__*.sql").length == 31,
                "Published Flyway history must be available exactly once");
        try (var input = new ClassPathResource("city-centers.csv").getInputStream()) {
            require(input.read() != -1, "City centers are missing");
        }

        var scanner = new ClassPathScanningCandidateComponentProvider(true);
        Set<String> components = scanner.findCandidateComponents("com.dayan.food").stream()
                .map(definition -> definition.getBeanClassName()).collect(Collectors.toSet());
        for (String name : Set.of("AuthController", "FoodController", "FoodCommentController",
                "AchievementController", "ImageController", "AgentController", "ApiExceptionHandler")) {
            require(components.contains("com.dayan.food.controller." + name), "Component not discovered: " + name);
        }
    }

    public static void main(String[] args) throws Exception {
        verifyResources();
        Path workspace = Path.of(args[0]);
        Path original = workspace.resolve("original.png");
        Path output = Files.createDirectory(workspace.resolve("variants"));
        ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), "png", original.toFile());
        // This launches the real worker via PropertiesLauncher from the same executable JAR.
        ImageSubprocess.decode(original, output);
        for (int size : new int[]{320, 640, 1280}) {
            require(ImageIO.read(output.resolve(size + ".png").toFile()) != null, "Image worker output missing");
        }
        System.out.println("MODULAR_CLASSPATH_OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
