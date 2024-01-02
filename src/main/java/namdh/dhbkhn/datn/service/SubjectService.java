package namdh.dhbkhn.datn.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import namdh.dhbkhn.datn.domain.Classes;
import namdh.dhbkhn.datn.domain.User;
import namdh.dhbkhn.datn.repository.ClassesRepository;
import namdh.dhbkhn.datn.repository.UserRepository;
import namdh.dhbkhn.datn.security.SecurityUtils;
import namdh.dhbkhn.datn.service.dto.class_name.ClassesInputDTO;
import namdh.dhbkhn.datn.service.dto.class_name.ClassesOutputDTO;
import namdh.dhbkhn.datn.service.dto.class_name.NumClassesDTO;
import namdh.dhbkhn.datn.service.error.AccessForbiddenException;
import namdh.dhbkhn.datn.service.error.BadRequestException;
import namdh.dhbkhn.datn.service.utils.Utils;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClassesService {

    private static final Logger log = LoggerFactory.getLogger(ClassesService.class);

    private final UserACL userACL;
    private final UserRepository userRepository;
    private final ClassesRepository classesRepository;

    public ClassesService(UserACL userACL, UserRepository userRepository, ClassesRepository classesRepository) {
        this.userACL = userACL;
        this.userRepository = userRepository;
        this.classesRepository = classesRepository;
    }

    public void importClassList(InputStream inputStream) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        User user = Utils.requireExists(SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin), "error.userNotFound");
        List<ClassesOutputDTO> classNameList = readExcelFileClassList(inputStream);

        if (classNameList.size() > 0) {
            for (ClassesOutputDTO classesOutputDTO : classNameList) {
                Optional<Classes> optional = classesRepository.findByCourseCodeAndClassNoteAndUserId(
                    classesOutputDTO.getCourseCode(),
                    classesOutputDTO.getClassNote(),
                    user.getId()
                );
                if (!optional.isPresent()) {
                    Classes classes = new Classes(classesOutputDTO);
                    classes.setUser(user);
                    classesRepository.save(classes);
                }
            }
        }
    }

    private List<ClassesOutputDTO> readExcelFileClassList(InputStream inputStream) {
        List<ClassesOutputDTO> result = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0 || row.getRowNum() == 1) {
                    // Ignore header
                    continue;
                }

                // Read cells and set value for classes object
                ClassesOutputDTO classesOutputDTO = new ClassesOutputDTO();
                for (int i = 0; i < 9; i++) {
                    Cell cell = row.getCell(i);
                    if (cell == null && i != 1 && i != 3 && i != 7 && i != 8) {
                        throw new BadRequestException("error.fieldNullOrEmpty", (row.getRowNum() + 1) + "-" + (i + 1));
                    }
                    Object cellValue = null;
                    if (!((i == 1 || i == 3 || i == 7 || i == 8) && cell == null)) {
                        cellValue = getCellValue(cell);
                    }

                    // Set value for classes object
                    switch (i) {
                        case 0:
                            classesOutputDTO.setSemester(Utils.handleDoubleNumber(cellValue.toString()));
                            break;
                        case 1:
                            if (cellValue == null) {
                                classesOutputDTO.setName(null);
                                break;
                            }
                            classesOutputDTO.setName(Utils.handleWhitespace(cellValue.toString()));
                            break;
                        case 2:
                            classesOutputDTO.setClassNote(Utils.handleWhitespace(cellValue.toString()));
                            break;
                        case 3:
                            if (cellValue == null) {
                                classesOutputDTO.setCourseCode(null);
                                break;
                            }
                            classesOutputDTO.setCourseCode(Utils.handleWhitespace(cellValue.toString()));
                            break;
                        case 4:
                            classesOutputDTO.setStartWeek(Integer.parseInt(Utils.handleDoubleNumber(cellValue.toString())));
                            break;
                        case 5:
                            classesOutputDTO.setNumberOfLessons(Integer.parseInt(Utils.handleDoubleNumber(cellValue.toString())));
                            break;
                        case 6:
                            classesOutputDTO.setNumberOfWeekStudy(
                                Integer.parseInt(Utils.handleDoubleNumber(cellValue.toString())) / classesOutputDTO.getNumberOfLessons()
                            );
                            break;
                        case 7:
                            if (cellValue == null) {
                                classesOutputDTO.setDepartmentName(null);
                                break;
                            }
                            classesOutputDTO.setDepartmentName(Utils.handleWhitespace(cellValue.toString()));
                            break;
                        case 8:
                            if (cellValue == null) {
                                classesOutputDTO.setConditions(1);
                                break;
                            }
                            classesOutputDTO.setConditions(Integer.parseInt(Utils.handleDoubleNumber(cellValue.toString())));
                            break;
                    }
                }
                result.add(classesOutputDTO);
            }

            workbook.close();
            inputStream.close();

            return result;
        } catch (IOException e) {
            log.error("Error upload excel file wrong format", e);
            throw new BadRequestException("error.uploadExcelWrongFormat", null);
        }
    }

    private static Object getCellValue(Cell cell) {
        CellType cellType = cell.getCellType();
        Object cellValue = null;
        switch (cellType) {
            case BOOLEAN:
                cellValue = cell.getBooleanCellValue();
                break;
            case FORMULA:
                Workbook workbook = cell.getSheet().getWorkbook();
                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                cellValue = evaluator.evaluate(cell).getNumberValue();
                break;
            case NUMERIC:
                cellValue = cell.getNumericCellValue();
                break;
            case STRING:
                cellValue = cell.getStringCellValue();
                break;
            case _NONE:
            case BLANK:
                if (cell.getColumnIndex() == 1 || cell.getColumnIndex() == 3 || cell.getColumnIndex() == 7 || cell.getColumnIndex() == 8) {
                    break;
                }
            case ERROR:
                throw new BadRequestException("error.cellContentError", (cell.getRowIndex() + 1) + "-" + (cell.getColumnIndex() + 1));
            default:
                break;
        }
        return cellValue;
    }

    public ClassesOutputDTO create(ClassesInputDTO classesInputDTO) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        User user = Utils.requireExists(SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin), "error.userNotFound");
        Optional<Classes> optional = classesRepository.findByCourseCodeAndClassNoteAndUserId(
            classesInputDTO.getCourseCode(),
            classesInputDTO.getClassNote(),
            user.getId()
        );
        if (optional.isPresent()) {
            throw new BadRequestException("error.classesExisted", null);
        }
        String semester = classesInputDTO.getSemester();
        if (Utils.isAllSpaces(semester) || semester.isEmpty()) {
            throw new BadRequestException("error.semesterEmptyOrBlank", null);
        }
        String classNote = classesInputDTO.getClassNote();
        if (Utils.isAllSpaces(classNote) || classNote.isEmpty()) {
            throw new BadRequestException("error.classNoteEmptyOrBlank", null);
        }
        String courseCode = classesInputDTO.getCourseCode();
        if (Utils.isAllSpaces(courseCode) || courseCode.isEmpty()) {
            throw new BadRequestException("error.courseCodeEmptyOrBlank", null);
        }
        int startWeek = classesInputDTO.getStartWeek();
        if (startWeek < 1) {
            throw new BadRequestException("error.startWeekInvalid", null);
        }
        int numberOfLessons = classesInputDTO.getNumberOfLessons();
        if (numberOfLessons < 1) {
            throw new BadRequestException("error.numberOfLessonsInvalid", null);
        }
        int totalNumberOfWeekStudy = classesInputDTO.getTotalNumberOfLessons();
        if (totalNumberOfWeekStudy < 1 || totalNumberOfWeekStudy < numberOfLessons) {
            throw new BadRequestException("error.totalNumberOfWeekStudyInvalid", null);
        }
        Classes classes = new Classes();
        classes.setUser(user);
        classes.setName(classesInputDTO.getName());
        classes.setCourseCode(classesInputDTO.getCourseCode());
        classes.setClassNote(classNote);
        classes.setStartWeek(startWeek);
        classes.setNumberOfLessons(numberOfLessons);
        int numberOfWeekStudy = totalNumberOfWeekStudy / numberOfLessons;
        classes.setNumberOfWeekStudy(numberOfWeekStudy);
        classes.setSemester(semester);
        classes.setConditions(classesInputDTO.getConditions());
        classes.setDepartmentName(classesInputDTO.getDepartmentName());
        classesRepository.save(classes);
        return new ClassesOutputDTO(classes);
    }

    public ClassesOutputDTO update(ClassesInputDTO classesInputDTO, long id) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        ClassesOutputDTO classesOutputDTO;
        Classes classes = Utils.requireExists(classesRepository.findById(id), "error.classesNotFound");
        if (!userACL.canUpdate(classes.getUser().getId())) {
            throw new AccessForbiddenException("error.notUserCreateClass");
        }
        String className = classesInputDTO.getName();
        if (Utils.isAllSpaces(className) || className.isEmpty()) {
            throw new BadRequestException("error.classNameEmptyOrBlank", null);
        }
        classes.setName(className);
        String classNote = classesInputDTO.getClassNote();
        if (Utils.isAllSpaces(classNote) || classNote.isEmpty()) {
            throw new BadRequestException("error.classNoteEmptyOrBlank", null);
        }
        classes.setClassNote(classNote);
        String courseCode = classesInputDTO.getCourseCode();
        if (Utils.isAllSpaces(courseCode) || courseCode.isEmpty()) {
            throw new BadRequestException("error.courseCodeEmptyOrBlank", null);
        }
        classes.setCourseCode(courseCode);
        int startWeek = classesInputDTO.getStartWeek();
        if (startWeek < 1 || startWeek > 53) {
            throw new BadRequestException("error.startWeekInvalid", null);
        }
        classes.setStartWeek(startWeek);
        int numberOfLessons = classesInputDTO.getNumberOfLessons();
        if (numberOfLessons < 1 || numberOfLessons > 6) {
            throw new BadRequestException("error.numberOfLessonsInvalid", null);
        }
        classes.setNumberOfLessons(numberOfLessons);
        int totalNumberOfLessons = classesInputDTO.getTotalNumberOfLessons();
        if (totalNumberOfLessons < 1 || totalNumberOfLessons < numberOfLessons) {
            throw new BadRequestException("error.totalNumberOfLessonsInvalid", null);
        }
        int numberOfWeekStudy = totalNumberOfLessons / numberOfLessons;
        classes.setNumberOfWeekStudy(numberOfWeekStudy);
        String semester = classesInputDTO.getSemester();
        if (Utils.isAllSpaces(semester) || semester.isEmpty()) {
            throw new BadRequestException("error.semesterEmptyOrBlank", null);
        }
        classes.setSemester(semester);
        int conditions = classesInputDTO.getConditions();
        if (conditions < 1) {
            throw new BadRequestException("error.conditionsIncorrect", null);
        }
        classes.setConditions(conditions);
        classesOutputDTO = new ClassesOutputDTO(classes);
        return classesOutputDTO;
    }

    public Page<ClassesOutputDTO> getAll(Pageable pageable, String name) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        User user = Utils.requireExists(SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin), "error.userNotFound");
        Page<ClassesOutputDTO> page;
        if (name == null) {
            page = classesRepository.findAllByUserIdAndNameIsNotNull(pageable, user.getId()).map(ClassesOutputDTO::new);
        } else {
            page = classesRepository.findAllByNameContainingIgnoreCaseAndUserId(pageable, name, user.getId()).map(ClassesOutputDTO::new);
        }
        return page;
    }

    public ClassesOutputDTO getOne(long id) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        Classes classes = Utils.requireExists(classesRepository.findById(id), "error.classesNotFound");
        if (!userACL.canUpdate(classes.getUser().getId())) {
            throw new AccessForbiddenException("error.notUserCreateClass");
        }
        return new ClassesOutputDTO(classes);
    }

    public NumClassesDTO getNumbForAllClasses() {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        User user = Utils.requireExists(SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin), "error.userNotFound");
        Long num = this.classesRepository.countAllByUserId(user.getId());
        NumClassesDTO result = new NumClassesDTO();
        result.setNum(num);
        return result;
    }

    public void delete(long id) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        Classes classes = Utils.requireExists(classesRepository.findById(id), "error.classesNotFound");
        if (!userACL.canUpdate(classes.getUser().getId())) {
            throw new AccessForbiddenException("error.notUserCreateClass");
        }
        classesRepository.delete(classes);
    }
}
