package afsdigital.grahamselect.api.tags.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SetAssetTagsRequest(
        @NotNull(message = "Lista de tagIds é obrigatória.")
        @Size(max = 100, message = "Máximo de 100 tags por ativo.")
        List<@NotNull(message = "tagId não pode ser nulo.") UUID> tagIds
) {
}
