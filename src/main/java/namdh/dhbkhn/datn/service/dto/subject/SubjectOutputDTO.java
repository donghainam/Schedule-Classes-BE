package namdh.dhbkhn.datn.service.dto.class_name;

import namdh.dhbkhn.datn.domain.Classes;

public class ClassesOutputDTO {

    private Long id;
    private Long userId;
    private String name;
    private String classNote;
    private String courseCode;
    private int startWeek;
    private int numberOfLessons;
    private int numberOfWeekStudy;
    private String semester;
    private int conditions;
    private int countWeekStudied;
    private String departmentName;

    public ClassesOutputDTO() {}

    public ClassesOutputDTO(Classes classes) {
        this.id = classes.getId();
        this.userId = classes.getUser().getId();
        this.name = classes.getName();
        this.classNote = classes.getClassNote();
        this.courseCode = classes.getCourseCode();
        this.startWeek = classes.getStartWeek();
        this.numberOfLessons = classes.getNumberOfLessons();
        this.semester = classes.getSemester();
        this.conditions = classes.getConditions();
        this.countWeekStudied = classes.getCountWeekStudied();
        this.numberOfWeekStudy = classes.getNumberOfWeekStudy();
        this.departmentName = classes.getDepartmentName();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassNote() {
        return classNote;
    }

    public void setClassNote(String classNote) {
        this.classNote = classNote;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public int getStartWeek() {
        return startWeek;
    }

    public void setStartWeek(int startWeek) {
        this.startWeek = startWeek;
    }

    public int getNumberOfLessons() {
        return numberOfLessons;
    }

    public void setNumberOfLessons(int numberOfLessons) {
        this.numberOfLessons = numberOfLessons;
    }

    public int getNumberOfWeekStudy() {
        return numberOfWeekStudy;
    }

    public void setNumberOfWeekStudy(int numberOfWeekStudy) {
        this.numberOfWeekStudy = numberOfWeekStudy;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public int getConditions() {
        return conditions;
    }

    public void setConditions(int conditions) {
        this.conditions = conditions;
    }

    public int getCountWeekStudied() {
        return countWeekStudied;
    }

    public void setCountWeekStudied(int countWeekStudied) {
        this.countWeekStudied = countWeekStudied;
    }

    public int getCountCondition() {
        return countWeekStudied;
    }

    public void setCountCondition(int countCondition) {
        this.countWeekStudied = countCondition;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
}
