package com.smartclassroom.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/groups")
public class StudentGroupCrudController {

    private final StudentGroupCrudService service;

    public StudentGroupCrudController(
            StudentGroupCrudService service) {

        this.service = service;
    }

    // Xem lớp theo ID
    @GetMapping("/{id}")
    public StudentGroup getById(@PathVariable long id) {
        return service.getById(id);
    }

    // Thêm lớp
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentGroup create(
            @RequestBody StudentGroupRequest request) {

        return service.create(request);
    }

    // Sửa lớp
    @PutMapping("/{id}")
    public StudentGroup update(
            @PathVariable long id,
            @RequestBody StudentGroupRequest request) {

        return service.update(id, request);
    }

    // Xóa lớp
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
