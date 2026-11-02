package com.smartclassroom.catalog;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    @GetMapping("/teachers")
    public List<Teacher> teachers() {
        return service.getTeachers();
    }

    @GetMapping("/groups")
    public List<StudentGroup> groups() {
        return service.getGroups();
    }

    @GetMapping("/courses")
    public List<Course> courses() {
        return service.getCourses();
    }

    @GetMapping("/offerings")
    public List<CourseOffering> offerings() {
        return service.getOfferings();
    }
}
