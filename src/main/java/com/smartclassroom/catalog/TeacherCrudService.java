package com.smartclassroom.catalog;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class TeacherCrudService {

    private final TeacherCrudRepository repository;

    public TeacherCrudService(TeacherCrudRepository repository) {
        this.repository = repository;
    }

    public Teacher getById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy giảng viên"
                ));
    }

    public Teacher create(TeacherRequest request) {

        TeacherRequest teacher = validate(request);

        try {
            return repository.insert(teacher);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mã giảng viên hoặc email đã tồn tại"
            );
        }
    }

    public Teacher update(long id, TeacherRequest request) {

        getById(id);
        TeacherRequest teacher = validate(request);

        try {
            int count = repository.update(id, teacher);

            if (count == 0) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy giảng viên"
                );
            }

            return getById(id);

        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mã giảng viên hoặc email đã tồn tại"
            );
        }
    }

    public void delete(long id) {

        getById(id);

        try {
            repository.delete(id);

        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Giảng viên đã được phân công giảng dạy"
            );
        }
    }

    private TeacherRequest validate(TeacherRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thiếu dữ liệu giảng viên"
            );
        }

        String code = request.code() == null
                ? ""
                : request.code().trim().toUpperCase(Locale.ROOT);

        String name = request.fullName() == null
                ? ""
                : request.fullName().trim();

        String email = request.email() == null
                ? ""
                : request.email().trim().toLowerCase(Locale.ROOT);

        if (!code.matches("[A-Z0-9-]{2,20}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã giảng viên phải có 2-20 ký tự"
            );
        }

        if (name.isBlank() || name.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Họ tên không hợp lệ"
            );
        }

        if (email.length() > 120 ||
                !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email không hợp lệ"
            );
        }

        return new TeacherRequest(code, name, email);
    }
}
