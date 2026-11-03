package com.smartclassroom.schedule;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auto-schedule")
public class AutoScheduleController {

    private final AutoScheduleConfirmationService confirmation;

    public AutoScheduleController(
            AutoScheduleConfirmationService confirmation) {

        this.confirmation = confirmation;
    }

    // Chỉ xem trước lịch, không lưu
    @PostMapping("/preview")
    public AutoSchedulePreview preview(
            @RequestBody AutoScheduleRequest request) {

        return confirmation.preview(request);
    }

    // Chỉ lưu khi có mã xác nhận hợp lệ
    @PostMapping("/confirm")
    public AutoSchedulePlan confirm(
            @RequestBody AutoScheduleConfirmationRequest request) {

        return confirmation.confirm(request);
    }
}
