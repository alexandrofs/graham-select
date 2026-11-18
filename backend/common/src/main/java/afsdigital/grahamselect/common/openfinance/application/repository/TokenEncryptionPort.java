package afsdigital.grahamselect.common.openfinance.application.repository;

public interface TokenEncryptionPort {
    String encrypt(String plainText);

    String decrypt(String cipherText);
}
