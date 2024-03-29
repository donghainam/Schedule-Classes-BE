package namdh.dhbkhn.datn.web.rest;

import namdh.dhbkhn.datn.service.ScheduleService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleResource {

    private final ScheduleService scheduleService;

    public ScheduleResource(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/excel")
    public ResponseEntity<Resource> exportSchedule(@RequestParam String semester, @RequestParam int numOfDayPerWeek) {
        byte[] bytes = scheduleService.exportSchedule(semester, numOfDayPerWeek);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity
            .ok()
            .header("content-disposition", "attachment; filename=Schedule_" + semester + ".xlsx")
            .header("Pragma", "public")
            .header("Cache-Control", "no-store")
            .header("Cache-Control", "max-age=0")
            .contentLength(bytes.length)
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(resource);
    }
}
