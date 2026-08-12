package com.sixro.logistics.notification.domain.entity;

import com.sixro.logistics.notification.domain.entity.AIRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_ai_log") // 프로젝트 테이블 명명 규칙에 맞게 수정 가능
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AIRequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ai_request_log_id")
    private UUID aiRequestLogId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "request_data", columnDefinition = "text", nullable = false)
    private String requestData;

    @Column(name = "response_data", columnDefinition = "text")
    private String responseData;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", length = 20)
    private AIRequestStatus requestStatus;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}
