package namdh.dhbkhn.datn.service;

import namdh.dhbkhn.datn.domain.User;
import namdh.dhbkhn.datn.repository.UserRepository;
import namdh.dhbkhn.datn.security.AuthoritiesConstants;
import namdh.dhbkhn.datn.security.SecurityUtils;
import namdh.dhbkhn.datn.service.utils.Utils;
import org.springframework.stereotype.Service;

@Service
public class UserACL {

    private final UserRepository userRepository;

    public UserACL(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isUser() {
        return SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.USER);
    }

    public boolean canUpdate(Long userId) {
        User user = SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneWithAuthoritiesByLogin).get();
        User userInput = Utils.requireExists(userRepository.findById(userId), "error.userNotFound");
        return user.getId().equals(userInput.getId());
    }
}
