package com.smartclassroom.catalog;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CatalogService {

    private final CatalogRepository repository;

    public CatalogService(CatalogRepository repository) {
        this.repository = repository;
    }

    public List<Teacher> getTeachers() {
        return repository.findTeachers();
    }

    public List<StudentGroup> getGroups() {
        return repository.findGroups();
    }

    public List<Course> getCourses() {
        return repository.findCourses();
    }

    public List<CourseOffering> getOfferings() {
        return repository.findOfferings();
    }
}
