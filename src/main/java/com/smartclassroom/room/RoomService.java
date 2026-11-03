
package com.smartclassroom.room;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RoomService {

    private final RoomRepository repository;

    public RoomService(RoomRepository repository) {
        this.repository = repository;
    }

    // Lấy danh sách phòng
    public List<Room> getAllRooms() {
        return repository.findAll();
    }

    // Xem một phòng theo ID
    public Room getRoomById(long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy phòng học"
                        )
                );
    }

    // Thêm phòng mới
    public Room createRoom(RoomRequest request) {
        RoomRequest room = validate(request);

        try {
            return repository.insert(room);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mã phòng đã tồn tại"
            );
        }
    }

    // Sửa phòng
    public Room updateRoom(long id, RoomRequest request) {

        getRoomById(id);

        RoomRequest room = validate(request);

        try {
            int updated = repository.update(id, room);

            if (updated == 0) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy phòng để cập nhật"
                );
            }

            return getRoomById(id);

        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mã phòng đã được sử dụng"
            );
        }
    }

    // Xóa phòng
    public void deleteRoom(long id) {

        int deleted = repository.delete(id);

        if (deleted == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy phòng để xóa"
            );
        }
    }

    // Kiểm tra và chuẩn hóa dữ liệu đầu vào
    private RoomRequest validate(RoomRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thiếu thông tin phòng học"
            );
        }

        String code = request.code() == null
                ? ""
                : request.code().trim().toUpperCase(Locale.ROOT);

        String name = request.name() == null
                ? ""
                : request.name().trim();

        String type = request.roomType() == null
                ? ""
                : request.roomType().trim().toUpperCase(Locale.ROOT);

        if (!code.matches("[A-Z0-9-]{2,20}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã phòng phải gồm 2-20 ký tự chữ, số hoặc dấu -"
            );
        }

        if (name.isBlank() || name.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên phòng phải có từ 1 đến 100 ký tự"
            );
        }

        if (request.capacity() == null
                || request.capacity() < 1
                || request.capacity() > 1000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sức chứa phải từ 1 đến 1000"
            );
        }

        if (!Set.of(
                "NORMAL",
                "COMPUTER_LAB",
                "LECTURE_HALL"
        ).contains(type)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Loại phòng không hợp lệ"
            );
        }

        return new RoomRequest(
                code,
                name,
                request.capacity(),
                type,
                Boolean.TRUE.equals(request.projector()),
                Boolean.TRUE.equals(request.computers())
        );
    }
}
