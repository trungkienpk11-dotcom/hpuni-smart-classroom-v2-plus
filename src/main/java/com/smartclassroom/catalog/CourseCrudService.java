package com.smartclassroom.catalog;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class CourseCrudService {

    private final CourseCrudRepository repository;

    public CourseCrudService(CourseCrudRepository repository) {
        this.repository = repository;
    }

    // 1. Xem môn học theo ID
    public Course getById(long id) {

        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy môn học"
                ));
    }

    // 2. Thêm môn học
    public Course create(CourseRequest request) {

        CourseRequest course = validate(request);

        try {
            return repository.insert(course);

        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mã môn học đã tồn tại hoặc dữ liệu bị trùng"
            );
        }
    }

    // 3. Cập nhật môn học
    public Course update(long id, CourseRequest request) {

        getById(id);

        CourseRequest course = validate(request);

        try {
            int updated = repository.update(id, course);

            if (updated == 0) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy môn học để sửa"
                );
            }

            return getById(id);

        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mã môn học đã tồn tại hoặc dữ liệu bị trùng"
            );
        }
    }

    // 4. Xóa môn học
    public void delete(long id) {

        getById(id);

        try {
            int deleted = repository.delete(id);

            if (deleted == 0) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy môn học để xóa"
                );
            }

        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể xóa môn đã được phân công giảng dạy"
            );
        }
    }

    // 5. Kiểm tra dữ liệu trước khi lưu
    private CourseRequest validate(CourseRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thiếu thông tin môn học"
            );
        }

        String code = request.code() == null
                ? ""
                : request.code().trim().toUpperCase(Locale.ROOT);

        String name = request.name() == null
                ? ""
                : request.name().trim();

        Integer credits = request.credits();

        if (!code.matches("[A-Z0-9-]{2,20}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã môn phải có 2-20 ký tự chữ, số hoặc dấu -"
            );
        }

        if (name.isBlank() || name.length() > 120) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên môn phải có từ 1 đến 120 ký tự"
            );
        }

        if (credits == null || credits < 1 || credits > 10) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số tín chỉ phải từ 1 đến 10"
            );
        }

        return new CourseRequest(code, name, credits);
    }
}
