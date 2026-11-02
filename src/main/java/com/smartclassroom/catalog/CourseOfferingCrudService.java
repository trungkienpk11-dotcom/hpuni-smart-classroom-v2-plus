
package com.smartclassroom.catalog;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
public class CourseOfferingCrudService {

    private final CourseOfferingCrudRepository repository;

    public CourseOfferingCrudService(
            CourseOfferingCrudRepository repository) {

        this.repository = repository;
    }

    // Tìm một lớp học phần
    public CourseOfferingDetail getById(long id) {

        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy lớp học phần"
                ));
    }

    // Thêm lớp học phần
    public CourseOfferingDetail create(
            CourseOfferingRequest request) {

        CourseOfferingRequest data = validate(request);

        try {
            return repository.insert(data);

        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Phân công đã tồn tại hoặc vi phạm ràng buộc"
            );
        }
    }

    // Cập nhật lớp học phần
    public CourseOfferingDetail update(
            long id, CourseOfferingRequest request) {

        getById(id);

        CourseOfferingRequest data = validate(request);

        try {
            int count = repository.update(id, data);

            if (count == 0) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy lớp học phần"
                );
            }

            return getById(id);

        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Phân công đã tồn tại hoặc vi phạm ràng buộc"
            );
        }
    }

    // Xóa lớp học phần
    public void delete(long id) {

        getById(id);

        try {
            int count = repository.delete(id);

            if (count == 0) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy lớp học phần"
                );
            }

        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Lớp học phần đang được sử dụng"
            );
        }
    }

    // Kiểm tra dữ liệu đầu vào
    private CourseOfferingRequest validate(
            CourseOfferingRequest request) {

        if (request == null) {
            throw badRequest("Thiếu thông tin phân công");
        }

        Long courseId = request.courseId();
        Long teacherId = request.teacherId();
        Long groupId = request.groupId();

        if (courseId == null || courseId <= 0
                || teacherId == null || teacherId <= 0
                || groupId == null || groupId <= 0) {

            throw badRequest(
                    "Phải chọn môn, giảng viên và lớp sinh viên"
            );
        }

        if (!repository.existsCourse(courseId)) {
            throw badRequest("Môn học không tồn tại");
        }

        if (!repository.existsTeacher(teacherId)) {
            throw badRequest("Giảng viên không tồn tại");
        }

        if (!repository.existsGroup(groupId)) {
            throw badRequest("Lớp sinh viên không tồn tại");
        }

        String semester = request.semester() == null
                ? ""
                : request.semester().trim().toUpperCase();

        if (!Set.of("HK1", "HK2", "HK3").contains(semester)) {
            throw badRequest("Học kỳ không hợp lệ");
        }

        String year = request.academicYear() == null
                ? ""
                : request.academicYear().trim();

        if (!year.matches("20\\d{2}-20\\d{2}")) {
            throw badRequest(
                    "Năm học phải có dạng 2026-2027"
            );
        }

        int startYear = Integer.parseInt(
                year.substring(0, 4)
        );

        int endYear = Integer.parseInt(
                year.substring(5, 9)
        );

        if (endYear != startYear + 1) {
            throw badRequest(
                    "Năm kết thúc phải liền sau năm bắt đầu"
            );
        }

        Integer sessions = request.sessionsPerWeek();

        if (sessions == null || sessions < 1 || sessions > 7) {
            throw badRequest(
                    "Số buổi mỗi tuần phải từ 1 đến 7"
            );
        }

        return new CourseOfferingRequest(
                courseId,
                teacherId,
                groupId,
                semester,
                year,
                sessions
        );
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
