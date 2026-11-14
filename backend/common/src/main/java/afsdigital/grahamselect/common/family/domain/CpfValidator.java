package afsdigital.grahamselect.common.family.domain;

import afsdigital.grahamselect.common.family.application.service.exceptions.InvalidCpfException;

public final class CpfValidator {

    private CpfValidator() {
    }

    public static String sanitize(String rawCpf) {
        if (rawCpf == null) {
            return "";
        }
        return rawCpf.replaceAll("\\D", "");
    }

    public static boolean isValid(String rawCpf) {
        String digits = sanitize(rawCpf);
        if (digits.length() != 11) {
            return false;
        }
        // Rejeita sequências repetidas como 00000000000, 11111111111, etc.
        if (digits.chars().distinct().count() == 1) {
            return false;
        }
        int firstCheck = calculateCheckDigit(digits, 9, 10);
        if (firstCheck != Character.getNumericValue(digits.charAt(9))) {
            return false;
        }
        int secondCheck = calculateCheckDigit(digits, 10, 11);
        return secondCheck == Character.getNumericValue(digits.charAt(10));
    }

    public static String validateAndSanitize(String rawCpf) {
        String digits = sanitize(rawCpf);
        if (!isValid(digits)) {
            throw new InvalidCpfException("CPF inválido. Informe 11 dígitos numéricos válidos.");
        }
        return digits;
    }

    public static String mask(String cpfDigits) {
        String digits = sanitize(cpfDigits);
        if (digits.length() != 11) {
            return "***.***.***-**";
        }
        return "***." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-**";
    }

    private static int calculateCheckDigit(String digits, int length, int initialWeight) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            int num = Character.getNumericValue(digits.charAt(i));
            sum += num * (initialWeight - i);
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }
}

