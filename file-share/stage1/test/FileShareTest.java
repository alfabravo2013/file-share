import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.mocks.web.response.HttpResponse;
import org.hyperskill.hstest.stage.SpringTest;
import org.hyperskill.hstest.testcase.CheckResult;

public class FileShareTest extends SpringTest {
    @DynamicTest
    CheckResult test() {
        HttpResponse response = get("/").send();
        if (response.getStatusCode() == 404) {
            return CheckResult.correct();
        }
        return CheckResult.wrong(
                "Expected status code %d but was %d"
                        .formatted(404, response.getStatusCode())
        );
    }
}
