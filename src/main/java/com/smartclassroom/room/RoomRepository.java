package com.smartclassroom.room;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RoomRepository {

    private final JdbcTemplate jdbc;

    public RoomRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Chuyển một dòng SQL thành đối tượng Room
    private final RowMapper<Room> mapper = (rs, rowNum) ->
            new Room(
                    rs.getLong("id"),
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getInt("capacity"),
                    rs.getString("room_type"),
                    rs.getBoolean("projector"),
                    rs.getBoolean("computers")
            );

    // 1. Lấy tất cả phòng học
    public List<Room> findAll() {
        String sql = "SELECT * FROM rooms ORDER BY id";

        return jdbc.query(sql, mapper);
    }

    // 2. Tìm phòng theo ID
    public Optional<Room> findById(long id) {
        String sql = "SELECT * FROM rooms WHERE id = ?";

        return jdbc.query(sql, mapper, id)
                .stream()
                .findFirst();
    }

    // 3. Tìm phòng theo mã phòng
    public Optional<Room> findByCode(String code) {
        String sql = "SELECT * FROM rooms WHERE code = ?";

        return jdbc.query(sql, mapper, code)
                .stream()
                .findFirst();
    }

    // 4. Thêm phòng mới
    public Room insert(RoomRequest room) {

        String sql = """
            INSERT INTO rooms
            (code, name, capacity, room_type,
             projector, computers)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        jdbc.update(
                sql,
                room.code(),
                room.name(),
                room.capacity(),
                room.roomType(),
                room.projector(),
                room.computers()
        );

        return findByCode(room.code()).orElseThrow();
    }

    // 5. Cập nhật thông tin phòng
    public int update(long id, RoomRequest room) {

        String sql = """
            UPDATE rooms
            SET code = ?,
                name = ?,
                capacity = ?,
                room_type = ?,
                projector = ?,
                computers = ?
            WHERE id = ?
            """;

        return jdbc.update(
                sql,
                room.code(),
                room.name(),
                room.capacity(),
                room.roomType(),
                room.projector(),
                room.computers(),
                id
        );
    }

    // 6. Xóa phòng theo ID
    public int delete(long id) {
        String sql = "DELETE FROM rooms WHERE id = ?";

        return jdbc.update(sql, id);
    }
}
