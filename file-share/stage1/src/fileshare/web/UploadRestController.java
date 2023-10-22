package fileshare.web;

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
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping(path = "/api/v1")
public class UploadRestController {
    private final String baseDir = "uploads";
    private final String baseUrl = "http://localhost:8888/api/v1/";

    @PostMapping(path = "/upload")
    public ResponseEntity<?> upload(@RequestParam(name = "file") MultipartFile file) {
        try {
            var filename = Objects.requireNonNull(file.getOriginalFilename());
            Path destination = Path.of(baseDir, filename);
            file.transferTo(destination);
            return ResponseEntity
                    .created(URI.create(baseUrl + "download/" + URLEncoder.encode(filename, StandardCharsets.UTF_8)))
                    .build();
        } catch (FileNotFoundException e) {
            return ResponseEntity
                    .notFound()
                    .build();
        } catch (IOException e) {
            var message = "Error saving file: " + e.getClass().getSimpleName() + "; " + e.getMessage();
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", message));
        }
    }

    @GetMapping(path = "/download/{url}")
    public ResponseEntity<Resource> download(@PathVariable String url) {
        String filename = url.replaceAll(baseUrl, "");
        Resource resource = new PathResource(Path.of(baseDir, URLDecoder.decode(filename, StandardCharsets.UTF_8)));
        return ResponseEntity
                .ok()
                .body(resource);
    }
}
