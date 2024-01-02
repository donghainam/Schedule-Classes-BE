package namdh.dhbkhn.datn.repository;

import java.util.List;
import java.util.Optional;
import namdh.dhbkhn.datn.domain.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    @Query(
        value = "Select * from subject where start_week <= ?1 and user_id = ?3 " +
        "and count_week_studied < number_of_week_study " +
        "and upper(semester) like upper(concat('%', ?2, '%')) order by id desc",
        nativeQuery = true
    )
    List<Subject> getAllClasses(int startWeek, String semester, Long userId);

    List<Subject> getClassesBySemester(String semester);

    Page<Subject> findAllByUserIdAndNameIsNotNull(Pageable pageable, Long userId);

    Page<Subject> findAllByNameContainingIgnoreCaseAndUserId(Pageable pageable, String name, Long userId);

    Optional<Subject> findByCourseCodeAndClassNoteAndUserId(String courseCode, String classNote, Long userId);

    Long countAllByUserId(Long userId);
}
