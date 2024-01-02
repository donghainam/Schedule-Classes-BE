package namdh.dhbkhn.datn.service.dto.classroom;

import namdh.dhbkhn.datn.domain.Classroom;

public class ClassroomOutputDTO {

    private Long id;
    private Long userId;
    private String name;
    private int maxSv;

    public ClassroomOutputDTO() {}

    public ClassroomOutputDTO(Classroom classroom) {
        this.id = classroom.getId();
        this.userId = classroom.getUser().getId();
        this.name = classroom.getName();
        this.maxSv = classroom.getMaxSv();
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

    public int getMaxSv() {
        return maxSv;
    }

    public void setMaxSv(int maxSv) {
        this.maxSv = maxSv;
    }
}
