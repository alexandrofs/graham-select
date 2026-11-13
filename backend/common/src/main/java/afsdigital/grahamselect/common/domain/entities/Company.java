package afsdigital.grahamselect.common.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Builder
public class Company {

    private final String id;
    private final String ticker;
    private final String name;
    private final String assetClass;
    private final String sector;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Company(String ticker) {
        this(UUID.randomUUID().toString(), ticker, null, null, null, null, null);
    }

    public Company(String id, String ticker) {
        this(id, ticker, null, null, null, null, null);
    }

}

