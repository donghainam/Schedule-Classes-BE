package namdh.dhbkhn.datn.service.error;

import javax.validation.ConstraintDeclarationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends ConstraintDeclarationException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
