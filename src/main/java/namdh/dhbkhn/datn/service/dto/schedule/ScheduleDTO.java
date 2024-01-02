package namdh.dhbkhn.datn.service.dto.schedule;

import java.util.HashMap;
import java.util.Map;
import namdh.dhbkhn.datn.domain.enumeration.WeekDay;

public class ScheduleDTO {

    private String courseCode;
    private String className;
    private String classNote;
    private String classroom;
    private String departmentName;
    private int maxSv;
    private Map<String, Object> timeNote;

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassNote() {
        return classNote;
    }

    public void setClassNote(String classNote) {
        this.classNote = classNote;
    }

    public String getClassroom() {
        return classroom;
    }

    public void setClassroom(String classroom) {
        this.classroom = classroom;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public int getMaxSv() {
        return maxSv;
    }

    public void setMaxSv(int maxSv) {
        this.maxSv = maxSv;
    }

    public Map<String, Object> getTimeNote() {
        return timeNote;
    }

    public void setTimeNote(Map<String, Object> timeNote) {
        this.timeNote = timeNote;
    }

    public ScheduleDTO addTimeNote(String key, Object value) {
        if (this.timeNote == null) {
            this.timeNote = new HashMap<>();
        }
        this.timeNote.put(key, value);
        return this;
    }

    public <T> T getTimeNoteExtra(String key) {
        if (this.timeNote != null && this.timeNote.containsKey(key)) {
            return (T) this.timeNote.get(key);
        }
        return null;
    }
}
