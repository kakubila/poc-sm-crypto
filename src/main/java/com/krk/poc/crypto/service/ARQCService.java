package com.krk.poc.crypto.service;

import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

@Service
public class ARQCService {

    private static final String ALG = "DESede/ECB/NoPadding";
    private final byte[] imkac;

    public ARQCService(@Value("${crypto.imkac}") String hexImkac) throws Exception {
        this.imkac = Hex.decodeHex(hexImkac);
    }

    public String generateARQC(String pan, String atc, String unpredictableNumber, String transactionData) throws Exception {
        String inputData = pan + atc + unpredictableNumber + transactionData;
        byte[] dataBytes = padData(Hex.decodeHex(inputData));

        SecretKeySpec keySpec = new SecretKeySpec(expandKey(imkac), "DESede");
        Cipher cipher = Cipher.getInstance(ALG);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);

        byte[] cryptogram = cipher.doFinal(dataBytes);
        return Hex.encodeHexString(truncateArqc(cryptogram)).toUpperCase();
    }

    public boolean validateARQC(String arqc, String pan, String atc, String unpredictableNumber, String transactionData) throws Exception {
        String generated = generateARQC(pan, atc, unpredictableNumber, transactionData);
        return generated.equalsIgnoreCase(arqc);
    }

    private byte[] expandKey(byte[] key16) {
        byte[] key24 = new byte[24];
        System.arraycopy(key16, 0, key24, 0, 16);
        System.arraycopy(key16, 0, key24, 16, 8);
        return key24;
    }

    private byte[] padData(byte[] data) {
        int remainder = data.length % 8;
        if (remainder == 0) return data;
        byte[] padded = new byte[data.length + (8 - remainder)];
        System.arraycopy(data, 0, padded, 0, data.length);
        return padded;
    }

    private byte[] truncateArqc(byte[] cryptogram) {
        byte[] arqc = new byte[8];
        System.arraycopy(cryptogram, 0, arqc, 0, 8);
        return arqc;
    }
}
