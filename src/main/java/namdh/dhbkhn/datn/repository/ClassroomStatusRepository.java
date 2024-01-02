package namdh.dhbkhn.datn.repository;

import java.util.List;
import java.util.Optional;
import namdh.dhbkhn.datn.domain.ClassroomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassroomStatusRepository extends JpaRepository<ClassroomStatus, Long> {
    @Query(value = "Select * from classroom_status where week = ?1 and status <= ?2 and user_id = ?3 limit 0, 1", nativeQuery = true)
    ClassroomStatus getClassroomStatusByWeekAndStatusAndUserId(int week, int status, Long userId);

    Optional<ClassroomStatus> findByClassroomIdAndWeek(long classroomId, int week);

    List<ClassroomStatus> findAllByClassroomId(long id);
}
