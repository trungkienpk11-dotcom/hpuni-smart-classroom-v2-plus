
package com.smartclassroom.room;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // GET: Lấy danh sách phòng
    @GetMapping
    public List<Room> getAllRooms() {
        return roomService.getAllRooms();
    }

    // GET: Xem chi tiết một phòng
    @GetMapping("/{id}")
    public Room getRoomById(@PathVariable long id) {
        return roomService.getRoomById(id);
    }

    // POST: Thêm phòng
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Room createRoom(@RequestBody RoomRequest request) {
        return roomService.createRoom(request);
    }

    // PUT: Cập nhật phòng
    @PutMapping("/{id}")
    public Room updateRoom(
            @PathVariable long id,
            @RequestBody RoomRequest request) {

        return roomService.updateRoom(id, request);
    }

    // DELETE: Xóa phòng
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(@PathVariable long id) {
        roomService.deleteRoom(id);
    }
}
