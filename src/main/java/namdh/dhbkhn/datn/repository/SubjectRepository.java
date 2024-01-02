package namdh.dhbkhn.datn.repository;

import java.util.List;
import java.util.Optional;
import namdh.dhbkhn.datn.domain.Classes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassesRepository extends JpaRepository<Classes, Long> {
    Optional<Classes> findByClassNoteAndUserId(String classNote, Long userId);

    @Query(
        value = "Select * from classes where start_week <= ?1 and user_id = ?3 " +
        "and count_week_studied < number_of_week_study " +
        "and upper(semester) like upper(concat('%', ?2, '%')) order by id desc",
        nativeQuery = true
    )
    List<Classes> getAllClasses(int startWeek, String semester, Long userId);

    List<Classes> getClassesBySemester(String semester);

    Page<Classes> findAllByUserIdAndNameIsNotNull(Pageable pageable, Long userId);

    Page<Classes> findAllByNameContainingIgnoreCaseAndUserId(Pageable pageable, String name, Long userId);

    Optional<Classes> findByCourseCodeAndClassNoteAndUserId(String courseCode, String classNote, Long userId);

    Long countAllByUserId(Long userId);
}
