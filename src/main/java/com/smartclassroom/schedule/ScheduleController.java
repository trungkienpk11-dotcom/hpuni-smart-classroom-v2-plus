package com.smartclassroom.schedule;

import com.smartclassroom.room.Room;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    // GET: Lấy thời khóa biểu của một tuần
    @GetMapping
    public List<ScheduleSession> getWeek(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStart) {

        return service.getWeek(weekStart);
    }

    // GET: Tìm phòng trống
    @GetMapping("/available-rooms")
    public List<Room> availableRooms(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime start,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime end,

            @RequestParam(defaultValue = "1")
            int minCapacity) {

        return service.findAvailableRooms(
                date, start, end, minCapacity
        );
    }

    // GET: Xem chi tiết buổi học
    @GetMapping("/{id}")
    public ScheduleSession getById(@PathVariable long id) {
        return service.getById(id);
    }

    // POST: Tạo lịch mới
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleSession create(
            @RequestBody ScheduleRequest request) {

        return service.create(request);
    }

    // PUT: Sửa lịch
    @PutMapping("/{id}")
    public ScheduleSession update(
            @PathVariable long id,
            @RequestBody ScheduleRequest request) {

        return service.update(id, request);
    }

    // DELETE: Xóa lịch
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
