package afsdigital.grahamselect.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAdminTickerRequest(
        @NotBlank(message = "O ticker é obrigatório.")
        @Pattern(regexp = "^\\s*[A-Za-z0-9]{2,10}\\s*$", message = "O ticker deve conter entre 2 e 10 caracteres alfanuméricos.")
        String ticker,

        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
        String name,

        @Size(max = 50, message = "A classe de ativo deve ter no máximo 50 caracteres.")
        String assetClass,

        @Size(max = 100, message = "O setor deve ter no máximo 100 caracteres.")
        String sector
) {
}
