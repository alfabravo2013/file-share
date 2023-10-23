package fileshare.service;

import fileshare.repository.FileInfo;
import fileshare.repository.FileInfoRepository;
import fileshare.service.exception.PayloadTooLargeException;
import fileshare.service.exception.UnsupportedMediaTypeException;
import fileshare.web.InfoResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class FileService {
    private final FileInfoRepository repository;
    @Value("${uploads.dir}")
    private String baseDir;

    public FileService(FileInfoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Long save(MultipartFile file) {
        try {

            var md5Hash = Md5HashProvider.hash(file.getBytes());
            var originalFilename = Objects.requireNonNull(file.getOriginalFilename());
            var mediaType = Objects.requireNonNull(file.getContentType());
            var localFilename = UUID.randomUUID().toString();

            var existingFileInfo = repository.findFirstByHash(md5Hash).orElse(null);


            var fileInfo = new FileInfo();
            fileInfo.setOriginalName(originalFilename);
            fileInfo.setMediaType(mediaType);
            fileInfo.setLocalName(localFilename);
            fileInfo.setHash(md5Hash);

            if (existingFileInfo != null) {
                // validation not needed or there can be a situation
                // when the byte[] is the same but media type is manually set and not white-listed?
                if (originalFilename.equals(existingFileInfo.getOriginalName())
                        && md5Hash.equals(existingFileInfo.getHash())) {
                    return existingFileInfo.getId();
                }
                fileInfo.setLocalName(existingFileInfo.getLocalName());
                repository.save(fileInfo);
            } else {
                var freeSpace = 200_000 - getInfo().totalBytes();
                System.out.println("file size: " + file.getSize());
                System.out.println("free space: " + freeSpace);
                if (freeSpace < file.getSize()) {
                    throw new PayloadTooLargeException();
                }

                if (!MediaTypeValidator.isValidFileContents(file.getBytes(), mediaType)) {
                    throw new UnsupportedMediaTypeException();
                }

                repository.save(fileInfo);
                Path destination = Path.of(baseDir, localFilename);
                file.transferTo(destination);
            }

            return fileInfo.getId();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FileContainer load(String id) {
        try {
            var longId = Long.parseLong(id);
            var fileInfo = repository
                    .findById(longId)
                    .orElseThrow(() -> new RuntimeException("id=%d, file info not found".formatted(id)));

            Resource resource = new PathResource(Path.of(baseDir, fileInfo.getLocalName()));
            if (resource.exists()) {
                return new FileContainer(resource, fileInfo.getMediaType(), fileInfo.getOriginalName());
            }
            throw new RuntimeException("Resource not found");
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

    public InfoResponse getInfo() {
        var count = new AtomicInteger(0);
        var size = new AtomicLong(0);
        try (var stream = Files.walk(Path.of(baseDir))) {
            stream.filter(path -> !path.toFile().isDirectory())
                    .forEach(path -> {
                        try {
                            count.addAndGet(1);
                            size.addAndGet(Files.size(path));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            return new InfoResponse(count.get(), size.get());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
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
