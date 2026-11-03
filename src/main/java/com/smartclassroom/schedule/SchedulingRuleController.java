package com.smartclassroom.schedule;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/scheduling-rules")
public class SchedulingRuleController {

    private final SchedulingRuleService service;

    public SchedulingRuleController(
            SchedulingRuleService service) {
        this.service = service;
    }

    // Xem lịch bận theo tuần
    @GetMapping("/busy")
    public List<SchedulingRuleService.TeacherBusy> getBusy(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStart) {

        return service.getBusyForWeek(weekStart);
    }

    // Thêm lịch bận
    @PostMapping("/busy")
    @ResponseStatus(HttpStatus.CREATED)
    public void addBusy(
            @RequestBody SchedulingRuleService.BusyRequest data) {

        service.addBusy(data);
    }

    // Xóa lịch bận
    @DeleteMapping("/busy/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBusy(@PathVariable long id) {
        service.deleteBusy(id);
    }

    // API lay danh sach giang vien
    @GetMapping("/teachers")
    public List<SchedulingRuleService.TeacherOption>
            getTeachers() {

        return service.getTeacherOptions();
    }

    // Xem yêu cầu thiết bị
    @GetMapping("/courses")
    public List<SchedulingRuleService.CourseRule> getCourses() {
        return service.getCourseRules();
    }

    // Cấu hình yêu cầu thiết bị của môn
    @PutMapping("/courses/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setCourseRule(
            @PathVariable long courseId,
            @RequestBody
            SchedulingRuleService.CourseRuleRequest data) {

        service.setCourseRule(courseId, data);
    }
}
