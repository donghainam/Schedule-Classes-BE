package namdh.dhbkhn.datn.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.mail.search.SubjectTerm;
import namdh.dhbkhn.datn.domain.Classroom;
import namdh.dhbkhn.datn.domain.ClassroomStatus;
import namdh.dhbkhn.datn.domain.Subject;
import namdh.dhbkhn.datn.domain.User;
import namdh.dhbkhn.datn.repository.ClassroomRepository;
import namdh.dhbkhn.datn.repository.ClassroomStatusRepository;
import namdh.dhbkhn.datn.repository.SubjectRepository;
import namdh.dhbkhn.datn.repository.UserRepository;
import namdh.dhbkhn.datn.security.SecurityUtils;
import namdh.dhbkhn.datn.service.dto.schedule.ScheduleDTO;
import namdh.dhbkhn.datn.service.error.AccessForbiddenException;
import namdh.dhbkhn.datn.service.error.BadRequestException;
import namdh.dhbkhn.datn.service.utils.Utils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

    private final UserACL userACL;

    private final UserRepository userRepository;

    private final SubjectRepository subjectRepository;

    private final ClassroomStatusRepository classroomStatusRepository;

    public ScheduleService(
        UserACL userACL,
        UserRepository userRepository,
        SubjectRepository subjectRepository,
        ClassroomStatusRepository classroomStatusRepository
    ) {
        this.userACL = userACL;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.classroomStatusRepository = classroomStatusRepository;
    }

    public byte[] exportSchedule(String semester, int numOfDayPerWeek) {
        if (!userACL.isUser()) {
            throw new AccessForbiddenException("error.notUser");
        }
        if (Utils.isAllSpaces(semester) || semester.isEmpty()) {
            throw new BadRequestException("error.semesterEmptyOrBlank", null);
        } else if (semester.length() != 5) {
            throw new BadRequestException("error.semesterInvalid", null);
        }
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Schedule");

            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setFontHeightInPoints((short) 13);
            style.setFont(font);

            Row rowHead = sheet.createRow(0);
            rowHead.createCell(0).setCellValue("CourseID");
            rowHead.createCell(1).setCellValue("CourseName");
            rowHead.createCell(2).setCellValue("Note");
            rowHead.createCell(3).setCellValue("WeekNote");
            rowHead.createCell(4).setCellValue("WeekDay");
            rowHead.createCell(5).setCellValue("Begin");
            rowHead.createCell(6).setCellValue("End");
            rowHead.createCell(7).setCellValue("Room");
            rowHead.createCell(8).setCellValue("DepartmentName");
            rowHead.createCell(9).setCellValue("MAX");

            for (int j = 0; j < 10; j++) {
                rowHead.getCell(j).setCellStyle(style);
            }

            List<ScheduleDTO> scheduleDTOS = this.getSchedule(semester, numOfDayPerWeek);
            this.writeToSheet(sheet, scheduleDTOS, 1);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            log.error("Error when create Excel file: ", e);
        }
        return new byte[0];
    }

    public List<ScheduleDTO> getSchedule(String semester, int numOfDayPerWeek) {
        List<ScheduleDTO> result = new ArrayList<>();
        User user = Utils.requireExists(SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin), "error.userNotFound");

        // Handle old data
        List<ClassroomStatus> classroomStatuses = classroomStatusRepository.findAll();
        for (ClassroomStatus classroomStatus : classroomStatuses) {
            classroomStatus.setTimeNote(null);
            classroomStatus.setStatus(0);
        }
        this.classroomStatusRepository.saveAll(classroomStatuses);
        List<Subject> clList = subjectRepository.getClassesBySemester(semester);
        for (Subject subject : clList) {
            subject.setCountWeekStudied(0);
        }
        this.subjectRepository.saveAll(clList);

        // Get start, end week for semester

        String st = semester.substring(4);
        int start;
        int end;
        if (st.equals("1")) {
            start = 1;
            end = 21;
        } else {
            start = 25;
            end = 45;
        }
        for (int i = start; i < end; i++) {
            // Get all Classes
            List<Subject> classesList = subjectRepository.getAllClasses(i, semester, user.getId());
            // Sort by priority
            this.sortByPriority(classesList);

            for (Subject classes : classesList) {
                boolean containsWeekOff = checkWeekOff(classes.getWeekOff(), i);
                if (containsWeekOff) {
                    continue;
                }
                int status;
                if (classes.getNumberOfLessons() > 3) {
                    status = 0;
                } else {
                    status = 1;
                }
                // Get one classroom from db
                ClassroomStatus classroomStatus = classroomStatusRepository.getClassroomStatusByWeekAndStatusAndUserId(
                    i,
                    status,
                    user.getId()
                );
                if (classroomStatus == null) {
                    continue;
                }
                Classroom classroom = classroomStatus.getClassroom();
                int[][] w = new int[numOfDayPerWeek][2];
                if (classroomStatus.getTimeNote() != null) {
                    this.initArray(w, numOfDayPerWeek);
                    this.getTimeNote(w, classroomStatus, numOfDayPerWeek);
                } else {
                    this.initArray(w, numOfDayPerWeek);
                }
                if (checkClassroom(w, classes.getNumberOfLessons(), numOfDayPerWeek)) {
                    this.scheduleClasses(classes, classroom, w, result, i, numOfDayPerWeek);
                } else {
                    log.info("Classrooms aren't enough for all classes");
                    throw new BadRequestException("error.classroomNotEnough", null);
                }
            }
        }
        return result;
    }

    private void scheduleClasses(Subject subject, Classroom classroom, int[][] w, List<ScheduleDTO> list, int week, int numOfDayPerWeek) {
        for (int i = 0; i < numOfDayPerWeek; i++) {
            for (int j = 0; j < 2; j++) {
                if (w[i][j] >= subject.getNumberOfLessons()) {
                    String begin = this.getBegin(w[i][j] + 10 * j);
                    String end = this.getEnd(w[i][j] + 10 * j, subject.getNumberOfLessons());
                    ClassroomStatus classroomStatus = Utils.requireExists(
                        classroomStatusRepository.findByClassroomIdAndWeek(classroom.getId(), week),
                        "error.classroomWeekNotFound"
                    );
                    List<String> beginList = classroomStatus.getTimeNoteExtra(String.valueOf(i) + 0);
                    List<String> endList = classroomStatus.getTimeNoteExtra(String.valueOf(i) + 1);
                    String sessionStr = String.valueOf(j + 1);
                    if (beginList != null && endList != null) {
                        for (int n = 0; n < beginList.size(); n++) {
                            int session = getSession(endList.get(n));
                            if (session == j && subject.getNumberOfLessons() == 3) {
                                String oldBegin = beginList.get(n);
                                if (session == 0) {
                                    if (oldBegin.equals("0645")) {
                                        begin = "0920";
                                        end = "1145";
                                    } else if (oldBegin.equals("0920")) {
                                        begin = "0645";
                                        end = "0910";
                                    }
                                } else {
                                    if (oldBegin.equals("1230")) {
                                        begin = "1505";
                                        end = "1730";
                                    } else if (oldBegin.equals("1505")) {
                                        begin = "1230";
                                        end = "1455";
                                    }
                                }
                            }
                        }
                    }
                    List<Integer> weekNote =
                        this.scheduleTheClassWeek(subject, classroom.getId(), week, begin, end, i, w[i][j], numOfDayPerWeek);

                    ScheduleDTO scheduleDTO = new ScheduleDTO();
                    scheduleDTO.setCourseCode(subject.getCourseCode());
                    scheduleDTO.setClassName(subject.getName());
                    scheduleDTO.setClassNote(subject.getClassNote());
                    scheduleDTO.setClassroom(classroom.getName());
                    scheduleDTO.setDepartmentName(subject.getDepartmentName());
                    scheduleDTO.setMaxSv(classroom.getMaxSv());

                    // TimeNote
                    scheduleDTO.addTimeNote("WeekNote", weekNote);
                    scheduleDTO.addTimeNote("WeekDay", i + 2);
                    scheduleDTO.addTimeNote("Begin", sessionStr + begin);
                    scheduleDTO.addTimeNote("End", sessionStr + end);
                    list.add(scheduleDTO);
                    w[i][j] -= subject.getNumberOfLessons();
                    return;
                }
            }
        }
    }

    private List<Integer> scheduleTheClassWeek(
        Subject subject,
        long classroomId,
        int weekFirst,
        String begin,
        String end,
        int weekDay,
        int remainingLessons,
        int numOfDayPerWeek
    ) {
        List<Integer> result = new ArrayList<>();
        int cnt = subject.getCountWeekStudied();
        int lastWeek;
        if (weekFirst > 20) {
            lastWeek = 46;
        } else {
            lastWeek = 23;
        }
        int session = this.getSession(end);
        while (cnt < subject.getNumberOfWeekStudy() && weekFirst < lastWeek) {
            boolean containsWeekOff = checkWeekOff(subject.getWeekOff(), weekFirst);
            if (containsWeekOff) {
                weekFirst++;
                continue;
            }
            ClassroomStatus classroomStatus = Utils.requireExists(
                classroomStatusRepository.findByClassroomIdAndWeek(classroomId, weekFirst),
                "error.classroomWeekNotFound"
            );
            // Get data of classroom
            int[][] w = new int[numOfDayPerWeek][2];
            if (classroomStatus.getTimeNote() != null) {
                this.initArray(w, numOfDayPerWeek);
                this.getTimeNote(w, classroomStatus, numOfDayPerWeek);
            } else {
                this.initArray(w, numOfDayPerWeek);
            }

            List<String> beginList = classroomStatus.getTimeNoteExtra(String.valueOf(weekDay) + 0);
            if (beginList == null) {
                beginList = new ArrayList<>();
            }
            List<String> endList = classroomStatus.getTimeNoteExtra(String.valueOf(weekDay) + 1);
            if (endList == null) {
                endList = new ArrayList<>();
            }

            // Check week and save
            if (w[weekDay][session] >= remainingLessons && !beginList.contains(begin) && !endList.contains(end)) {
                result.add(weekFirst);
                beginList.add(begin);
                endList.add(end);
                classroomStatus.addTimeNote(String.valueOf(weekDay) + 0, beginList);
                classroomStatus.addTimeNote(String.valueOf(weekDay) + 1, endList);
                // Handle condition classroom
                w[weekDay][session] -= subject.getNumberOfLessons();
                if (!checkClassroom(w, 3, numOfDayPerWeek)) {
                    classroomStatus.setStatus(2);
                } else if (!checkClassroom(w, 5, numOfDayPerWeek)) {
                    classroomStatus.setStatus(1);
                }
                classroomStatusRepository.save(classroomStatus);
                weekFirst += subject.getConditions();
                cnt++;
            } else {
                weekFirst++;
            }
        }
        subject.setCountWeekStudied(cnt);
        subjectRepository.save(subject);
        return result;
    }

    public void writeToSheet(Sheet sheet, List<ScheduleDTO> scheduleDTOS, int line) {
        for (ScheduleDTO scheduleDTO : scheduleDTOS) {
            Row row = sheet.createRow(line++);
            row.createCell(0).setCellValue(scheduleDTO.getCourseCode());
            row.createCell(1).setCellValue(scheduleDTO.getClassName());
            row.createCell(2).setCellValue(scheduleDTO.getClassNote());
            List<Integer> weekNote = scheduleDTO.getTimeNoteExtra("WeekNote");
            String weekNoteSt = weekNote.stream().map(String::valueOf).collect(Collectors.joining(","));
            row.createCell(3).setCellValue(weekNoteSt);
            int weekDay = scheduleDTO.getTimeNoteExtra("WeekDay");
            row.createCell(4).setCellValue(String.valueOf(weekDay));
            row.createCell(5).setCellValue((String) scheduleDTO.getTimeNoteExtra("Begin"));
            row.createCell(6).setCellValue((String) scheduleDTO.getTimeNoteExtra("End"));
            row.createCell(7).setCellValue(scheduleDTO.getClassroom());
            row.createCell(8).setCellValue(scheduleDTO.getDepartmentName());
            row.createCell(9).setCellValue(scheduleDTO.getMaxSv());
        }
    }

    private void sortByPriority(List<Subject> input) {
        input.sort((cls1, cls2) -> {
            // Sort by startWeek in ascending order
            int startWeekComparison = Integer.compare(cls1.getStartWeek(), cls2.getStartWeek());
            // If startWeeks are equal, sort by numberOfWeekStudy in descending order
            if (startWeekComparison == 0) {
                int numberOfWeekStudyComparison = Integer.compare(cls2.getNumberOfWeekStudy(), cls1.getNumberOfWeekStudy());
                // If startWeeks are equal, sort by numberOfLesson in descending order
                if (numberOfWeekStudyComparison == 0) {
                    return Integer.compare(cls2.getNumberOfLessons(), cls1.getNumberOfLessons());
                }
                return numberOfWeekStudyComparison;
            }
            return startWeekComparison;
        });
    }

    private void getTimeNote(int[][] w, ClassroomStatus classroomStatus, int numOfDayPerWeek) {
        for (int i = 0; i < numOfDayPerWeek; i++) {
            List<String> beginList = classroomStatus.getTimeNoteExtra(String.valueOf(i) + 0);
            List<String> endList = classroomStatus.getTimeNoteExtra(String.valueOf(i) + 1);
            if (endList != null && beginList != null) {
                for (int j = 0; j < beginList.size(); j++) {
                    String begin = beginList.get(j);
                    String end = endList.get(j);
                    int session = this.getSession(end);
                    int sub = Integer.parseInt(end) - Integer.parseInt(begin);
                    int timeHaveStudied = getTimeHaveStudied(sub);
                    w[i][session] -= timeHaveStudied;
                }
            }
        }
    }

    private void initArray(int[][] w, int numOfDayPerWeek) {
        for (int i = 0; i < numOfDayPerWeek; i++) {
            for (int j = 0; j < 2; j++) {
                w[i][j] = 6;
            }
        }
    }

    private boolean checkClassroom(int[][] w, int numberOfLesson, int numOfDayPerWeek) {
        for (int i = 0; i < numOfDayPerWeek; i++) {
            for (int j = 0; j < 2; j++) {
                if (w[i][j] >= numberOfLesson) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getBegin(int w) {
        String result;
        switch (w) {
            case 6:
                result = "0645";
                break;
            case 5:
                result = "0730";
                break;
            case 4:
                result = "0825";
                break;
            case 3:
                result = "0920";
                break;
            case 2:
                result = "1015";
                break;
            case 1:
                result = "1100";
                break;
            case 16:
                result = "1230";
                break;
            case 15:
                result = "1315";
                break;
            case 14:
                result = "1410";
                break;
            case 13:
                result = "1505";
                break;
            case 12:
                result = "1600";
                break;
            default:
                result = "1645";
        }
        return result;
    }

    private String getEnd(int w, int numberOfLesson) {
        String result;
        switch (w - numberOfLesson) {
            case 0:
                result = "1145";
                break;
            case 1:
                result = "1100";
                break;
            case 2:
                result = "1005";
                break;
            case 3:
                result = "0910";
                break;
            case 4:
                result = "0815";
                break;
            case 5:
                result = "0730";
                break;
            case 10:
                result = "1730";
                break;
            case 11:
                result = "1645";
                break;
            case 12:
                result = "1550";
                break;
            case 13:
                result = "1455";
                break;
            case 14:
                result = "1400";
                break;
            default:
                result = "1315";
        }
        return result;
    }

    private int getSession(String end) {
        int session;
        switch (end) {
            case "0730":
            case "0815":
            case "0910":
            case "1005":
            case "1100":
            case "1145":
                session = 0;
                break;
            default:
                session = 1;
        }
        return session;
    }

    private int getRemainingWOfSession(String end) {
        int w;
        switch (end) {
            case "1145":
            case "1730":
                w = 0;
                break;
            case "1100":
            case "1645":
                w = 1;
                break;
            case "1005":
            case "1550":
                w = 2;
                break;
            case "0910":
            case "1455":
                w = 3;
                break;
            case "0815":
            case "1400":
                w = 4;
                break;
            case "0730":
            case "1315":
                w = 5;
                break;
            default:
                w = 6;
        }
        return w;
    }

    private int getTimeHaveStudied(int sub) {
        int num;
        switch (sub) {
            case 45:
            case 85:
                num = 1;
                break;
            case 130:
            case 140:
            case 170:
            case 180:
                num = 2;
                break;
            case 265:
            case 275:
            case 225:
            case 235:
                num = 3;
                break;
            case 360:
            case 370:
            case 320:
            case 330:
                num = 4;
                break;
            default:
                num = 5;
        }
        return num;
    }

    private boolean checkWeekOff(String weekOff, int thisWeek) {
        boolean containsWeek = false;
        String[] listWeek = weekOff.split(",");
        for (String str : listWeek) {
            int intValue = Integer.parseInt(str);
            if (intValue == thisWeek) {
                containsWeek = true;
            }
        }
        return containsWeek;
    }
}
