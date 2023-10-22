import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.dynamic.input.DynamicTesting;
import org.hyperskill.hstest.exception.outcomes.UnexpectedError;
import org.hyperskill.hstest.exception.outcomes.WrongAnswer;
import org.hyperskill.hstest.stage.SpringTest;
import org.hyperskill.hstest.testcase.CheckResult;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

import static org.hyperskill.hstest.testing.expect.Expectation.expect;
import static org.hyperskill.hstest.testing.expect.json.JsonChecker.isObject;

public class FileShareTest extends SpringTest {
    private final String uploadUrl = "http://localhost:" + this.port + "/api/v1/upload";
    private final String infoUrl = "/api/v1/info";
    private final Path storagePath = Path.of("../uploads");

    CheckResult emptyStorageAndCheckInfo() {
        clearStorage();
        var response = get(infoUrl).send();
        if (response.getStatusCode() != 200) {
            return CheckResult.wrong("""
                    GET %s should respond with status code 200, responded with %d
                                        
                    Response body:
                    %s
                    """.formatted(infoUrl, response.getStatusCode(), response.getContent()));
        }

        try {
            response.getJson();
        } catch (Exception e) {
            return CheckResult.wrong("GET %s should return a valid JSON".formatted(infoUrl));
        }

        expect(response.getContent()).asJson().check(isObject()
                .value("total_files", 0)
                .value("total_bytes", 0)
        );

        return CheckResult.correct();
    }

    CheckResult testInfo(int count, long size) {
        var response = get(infoUrl).send();
        if (response.getStatusCode() != 200) {
            return CheckResult.wrong("""
                    GET %s should respond with status code 200, responded with %d
                                        
                    Response body:
                    %s
                    """.formatted(infoUrl, response.getStatusCode(), response.getContent()));
        }

        try {
            response.getJson();
        } catch (Exception e) {
            return CheckResult.wrong("GET %s should return a valid JSON".formatted(infoUrl));
        }

        expect(response.getContent()).asJson().check(isObject()
                .value("total_files", count)
                .value("total_bytes", size)
        );

        return CheckResult.correct();
    }

    CheckResult testPostAndGetFile(String filepath, String filename) {
        try {
            FileClient client = new FileClient();

            FileData fileData = FileData.withNewName(filepath, filename);

            HttpResponse<byte[]> postResponse = client.post(uploadUrl, fileData);

            if (postResponse.statusCode() != 201) {
                return CheckResult.wrong(
                        "Expected status code %d but was %d".formatted(201, postResponse.statusCode()));
            }

            return CheckResult.correct();
        } catch (IOException | InterruptedException e) {
            return CheckResult.wrong("Error occurred during the test execution: " + e.getMessage());
        }
    }

    @DynamicTest
    DynamicTesting[] dt = {
            this::emptyStorageAndCheckInfo,
            () -> testPostAndGetFile("./test/files/file 1.jpg", "file 1.jpg"),
            () -> testPostAndGetFile("./test/files/file2.jpg", "file 1.jpg"),
            () -> testPostAndGetFile("./test/files/hello", "file2.exe"),
            () -> testInfo(2, 47086),
    };

    private void clearStorage() {
        try (var stream = Files.walk(storagePath)) {
            stream
                    .sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(storagePath))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new UnexpectedError(e.getMessage());
                        }
                    });
        } catch (Exception ex) {
            throw new UnexpectedError(ex.getMessage());
        }
    }
}
