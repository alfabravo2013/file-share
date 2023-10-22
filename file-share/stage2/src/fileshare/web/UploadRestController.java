package fileshare.web;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping(path = "/api/v1")
public class UploadRestController {

    @Value("${uploads.dir}")
    private String baseDir;
    private final String baseUrl = "http://localhost:8888/api/v1";

    @GetMapping(path = "/info")
    public ResponseEntity<InfoResponse> info() {
        var count = new AtomicInteger(0);
        var size = new AtomicLong(0);
        try (var stream = Files.walk(Path.of(baseDir))) {
            stream.filter(path -> !path.toFile().isDirectory())
                    .forEach(path -> {
                        try {
                            count.addAndGet(1);
                            size.addAndGet(Files.size(path));
                        } catch (IOException e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    });
            return ResponseEntity.ok().body(new InfoResponse(count.get(), size.get()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }

    @PostMapping(path = "/upload")
    public ResponseEntity<?> upload(@RequestParam(name = "file") MultipartFile file) {
        try {
            var filename = Objects.requireNonNull(file.getOriginalFilename());
            Path destination = Path.of(baseDir, filename);
            file.transferTo(destination);
            return ResponseEntity
                    .created(URI.create(baseUrl + "/download/" + URLEncoder.encode(filename, StandardCharsets.UTF_8)))
                    .build();
        } catch (FileNotFoundException e) {
            return ResponseEntity
                    .notFound()
                    .build();
        } catch (IOException e) {
            var message = "Error saving file: " + e.getClass().getSimpleName() + "; " + e.getMessage();
            System.out.println(message);
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", message));
        }
    }

    @GetMapping(path = "/download/{url}")
    public ResponseEntity<Resource> download(@PathVariable String url) {
        String filename = url.replaceAll(baseUrl, "");
        Resource resource = new PathResource(Path.of(baseDir, URLDecoder.decode(filename, StandardCharsets.UTF_8)));
        if (resource.exists()) {
            return ResponseEntity
                    .ok()
                    .body(resource);
        }

        return ResponseEntity.notFound().build();
    }

    @PostConstruct
    public void init() {
        Path path = Path.of(baseDir);
        if (!path.toFile().exists()) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
