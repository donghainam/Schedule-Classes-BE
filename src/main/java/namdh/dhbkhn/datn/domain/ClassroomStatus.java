package namdh.dhbkhn.datn.domain;

import com.vladmihalcea.hibernate.type.json.JsonStringType;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = "classroom_status")
public class ClassroomStatus implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "classroom_id")
    private Classroom classroom;

    @Column(name = "week")
    private int week;

    @Type(type = "json")
    @Column(name = "time_note")
    private Map<String, Object> timeNote;

    @Column(name = "status")
    private int status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Classroom getClassroom() {
        return classroom;
    }

    public void setClassroom(Classroom classroom) {
        this.classroom = classroom;
    }

    public int getWeek() {
        return week;
    }

    public void setWeek(int week) {
        this.week = week;
    }

    public Map<String, Object> getTimeNote() {
        return timeNote;
    }

    public void setTimeNote(Map<String, Object> timeNote) {
        this.timeNote = timeNote;
    }

    public ClassroomStatus addTimeNote(String key, Object value) {
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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
