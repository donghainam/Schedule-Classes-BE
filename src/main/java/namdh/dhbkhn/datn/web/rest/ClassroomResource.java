package namdh.dhbkhn.datn.web.rest;

import java.util.List;
import namdh.dhbkhn.datn.service.ClassroomService;
import namdh.dhbkhn.datn.service.dto.classroom.ClassroomInputDTO;
import namdh.dhbkhn.datn.service.dto.classroom.ClassroomOutputDTO;
import namdh.dhbkhn.datn.service.dto.classroom.NumClassroomDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api/classroom")
public class ClassroomResource {

    private final ClassroomService classroomService;

    public ClassroomResource(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @PostMapping("/create")
    public ResponseEntity<ClassroomOutputDTO> create(@RequestBody ClassroomInputDTO classroomInputDTO) {
        return new ResponseEntity<>(this.classroomService.create(classroomInputDTO), HttpStatus.CREATED);
    }

    @GetMapping("")
    public ResponseEntity<List<ClassroomOutputDTO>> getAll(Pageable pageable, @RequestParam(required = false) String name) {
        Page<ClassroomOutputDTO> page = classroomService.getAll(pageable, name);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassroomOutputDTO> getOne(@PathVariable(name = "id") long id) {
        return new ResponseEntity<>(this.classroomService.getOne(id), HttpStatus.OK);
    }

    @GetMapping("/number-of-classroom")
    public ResponseEntity<NumClassroomDTO> getNumForAllClassroom() {
        return new ResponseEntity<>(this.classroomService.getNumbForAllClassroom(), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassroomOutputDTO> update(@PathVariable(name = "id") long id, @RequestBody ClassroomInputDTO classroomInputDTO) {
        return new ResponseEntity<>(this.classroomService.update(classroomInputDTO, id), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable(name = "id") long id) {
        this.classroomService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
