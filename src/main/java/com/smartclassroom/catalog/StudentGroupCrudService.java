package com.smartclassroom.catalog;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class StudentGroupCrudService {

    private final StudentGroupCrudRepository repository;

    public StudentGroupCrudService(
            StudentGroupCrudRepository repository) {

        this.repository = repository;
    }

    // Xem chi tiết lớp
    public StudentGroup getById(long id) {

        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy lớp sinh viên"
                ));
    }

    // Thêm lớp
    public StudentGroup create(StudentGroupRequest request) {

        StudentGroupRequest group = validate(request);

        try {
            return repository.insert(group);

        } catch (DataIntegrityViolationException ex) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mã lớp đã tồn tại hoặc dữ liệu vi phạm ràng buộc"
            );
        }
    }

    // Sửa lớp
    public StudentGroup update(
            long id,
            StudentGroupRequest request) {

        getById(id);

        StudentGroupRequest group = validate(request);

        try {
            int count = repository.update(id, group);

            if (count == 0) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy lớp để cập nhật"
                );
            }

            return getById(id);

        } catch (DataIntegrityViolationException ex) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mã lớp đã tồn tại hoặc dữ liệu vi phạm ràng buộc"
            );
        }
    }

    // Xóa lớp
    public void delete(long id) {

        getById(id);

        try {
            int count = repository.delete(id);

            if (count == 0) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy lớp để xóa"
                );
            }

        } catch (DataIntegrityViolationException ex) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể xóa lớp đã có phân công học phần"
            );
        }
    }

    // Kiểm tra thông tin lớp
    private StudentGroupRequest validate(
            StudentGroupRequest request) {

        if (request == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thiếu thông tin lớp sinh viên"
            );
        }

        String code = request.code() == null
                ? ""
                : request.code().trim().toUpperCase(Locale.ROOT);

        String name = request.name() == null
                ? ""
                : request.name().trim();

        Integer count = request.studentCount();

        if (!code.matches("[A-Z0-9-]{2,20}")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã lớp phải gồm 2-20 ký tự chữ, số hoặc dấu -"
            );
        }

        if (name.isBlank() || name.length() > 100) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên lớp không hợp lệ"
            );
        }

        if (count == null || count < 1 || count > 500) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sĩ số phải từ 1 đến 500"
            );
        }

        return new StudentGroupRequest(
                code,
                name,
                count
        );
    }
}
