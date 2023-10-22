import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.dynamic.input.DynamicTesting;
import org.hyperskill.hstest.exception.outcomes.WrongAnswer;
import org.hyperskill.hstest.stage.SpringTest;
import org.hyperskill.hstest.testcase.CheckResult;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Arrays;

public class FileShareTest extends SpringTest {
    private final String url = "http://localhost:" + this.port + "/api/v1/upload";

    CheckResult testPostAndGetFile(String filepath, String filename) {
        try {
            FileClient client = new FileClient();

            FileData fileData = filename.isBlank()
                    ? FileData.of(filepath)
                    : FileData.withNewName(filepath, filename);

            HttpResponse<byte[]> postResponse = client.post(url, fileData);

            if (postResponse.statusCode() != 201) {
                return CheckResult.wrong(
                        "Expected status code %d but was %d".formatted(201, postResponse.statusCode()));
            }

            String location = postResponse.headers().firstValue("Location")
                    .orElseThrow(() -> new WrongAnswer("Response should contain the 'Location' header."));

            if (location.isBlank()) {
                return CheckResult.wrong("The value of the 'Location' header should not be blank");
            }

            HttpResponse<byte[]> getResponse = client.get(location);

            if (!Arrays.equals(fileData.getContents(), getResponse.body())) {
                return CheckResult.wrong("""
                        When requested this file:
                        %s
                        the request body returned by your application does not match the expected file content
                        """.formatted(location));
            }

            return CheckResult.correct();
        } catch (IOException | InterruptedException e) {
            return CheckResult.wrong("Error occurred during the test execution: " + e.getMessage());
        }
    }

    @DynamicTest
    DynamicTesting[] dt = {
            () -> testPostAndGetFile("./test/file 1.jpg", ""),
            () -> testPostAndGetFile("./test/file2.jpg", "file 1.jpg"),
    };
}
