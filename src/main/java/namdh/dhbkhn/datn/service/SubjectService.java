package namdh.dhbkhn.datn.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import namdh.dhbkhn.datn.domain.Subject;
import namdh.dhbkhn.datn.domain.User;
import namdh.dhbkhn.datn.repository.SubjectRepository;
import namdh.dhbkhn.datn.repository.UserRepository;
import namdh.dhbkhn.datn.security.SecurityUtils;
import namdh.dhbkhn.datn.service.dto.subject.NumSubjectDTO;
import namdh.dhbkhn.datn.service.dto.subject.SubjectInputDTO;
import namdh.dhbkhn.datn.service.dto.subject.SubjectOutputDTO;
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
public class SubjectService {

    private static final Logger log = LoggerFactory.getLogger(SubjectService.class);

    private final UserACL userACL;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;

    public SubjectService(UserACL userACL, UserRepository userRepository, SubjectRepository subjectRepository) {
        this.userACL = userACL;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
    }

    public void importClassList(InputStream inputStream) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        User user = Utils.requireExists(SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin), "error.userNotFound");
        List<SubjectOutputDTO> classNameList = readExcelFileClassList(inputStream);

        if (classNameList.size() > 0) {
            for (SubjectOutputDTO classesOutputDTO : classNameList) {
                Optional<Subject> optional = subjectRepository.findByCourseCodeAndClassNoteAndUserId(
                    classesOutputDTO.getCourseCode(),
                    classesOutputDTO.getClassNote(),
                    user.getId()
                );
                if (optional.isEmpty()) {
                    Subject classes = new Subject(classesOutputDTO);
                    classes.setUser(user);
                    subjectRepository.save(classes);
                }
            }
        }
    }

    private List<SubjectOutputDTO> readExcelFileClassList(InputStream inputStream) {
        List<SubjectOutputDTO> result = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0 || row.getRowNum() == 1) {
                    // Ignore header
                    continue;
                }

                // Read cells and set value for classes object
                SubjectOutputDTO subjectOutputDTO = new SubjectOutputDTO();
                for (int i = 0; i < 10; i++) {
                    Cell cell = row.getCell(i);
                    if (cell == null && i != 1 && i != 3 && i != 7 && i != 8 && i != 9) {
                        throw new BadRequestException("error.fieldNullOrEmpty", (row.getRowNum() + 1) + "-" + (i + 1));
                    }
                    Object cellValue = null;
                    if (!((i == 1 || i == 3 || i == 7 || i == 8 || i == 9) && cell == null)) {
                        cellValue = getCellValue(cell);
                    }

                    // Set value for classes object
                    switch (i) {
                        case 0:
                            subjectOutputDTO.setSemester(Utils.handleDoubleNumber(cellValue.toString()));
                            break;
                        case 1:
                            if (cellValue == null) {
                                subjectOutputDTO.setName(null);
                                break;
                            }
                            subjectOutputDTO.setName(Utils.handleWhitespace(cellValue.toString()));
                            break;
                        case 2:
                            subjectOutputDTO.setClassNote(Utils.handleWhitespace(cellValue.toString()));
                            break;
                        case 3:
                            if (cellValue == null) {
                                subjectOutputDTO.setCourseCode(null);
                                break;
                            }
                            subjectOutputDTO.setCourseCode(Utils.handleWhitespace(cellValue.toString()));
                            break;
                        case 4:
                            subjectOutputDTO.setStartWeek(Integer.parseInt(Utils.handleDoubleNumber(cellValue.toString())));
                            break;
                        case 5:
                            subjectOutputDTO.setNumberOfLessons(Integer.parseInt(Utils.handleDoubleNumber(cellValue.toString())));
                            break;
                        case 6:
                            subjectOutputDTO.setNumberOfWeekStudy(
                                Integer.parseInt(Utils.handleDoubleNumber(cellValue.toString())) / subjectOutputDTO.getNumberOfLessons()
                            );
                            break;
                        case 7:
                            if (cellValue == null) {
                                subjectOutputDTO.setDepartmentName(null);
                                break;
                            }
                            subjectOutputDTO.setDepartmentName(Utils.handleWhitespace(cellValue.toString()));
                            break;
                        case 8:
                            if (cellValue == null) {
                                subjectOutputDTO.setConditions(1);
                                break;
                            }
                            subjectOutputDTO.setConditions(Integer.parseInt(Utils.handleDoubleNumber(cellValue.toString())));
                            break;
                        case 9:
                            if (cellValue == null) {
                                subjectOutputDTO.setWeekOff(null);
                                break;
                            }
                            String stringWeek = Utils.handleWhitespace(cellValue.toString());
                            String[] listWeek = stringWeek.split(",");
                            StringBuilder stringWeekOff = new StringBuilder();
                            if (listWeek.length == 1) {
                                stringWeekOff.append(Integer.parseInt(Utils.handleDoubleNumber(listWeek[0])));
                            } else {
                                for (String s : listWeek) {
                                    stringWeekOff.append(Integer.parseInt(s)).append(",");
                                }
                                if (stringWeekOff.length() > 0) {
                                    stringWeekOff.deleteCharAt(stringWeekOff.length() - 1);
                                }
                            }
                            subjectOutputDTO.setWeekOff(stringWeekOff.toString());
                            break;
                    }
                }
                result.add(subjectOutputDTO);
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

    public SubjectOutputDTO create(SubjectInputDTO subjectInputDTO) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        User user = Utils.requireExists(SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin), "error.userNotFound");
        Optional<Subject> optional = subjectRepository.findByCourseCodeAndClassNoteAndUserId(
            subjectInputDTO.getCourseCode(),
            subjectInputDTO.getClassNote(),
            user.getId()
        );
        if (optional.isPresent()) {
            throw new BadRequestException("error.classesExisted", null);
        }
        String semester = subjectInputDTO.getSemester();
        if (Utils.isAllSpaces(semester) || semester.isEmpty()) {
            throw new BadRequestException("error.semesterEmptyOrBlank", null);
        }
        String classNote = subjectInputDTO.getClassNote();
        if (Utils.isAllSpaces(classNote) || classNote.isEmpty()) {
            throw new BadRequestException("error.classNoteEmptyOrBlank", null);
        }
        String courseCode = subjectInputDTO.getCourseCode();
        if (Utils.isAllSpaces(courseCode) || courseCode.isEmpty()) {
            throw new BadRequestException("error.courseCodeEmptyOrBlank", null);
        }
        int startWeek = subjectInputDTO.getStartWeek();
        if (startWeek < 1) {
            throw new BadRequestException("error.startWeekInvalid", null);
        }
        int numberOfLessons = subjectInputDTO.getNumberOfLessons();
        if (numberOfLessons < 1) {
            throw new BadRequestException("error.numberOfLessonsInvalid", null);
        }
        int totalNumberOfWeekStudy = subjectInputDTO.getTotalNumberOfLessons();
        if (totalNumberOfWeekStudy < 1 || totalNumberOfWeekStudy < numberOfLessons) {
            throw new BadRequestException("error.totalNumberOfWeekStudyInvalid", null);
        }
        Subject classes = new Subject();
        classes.setUser(user);
        classes.setName(subjectInputDTO.getName());
        classes.setCourseCode(subjectInputDTO.getCourseCode());
        classes.setClassNote(classNote);
        classes.setStartWeek(startWeek);
        classes.setNumberOfLessons(numberOfLessons);
        int numberOfWeekStudy = totalNumberOfWeekStudy / numberOfLessons;
        classes.setNumberOfWeekStudy(numberOfWeekStudy);
        classes.setSemester(semester);
        classes.setConditions(subjectInputDTO.getConditions());
        classes.setDepartmentName(subjectInputDTO.getDepartmentName());
        classes.setWeekOff(subjectInputDTO.getWeekOff());
        subjectRepository.save(classes);
        return new SubjectOutputDTO(classes);
    }

    public SubjectOutputDTO update(SubjectInputDTO subjectInputDTO, long id) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        SubjectOutputDTO classesOutputDTO;
        Subject classes = Utils.requireExists(subjectRepository.findById(id), "error.classesNotFound");
        if (!userACL.canUpdate(classes.getUser().getId())) {
            throw new AccessForbiddenException("error.notUserCreateClass");
        }
        User user = Utils.requireExists(SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin), "error.userNotFound");
        Optional<Subject> optional = subjectRepository.findByCourseCodeAndClassNoteAndUserId(
            subjectInputDTO.getCourseCode(),
            subjectInputDTO.getClassNote(),
            user.getId()
        );
        if (optional.isPresent()) {
            throw new BadRequestException("error.classesExisted", null);
        }
        String className = subjectInputDTO.getName();
        if (Utils.isAllSpaces(className) || className.isEmpty()) {
            throw new BadRequestException("error.classNameEmptyOrBlank", null);
        }
        classes.setName(className);
        String classNote = subjectInputDTO.getClassNote();
        if (Utils.isAllSpaces(classNote) || classNote.isEmpty()) {
            throw new BadRequestException("error.classNoteEmptyOrBlank", null);
        }
        classes.setClassNote(classNote);
        String courseCode = subjectInputDTO.getCourseCode();
        if (Utils.isAllSpaces(courseCode) || courseCode.isEmpty()) {
            throw new BadRequestException("error.courseCodeEmptyOrBlank", null);
        }
        classes.setCourseCode(courseCode);
        int startWeek = subjectInputDTO.getStartWeek();
        if (startWeek < 1 || startWeek > 53) {
            throw new BadRequestException("error.startWeekInvalid", null);
        }
        classes.setStartWeek(startWeek);
        int numberOfLessons = subjectInputDTO.getNumberOfLessons();
        if (numberOfLessons < 1 || numberOfLessons > 6) {
            throw new BadRequestException("error.numberOfLessonsInvalid", null);
        }
        classes.setNumberOfLessons(numberOfLessons);
        int totalNumberOfLessons = subjectInputDTO.getTotalNumberOfLessons();
        if (totalNumberOfLessons < 1 || totalNumberOfLessons < numberOfLessons) {
            throw new BadRequestException("error.totalNumberOfLessonsInvalid", null);
        }
        int numberOfWeekStudy = totalNumberOfLessons / numberOfLessons;
        classes.setNumberOfWeekStudy(numberOfWeekStudy);
        String semester = subjectInputDTO.getSemester();
        if (Utils.isAllSpaces(semester) || semester.isEmpty()) {
            throw new BadRequestException("error.semesterEmptyOrBlank", null);
        }
        classes.setSemester(semester);
        int conditions = subjectInputDTO.getConditions();
        if (conditions < 1) {
            throw new BadRequestException("error.conditionsIncorrect", null);
        }
        classes.setConditions(conditions);
        classes.setDepartmentName(subjectInputDTO.getDepartmentName());
        classes.setWeekOff(subjectInputDTO.getWeekOff());
        subjectRepository.save(classes);
        classesOutputDTO = new SubjectOutputDTO(classes);
        return classesOutputDTO;
    }

    public Page<SubjectOutputDTO> getAll(Pageable pageable, String name) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        User user = Utils.requireExists(SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin), "error.userNotFound");
        Page<SubjectOutputDTO> page;
        if (name == null) {
            page = subjectRepository.findAllByUserIdAndNameIsNotNull(pageable, user.getId()).map(SubjectOutputDTO::new);
        } else {
            page = subjectRepository.findAllByNameContainingIgnoreCaseAndUserId(pageable, name, user.getId()).map(SubjectOutputDTO::new);
        }
        return page;
    }

    public SubjectOutputDTO getOne(long id) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        Subject subject = Utils.requireExists(subjectRepository.findById(id), "error.classesNotFound");
        if (!userACL.canUpdate(subject.getUser().getId())) {
            throw new AccessForbiddenException("error.notUserCreateClass");
        }
        return new SubjectOutputDTO(subject);
    }

    public NumSubjectDTO getNumbForAllSubject() {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        User user = Utils.requireExists(SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin), "error.userNotFound");
        Long num = this.subjectRepository.countAllByUserId(user.getId());
        NumSubjectDTO result = new NumSubjectDTO();
        result.setNum(num);
        return result;
    }

    public void delete(long id) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        Subject classes = Utils.requireExists(subjectRepository.findById(id), "error.classesNotFound");
        if (!userACL.canUpdate(classes.getUser().getId())) {
            throw new AccessForbiddenException("error.notUserCreateClass");
        }
        subjectRepository.delete(classes);
    }
}
