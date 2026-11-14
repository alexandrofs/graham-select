package afsdigital.grahamselect.api.family.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFamilyProfileRequest(
        @NotBlank(message = "CPF é obrigatório.")
        @Size(max = 20, message = "CPF deve ter no máximo 20 caracteres.")
        String cpf,

        @NotBlank(message = "Nome é obrigatório.")
        @Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres.")
        String name,

        @Size(max = 50, message = "Parentesco deve ter no máximo 50 caracteres.")
        String relationship
) {
}

