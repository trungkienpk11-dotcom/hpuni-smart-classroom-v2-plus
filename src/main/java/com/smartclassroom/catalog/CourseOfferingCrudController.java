package com.smartclassroom.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/offerings")
public class CourseOfferingCrudController {

    private final CourseOfferingCrudService service;

    public CourseOfferingCrudController(
            CourseOfferingCrudService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public CourseOfferingDetail getById(
            @PathVariable long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseOfferingDetail create(
            @RequestBody CourseOfferingRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CourseOfferingDetail update(
            @PathVariable long id,
            @RequestBody CourseOfferingRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
