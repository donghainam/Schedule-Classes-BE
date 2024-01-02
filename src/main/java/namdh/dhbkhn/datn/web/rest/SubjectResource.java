package namdh.dhbkhn.datn.web.rest;

import java.io.IOException;
import java.util.List;
import namdh.dhbkhn.datn.service.SubjectService;
import namdh.dhbkhn.datn.service.dto.subject.NumSubjectDTO;
import namdh.dhbkhn.datn.service.dto.subject.SubjectInputDTO;
import namdh.dhbkhn.datn.service.dto.subject.SubjectOutputDTO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api/classes")
public class SubjectResource {

    private final SubjectService subjectService;

    public SubjectResource(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplateImportClass() {
        Resource resource = new ClassPathResource("TemplateImportSubject.xlsx");
        return ResponseEntity
            .ok()
            .header("content-disposition", "attachment; filename=TemplateImportSubject.xlsx")
            .header("Pragma", "public")
            .header("Cache-Control", "no-store")
            .header("Cache-Control", "max-age=0")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(resource);
    }

    @PostMapping("/import")
    public void importClassViaExcel(@RequestParam("file") MultipartFile file) throws IOException {
        subjectService.importClassList(file.getInputStream());
    }

    @PostMapping("/create")
    public ResponseEntity<SubjectOutputDTO> create(@RequestBody SubjectInputDTO subjectInputDTO) {
        return new ResponseEntity<>(this.subjectService.create(subjectInputDTO), HttpStatus.CREATED);
    }

    @GetMapping("")
    public ResponseEntity<List<SubjectOutputDTO>> getAll(Pageable pageable, @RequestParam(required = false) String name) {
        Page<SubjectOutputDTO> page = this.subjectService.getAll(pageable, name);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubjectOutputDTO> getOne(@PathVariable(name = "id") long id) {
        return new ResponseEntity<>(this.subjectService.getOne(id), HttpStatus.OK);
    }

    @GetMapping("/number-of-classes")
    public ResponseEntity<NumSubjectDTO> getNumForAllClasses() {
        return new ResponseEntity<>(this.subjectService.getNumbForAllSubject(), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubjectOutputDTO> update(@PathVariable(name = "id") long id, @RequestBody SubjectInputDTO subjectInputDTO) {
        return new ResponseEntity<>(this.subjectService.update(subjectInputDTO, id), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable(name = "id") long id) {
        this.subjectService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
