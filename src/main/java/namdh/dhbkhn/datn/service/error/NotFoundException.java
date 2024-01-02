package namdh.dhbkhn.datn.service.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.StatusType;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class NotFoundException extends AbstractThrowableProblem {

    public NotFoundException(String key, String param) {
        super(
            null,
            key,
            new StatusType() {
                @Override
                public int getStatusCode() {
                    return 404;
                }

                @Override
                public String getReasonPhrase() {
                    return null;
                }
            },
            param
        );
    }
}
