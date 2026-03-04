package com.github.mangila.app.director.persistance.outbox;

import com.github.mangila.app.shared.persistence.base.OutboxBaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity(name = "director_outbox")
@Table(name = "director_outbox")
public class DirectorOutboxEntity extends OutboxBaseEntity {

    public DirectorOutboxEntity() {
        // do nothing, for JPA
    }

}
