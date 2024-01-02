package namdh.dhbkhn.datn.web.rest;

import java.io.IOException;
import java.util.List;
import namdh.dhbkhn.datn.service.ClassesService;
import namdh.dhbkhn.datn.service.dto.class_name.ClassesInputDTO;
import namdh.dhbkhn.datn.service.dto.class_name.ClassesOutputDTO;
import namdh.dhbkhn.datn.service.dto.class_name.NumClassesDTO;
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
public class ClassesResource {

    private final ClassesService classesService;

    public ClassesResource(ClassesService classNameService) {
        this.classesService = classNameService;
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
        classesService.importClassList(file.getInputStream());
    }

    @PostMapping("/create")
    public ResponseEntity<ClassesOutputDTO> create(@RequestBody ClassesInputDTO classesInputDTO) {
        return new ResponseEntity<>(this.classesService.create(classesInputDTO), HttpStatus.CREATED);
    }

    @GetMapping("")
    public ResponseEntity<List<ClassesOutputDTO>> getAll(Pageable pageable, @RequestParam(required = false) String name) {
        Page<ClassesOutputDTO> page = this.classesService.getAll(pageable, name);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassesOutputDTO> getOne(@PathVariable(name = "id") long id) {
        return new ResponseEntity<>(this.classesService.getOne(id), HttpStatus.OK);
    }

    @GetMapping("/number-of-classes")
    public ResponseEntity<NumClassesDTO> getNumForAllClasses() {
        return new ResponseEntity<>(this.classesService.getNumbForAllClasses(), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassesOutputDTO> update(@PathVariable(name = "id") long id, @RequestBody ClassesInputDTO classesInputDTO) {
        return new ResponseEntity<>(this.classesService.update(classesInputDTO, id), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable(name = "id") long id) {
        this.classesService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
