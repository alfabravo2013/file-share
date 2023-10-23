package fileshare.web;

import fileshare.service.FileContainer;
import fileshare.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/v1")
public class UploadRestController {
    private final int maxFileSize = 50_000;
    private final FileService service;

    private final String baseUrl = "http://localhost:8888/api/v1";

    public UploadRestController(FileService service) {
        this.service = service;
    }

    @GetMapping(path = "/info")
    public ResponseEntity<InfoResponse> info() {
        try {
            var info = service.getInfo();
            return ResponseEntity
                    .ok()
                    .body(info);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }

    @PostMapping(path = "/upload")
    public ResponseEntity<?> upload(@RequestParam(name = "file") MultipartFile file) {

        if (file.getSize() > maxFileSize) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        var id = service.save(file);
        return ResponseEntity
                .created(URI.create(baseUrl + "/download/" + id))
                .build();
    }

    @GetMapping(path = "/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable String id) {
        try {
            FileContainer container = service.load(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.attachment().filename(container.originalName()).build());
            System.out.println(container.mediaType());
            headers.setContentType(MediaType.parseMediaType(container.mediaType()));
            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(container.resource());
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity
                    .notFound()
                    .build();
        }
    }
}
