package namdh.dhbkhn.datn.service;

import java.util.List;
import java.util.Optional;
import namdh.dhbkhn.datn.domain.Classroom;
import namdh.dhbkhn.datn.domain.ClassroomStatus;
import namdh.dhbkhn.datn.domain.User;
import namdh.dhbkhn.datn.repository.ClassroomRepository;
import namdh.dhbkhn.datn.repository.ClassroomStatusRepository;
import namdh.dhbkhn.datn.repository.UserRepository;
import namdh.dhbkhn.datn.security.SecurityUtils;
import namdh.dhbkhn.datn.service.dto.classroom.ClassroomInputDTO;
import namdh.dhbkhn.datn.service.dto.classroom.ClassroomOutputDTO;
import namdh.dhbkhn.datn.service.dto.classroom.NumClassroomDTO;
import namdh.dhbkhn.datn.service.error.AccessForbiddenException;
import namdh.dhbkhn.datn.service.error.BadRequestException;
import namdh.dhbkhn.datn.service.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClassroomService {

    private final UserACL userACL;
    private final UserRepository userRepository;

    private final ClassroomRepository classroomRepository;

    private final ClassroomStatusRepository classroomStatusRepository;

    public ClassroomService(
        UserACL userACL,
        UserRepository userRepository,
        ClassroomRepository classroomRepository,
        ClassroomStatusRepository classroomStatusRepository
    ) {
        this.userACL = userACL;
        this.userRepository = userRepository;
        this.classroomRepository = classroomRepository;
        this.classroomStatusRepository = classroomStatusRepository;
    }

    public ClassroomOutputDTO create(ClassroomInputDTO classroomInputDTO) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        User user = Utils.requireExists(SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin), "error.userNotFound");
        Optional<Classroom> optional = classroomRepository.findByNameAndUserId(classroomInputDTO.getName(), user.getId());
        if (optional.isPresent()) {
            throw new BadRequestException("error.classroomNameExisted", null);
        }
        String name = classroomInputDTO.getName();
        if (Utils.isAllSpaces(name) || name.isEmpty()) {
            throw new BadRequestException("error.classroomNameEmptyOrBlank", null);
        }
        String maxSv = classroomInputDTO.getMaxSv();
        String regexMaxSv = "^[0-9-\\s]*$";
        if (Utils.isAllSpaces(maxSv) || maxSv.isEmpty()) {
            throw new BadRequestException("error.maxSvEmptyOrBlank", null);
        } else if (!maxSv.matches(regexMaxSv)) {
            throw new BadRequestException("error.maxSvInvalid", null);
        }
        Classroom classroom = new Classroom();
        classroom.setUser(user);
        classroom.setName(name);
        classroom.setMaxSv(Integer.parseInt(maxSv));
        classroomRepository.save(classroom);
        for (int i = 1; i < 54; i++) {
            ClassroomStatus classroomStatus = new ClassroomStatus();
            classroomStatus.setUser(user);
            classroomStatus.setClassroom(classroom);
            classroomStatus.setWeek(i);
            classroomStatusRepository.save(classroomStatus);
        }
        return new ClassroomOutputDTO(classroom);
    }

    public ClassroomOutputDTO update(ClassroomInputDTO classroomInputDTO, long id) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        ClassroomOutputDTO classroomOutputDTO;
        Classroom classroom = Utils.requireExists(classroomRepository.findById(id), "error.classroomNotFound");
        if (!userACL.canUpdate(classroom.getUser().getId())) {
            throw new AccessForbiddenException("error.notUserCreateClassroom");
        }
        String name = classroomInputDTO.getName();
        if (Utils.isAllSpaces(name) || name.isEmpty()) {
            throw new BadRequestException("error.classroomNameEmptyOrBlank", null);
        }
        String regexMaxSv = "^[0-9-\\s]*$";
        String maxSv = classroomInputDTO.getMaxSv();
        if (Utils.isAllSpaces(maxSv) || maxSv.isEmpty()) {
            throw new BadRequestException("error.maxSvEmptyOrBlank", null);
        } else if (!maxSv.matches(regexMaxSv)) {
            throw new BadRequestException("error.maxSvInvalid", null);
        }
        classroom.setName(name);
        classroom.setMaxSv(Integer.parseInt(maxSv));
        classroomOutputDTO = new ClassroomOutputDTO(classroom);
        return classroomOutputDTO;
    }

    public Page<ClassroomOutputDTO> getAll(Pageable pageable, String name) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        User user = Utils.requireExists(SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin), "error.userNotFound");
        Page<ClassroomOutputDTO> page;
        if (name == null) {
            page = classroomRepository.getAllByUserIdAndNameIsNotNull(pageable, user.getId()).map(ClassroomOutputDTO::new);
        } else {
            page =
                classroomRepository.findAllByNameContainingIgnoreCaseAndUserId(pageable, name, user.getId()).map(ClassroomOutputDTO::new);
        }
        return page;
    }

    public ClassroomOutputDTO getOne(long id) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        Classroom classroom = Utils.requireExists(classroomRepository.findById(id), "error.classroomNotFound");
        if (!userACL.canUpdate(classroom.getUser().getId())) {
            throw new AccessForbiddenException("error.notUserCreateClassroom");
        }
        return new ClassroomOutputDTO(classroom);
    }

    public NumClassroomDTO getNumbForAllClassroom() {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        User user = Utils.requireExists(SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin), "error.userNotFound");
        Long num = this.classroomRepository.countAllByUserId(user.getId());
        NumClassroomDTO result = new NumClassroomDTO();
        result.setNum(num);
        return result;
    }

    public void delete(long id) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        Classroom classroom = Utils.requireExists(classroomRepository.findById(id), "error.classroomNotFound");
        if (!userACL.canUpdate(classroom.getUser().getId())) {
            throw new AccessForbiddenException("error.notUserCreateClassroom");
        }
        List<ClassroomStatus> classroomStatuses = classroomStatusRepository.findAllByClassroomId(classroom.getId());
        classroomStatusRepository.deleteAll(classroomStatuses);
        classroomRepository.delete(classroom);
    }
}
