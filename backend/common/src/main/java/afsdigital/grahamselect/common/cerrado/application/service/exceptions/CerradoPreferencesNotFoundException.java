package afsdigital.grahamselect.common.cerrado.application.service.exceptions;

/**
 * Excecao lancada quando o usuario ainda nao possui preferencias do Diagrama do Cerrado salvas.
 */
public class CerradoPreferencesNotFoundException extends RuntimeException {

    public CerradoPreferencesNotFoundException(String message) {
        super(message);
    }
}
