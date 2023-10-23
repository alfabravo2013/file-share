package fileshare.service;

import org.springframework.core.io.Resource;

public record FileContainer(
        Resource resource,
        String mediaType,
        String originalName
) {
}
