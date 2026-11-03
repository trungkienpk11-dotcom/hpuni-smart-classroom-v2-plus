package com.smartclassroom.schedule;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AutoScheduleConfirmationService {

    // Mã xác nhận có hiệu lực trong 10 phút
    private static final Duration PREVIEW_TTL =
            Duration.ofMinutes(10);

    private final AutoScheduleService scheduler;

    // Lưu phương án tạm thời trong RAM
    private final Map<String, PendingPreview> previews =
            new ConcurrentHashMap<>();

    private record PendingPreview(
            AutoScheduleRequest request,
            AutoSchedulePlan plan,
            Instant createdAt
    ) {}

    public AutoScheduleConfirmationService(
            AutoScheduleService scheduler) {

        this.scheduler = scheduler;
    }

    // 1. XẾP LỊCH THỬ
    // Chỉ tính toán, không ghi database
    public AutoSchedulePreview preview(
            AutoScheduleRequest request) {

        AutoSchedulePlan plan = scheduler.preview(request);

        Instant now = Instant.now();

        // Xóa các phiên xem trước đã hết hạn
        previews.entrySet().removeIf(entry ->
                now.isAfter(
                        entry.getValue().createdAt()
                                .plus(PREVIEW_TTL)
                )
        );

        // Không có đề xuất thì không cần mã xác nhận
        if (plan.suggestions().isEmpty()) {
            return new AutoSchedulePreview(null, plan);
        }

        String token = UUID.randomUUID().toString();

        previews.put(
                token,
                new PendingPreview(request, plan, now)
        );

        return new AutoSchedulePreview(token, plan);
    }

    // 2. XÁC NHẬN LƯU LỊCH
    @Transactional
    public synchronized AutoSchedulePlan confirm(
            AutoScheduleConfirmationRequest request) {

        if (request == null ||
                request.token() == null ||
                request.token().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thiếu mã xác nhận lịch"
            );
        }

        String token = request.token();

        PendingPreview saved = previews.get(token);

        if (saved == null) {
            throw conflict(
                    "Không có bản xem trước hợp lệ. " +
                            "Hãy xếp lịch thử lại."
            );
        }

        // Kiểm tra hết hạn
        if (Instant.now().isAfter(
                saved.createdAt().plus(PREVIEW_TTL))) {

            previews.remove(token);

            throw conflict(
                    "Bản xem trước đã hết hạn. " +
                            "Hãy xếp lịch thử lại."
            );
        }

        // Tính lại để kiểm tra dữ liệu hiện tại
        AutoSchedulePlan fresh =
                scheduler.preview(saved.request());

        if (!fresh.equals(saved.plan())) {

            previews.remove(token);

            throw conflict(
                    "Dữ liệu lịch học đã thay đổi. " +
                            "Hãy xếp lịch thử lại trước khi lưu."
            );
        }

        // Chỉ tới đây mới ghi vào database
        AutoSchedulePlan applied =
                scheduler.apply(saved.request());

        if (!applied.equals(saved.plan())) {

            previews.remove(token);

            throw conflict(
                    "Phương án đã thay đổi trong lúc lưu. " +
                            "Giao dịch được hủy."
            );
        }

        // Mỗi token chỉ được xác nhận một lần
        previews.remove(token);

        return applied;
    }

    private ResponseStatusException conflict(
            String message) {

        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }
}
