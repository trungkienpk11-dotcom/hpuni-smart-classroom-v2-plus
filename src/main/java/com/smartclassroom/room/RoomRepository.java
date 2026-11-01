
package com.smartclassroom.room;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class RoomRepository {

    private final JdbcTemplate jdbc;

    public RoomRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Room> findAll() {

        String sql = "SELECT * FROM rooms ORDER BY id";

        return jdbc.query(sql, (rs, rowNum) ->
                new Room(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getInt("capacity"),
                        rs.getString("room_type"),
                        rs.getBoolean("projector"),
                        rs.getBoolean("computers")
                )
        );
    }
}
