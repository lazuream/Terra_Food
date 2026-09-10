package com.dayan.food.config;

import com.dayan.food.entity.po.ImageAsset;
import com.dayan.food.mapper.ImageAssetMapper;
import com.dayan.food.image.ImageDimensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(name = "app.upload.backfill-enabled", havingValue = "true")
public class ImageBackfillRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageBackfillRunner.class);
    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private final ImageAssetMapper mapper;
    private final Path directory;
    private final boolean dryRun;
    private final boolean retryFailed;
    private final int batchSize;
    private final String after;
    private final ConfigurableApplicationContext context;
    private final AtomicBoolean completed = new AtomicBoolean();

    public ImageBackfillRunner(ImageAssetMapper mapper,
            @Value("${app.upload-directory:uploads}") String directory,
            @Value("${app.upload.backfill-dry-run:true}") boolean dryRun,
            @Value("${app.upload.backfill-retry-failed:false}") boolean retryFailed,
            @Value("${app.upload.backfill-batch-size:100}") int batchSize,
            @Value("${app.upload.backfill-after:}") String after,
            ConfigurableApplicationContext context) {
        this.mapper=mapper; this.directory=Path.of(directory).toAbsolutePath().normalize();
        this.dryRun=dryRun; this.retryFailed=retryFailed; this.batchSize=Math.min(Math.max(batchSize,1),1000);
        this.after=after == null ? "" : after;
        this.context=context;
    }

    @Override public void run(ApplicationArguments args) throws Exception {
        int examined=0, queued=0, skipped=0, failed=0; String last="";
        if (Files.isDirectory(directory)) {
            try (var files=Files.list(directory)) {
                for (Path path : files.filter(Files::isRegularFile).sorted().toList()) {
                    if (path.getFileName().toString().compareTo(after) <= 0) continue;
                    if (examined >= batchSize) break;
                    String name=path.getFileName().toString(); int dot=name.lastIndexOf('.');
                    if (dot<0 || !EXTENSIONS.contains(name.substring(dot+1).toLowerCase())) continue;
                    examined++; last=name;
                    String url="/uploads/"+name; ImageAsset existing=mapper.findByOriginalUrl(url);
                    if (existing!=null) {
                        if (!dryRun && retryFailed && "FAILED".equals(existing.getStatus())) queued+=mapper.retryFailed(existing.getId());
                        else skipped++;
                        continue;
                    }
                    ImageDimensions dimensions;
                    try { dimensions=ImageDimensions.read(path); }
                    catch (Exception invalid) { failed++; continue; }
                    if (!dryRun) mapper.insert(new ImageAsset(url,dimensions.width(),dimensions.height()));
                    queued++;
                }
            }
        }
        LOGGER.info("image_backfill dryRun={} examined={} queued={} skipped={} failed={} nextAfter={}",dryRun,examined,queued,skipped,failed,last);
        completed.set(true);
    }

    @EventListener(ApplicationReadyEvent.class)
    void closeAfterApplicationIsReady() {
        if (!completed.get()) return;
        Thread shutdown = new Thread(context::close, "image-backfill-shutdown");
        shutdown.setDaemon(false);
        shutdown.start();
    }
}
