package afsdigital.grahamselect.api.dlq.application.service.exceptions;

public class DlqMessageNotFoundException extends RuntimeException {

    public DlqMessageNotFoundException(Long id) {
        super("Mensagem DLQ não encontrada com ID: " + id);
    }
}
