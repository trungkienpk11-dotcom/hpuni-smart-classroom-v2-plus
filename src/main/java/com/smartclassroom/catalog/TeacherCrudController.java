package com.smartclassroom.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/teachers")
public class TeacherCrudController {

    private final TeacherCrudService service;

    public TeacherCrudController(TeacherCrudService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public Teacher getById(@PathVariable long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Teacher create(@RequestBody TeacherRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public Teacher update(
            @PathVariable long id,
            @RequestBody TeacherRequest request) {

        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
