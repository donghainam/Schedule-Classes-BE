package namdh.dhbkhn.datn.repository;

import java.util.Optional;
import namdh.dhbkhn.datn.domain.Classroom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    Optional<Classroom> findByNameAndUserId(String name, Long userId);

    @Query(value = "Select * from classroom where status is false order by id desc limit 0, 1", nativeQuery = true)
    Classroom getLastClassroomByStatusIsFalse();

    Page<Classroom> getAllByUserIdAndNameIsNotNull(Pageable pageable, Long userId);

    Page<Classroom> findAllByNameContainingIgnoreCaseAndUserId(Pageable pageable, String name, Long userId);

    Long countAllByUserId(Long userId);
}
