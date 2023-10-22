package fileshare.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InfoResponse(
        @JsonProperty("total_files")
        int totalFiles,

        @JsonProperty("total_bytes")
        long totalBytes
) {
}
