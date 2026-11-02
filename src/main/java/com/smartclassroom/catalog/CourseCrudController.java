package com.smartclassroom.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/courses")
public class CourseCrudController {

    private final CourseCrudService service;

    public CourseCrudController(CourseCrudService service) {
        this.service = service;
    }

    // GET: Xem chi tiết môn học
    @GetMapping("/{id}")
    public Course getById(@PathVariable long id) {
        return service.getById(id);
    }

    // POST: Thêm môn học
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Course create(@RequestBody CourseRequest request) {
        return service.create(request);
    }

    // PUT: Cập nhật môn học
    @PutMapping("/{id}")
    public Course update(
            @PathVariable long id,
            @RequestBody CourseRequest request) {

        return service.update(id, request);
    }

    // DELETE: Xóa môn học
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
