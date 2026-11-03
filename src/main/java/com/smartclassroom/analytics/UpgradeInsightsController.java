package com.smartclassroom.analytics;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.sql.Date;
import java.sql.Time;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Chỉ đọc: dữ liệu học tập thực tế của bản tự học, không dùng database V2 cũ. */
@RestController
@RequestMapping("/api/upgrade")
public class UpgradeInsightsController {
    private final JdbcTemplate db;
    public UpgradeInsightsController(JdbcTemplate db) { this.db = db; }

    private static Object field(Map<String,Object> row,String key) {
        for(var e:row.entrySet()) if(e.getKey().equalsIgnoreCase(key)) return e.getValue();
        throw new IllegalStateException("Missing SQL column: "+key);
    }

    @GetMapping("/insights")
    public Map<String,Object> insights(@RequestParam(name="week",required=false) LocalDate week) {
        LocalDate monday=(week==null?LocalDate.now():week).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Date start=Date.valueOf(monday), end=Date.valueOf(monday.plusDays(6));
        List<Map<String,Object>> usage=db.queryForList("""
            SELECT r.code, r.name, r.capacity, COUNT(s.id) AS sessions
            FROM rooms r LEFT JOIN schedule_sessions s
              ON r.id=s.room_id AND s.session_date BETWEEN ? AND ?
            GROUP BY r.id,r.code,r.name,r.capacity
            ORDER BY sessions DESC,r.code
            """,start,end);
        List<Map<String,Object>> daily=db.queryForList("""
            SELECT session_date, COUNT(*) AS sessions FROM schedule_sessions
            WHERE session_date BETWEEN ? AND ? GROUP BY session_date
            """,start,end);
        List<Map<String,Object>> days=new ArrayList<>();
        for(int i=0;i<7;i++) {
            LocalDate date=monday.plusDays(i);
            long count=daily.stream().filter(x->date.equals(((Date)field(x,"session_date")).toLocalDate()))
                .mapToLong(x->((Number)field(x,"sessions")).longValue()).sum();
            days.add(Map.of("date",date.toString(),"sessions",count));
        }
        long total=days.stream().mapToLong(x->(long)x.get("sessions")).sum();
        long active=usage.stream().filter(x->((Number)field(x,"sessions")).longValue()>0).count();
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("weekStart",monday.toString());
        result.put("weekEnd",monday.plusDays(6).toString());
        result.put("weeklySessions",total);
        result.put("totalRooms",usage.size());
        result.put("activeRooms",active);
        result.put("byDay",days);
        result.put("roomUsage",usage);
        return result;
    }

    @GetMapping("/availability")
    public List<Map<String,Object>> availability(@RequestParam("date") LocalDate date,
        @RequestParam("start") LocalTime start,@RequestParam("end") LocalTime end,
        @RequestParam(name="capacity",defaultValue="1") int capacity,
        @RequestParam(name="computers",defaultValue="false") boolean computers,
        @RequestParam(name="projector",defaultValue="false") boolean projector) {
        if(!start.isBefore(end) || capacity<1 || capacity>10000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Ngày/giờ hoặc sức chứa không hợp lệ");
        // SQL lọc trùng giờ bằng giao khoảng [start,end): a.start < b.end AND a.end > b.start
        return db.query("""
          SELECT r.id,r.code,r.name,r.capacity,r.room_type,r.computers,r.projector
          FROM rooms r
          WHERE r.capacity >= ? AND (?=FALSE OR r.computers=TRUE)
            AND (?=FALSE OR r.projector=TRUE)
            AND NOT EXISTS (
              SELECT 1 FROM schedule_sessions s
              WHERE s.room_id=r.id AND s.session_date=?
                AND s.start_time < ? AND s.end_time > ?
            )
          ORDER BY r.capacity,r.code
          """,(rs,n)->{
            Map<String,Object> x=new LinkedHashMap<>();
            x.put("id",rs.getLong("id"));x.put("code",rs.getString("code"));
            x.put("name",rs.getString("name"));x.put("capacity",rs.getInt("capacity"));
            x.put("roomType",rs.getString("room_type"));
            x.put("computers",rs.getBoolean("computers"));x.put("projector",rs.getBoolean("projector"));
            return x;
          }, capacity,computers,projector,Date.valueOf(date),Time.valueOf(end),Time.valueOf(start));
    }
}
