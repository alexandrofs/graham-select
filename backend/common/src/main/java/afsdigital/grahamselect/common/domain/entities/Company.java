package afsdigital.grahamselect.common.domain.entities;

import lombok.Getter;

import java.util.UUID;

@Getter
public class Company {

    private final String id;
    private final String ticker;

    public Company(String ticker) {
        this.id = UUID.randomUUID().toString();
        this.ticker = ticker;
    }

}
