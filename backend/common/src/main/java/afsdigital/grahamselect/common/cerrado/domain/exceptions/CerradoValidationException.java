package afsdigital.grahamselect.common.cerrado.domain.exceptions;

/**
 * Excecao de dominio lancada quando regras de integridade ou limites do
 * Diagrama do Cerrado sao violados.
 */
public class CerradoValidationException extends RuntimeException {

    public CerradoValidationException(String message) {
        super(message);
    }

    public CerradoValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
