package afsdigital.grahamselect.api.admin.dto;

import jakarta.validation.constraints.Size;

public record UpdateAdminTickerRequest(
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
        String name,

        @Size(max = 50, message = "A classe de ativo deve ter no máximo 50 caracteres.")
        String assetClass,

        @Size(max = 100, message = "O setor deve ter no máximo 100 caracteres.")
        String sector
) {
}
