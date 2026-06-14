package afsdigital.grahamselect.goals.application.service.exceptions;

public class FinancialGoalNotFoundException extends RuntimeException {

    public FinancialGoalNotFoundException(String message) {
        super(message);
    }
}
