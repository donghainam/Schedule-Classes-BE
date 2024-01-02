package namdh.dhbkhn.datn.domain;

import java.io.Serializable;
import javax.persistence.*;

@Entity
@Table(name = "classroom")
public class Classroom extends AbstractAuditingEntity<Long> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "name")
    private String name;

    @Column(name = "max_sv")
    private int maxSv;

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
