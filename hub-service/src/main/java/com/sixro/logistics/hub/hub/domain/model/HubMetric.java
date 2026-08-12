package com.sixro.logistics.hub.hub.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(schema = "hub_schema", name = "p_hub_metrics")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HubMetric {

    @Id
    @Column(name = "hub_id", columnDefinition = "uuid")
    private UUID hubId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "hub_id")
    private Hub hub;

    @Column(name = "current_volume", nullable = false)
    private int currentVolume;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public HubMetric(Hub hub, int currentVolume) {
        this.hub = hub;
        this.currentVolume = currentVolume;
    }

    public void updateVolume(int currentVolume) {
        this.currentVolume = currentVolume;
    }
}