package com.fiap.bank.atm.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public abstract class BaseEntity {

    private final UUID id;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected BaseEntity(UUID id) {
        this(id, LocalDateTime.now(), LocalDateTime.now());
    }

    protected BaseEntity(UUID id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "Id cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "UpdatedAt cannot be null");
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    protected void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseEntity other)) {
            return false;
        }
        return getClass() == o.getClass() && id.equals(other.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }
}
