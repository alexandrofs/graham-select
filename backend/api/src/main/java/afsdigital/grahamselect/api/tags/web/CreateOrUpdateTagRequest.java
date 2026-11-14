package afsdigital.grahamselect.api.tags.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrUpdateTagRequest(
        @NotBlank(message = "Nome da tag é obrigatório.")
        @Size(max = 50, message = "Nome da tag deve ter no máximo 50 caracteres.")
        String name,

        @Size(max = 7, message = "Cor da tag deve ter no máximo 7 caracteres (#RRGGBB).")
        String color
) {
    public CreateOrUpdateTagRequest {
        name = trimAsciiSpaces(name);
        color = trimAsciiSpaces(color);
    }

    // Trim only spaces so newline/control characters remain observable to TagInputValidator.
    private static String trimAsciiSpaces(String value) {
        if (value == null) {
            return null;
        }
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == ' ') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == ' ') {
            end--;
        }
        return value.substring(start, end);
    }
}
