/*package com.nis.modules.romote.entity;

import com.nis.modules.romote.util.CommonUtils;
import com.nis.modules.romote.util.Constants;
import com.nis.modules.romote.util.IOUtils;
import com.nis.modules.romote.util.RSAUtils;

import java.io.File;
import java.util.Map;

public  class Auth {
    public static String publicKey = null;
    public static String privateKey = null;
    public static String KEY_PATH = null;
    public static String PRIVATE_KEY_PATH = null;
    public static String PUBLIC_KEY_PATH = null;

    private static void generateKey() {
        if (CommonUtils.isEmpty(publicKey, privateKey)) {
            try {
                File keyPath = new File(KEY_PATH);
                if (!keyPath.exists()) {
                    keyPath.mkdirs();
                }
                Map<String, Object> keyMap = RSAUtils.genKeyPair();
                publicKey = RSAUtils.getPublicKey(keyMap);
                privateKey = RSAUtils.getPrivateKey(keyMap);
                File pubFile = new File(getPublicKeyPath());
                File prvFile = new File(getPrivateKeyPath());
                IOUtils.writeText(pubFile, publicKey, Constants.CHARSET_UTF8);
                IOUtils.writeText(prvFile, privateKey, Constants.CHARSET_UTF8);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("init RSA'publicKey and privateKey error!");
            }
        }
    }

    public static String getPublicKey() {
        return getKey(KeyType.PUBLIC);
    }

    public static String getPrivateKey() {
        return getKey(KeyType.PRIVATE);
    }

    public static String getKey(KeyType type) {
        File file = new File(type.equals(KeyType.PUBLIC) ? getPublicKeyPath() : getPrivateKeyPath());
        if (file.exists()) {
            switch (type) {
                case PUBLIC:
                    publicKey = IOUtils.readText(file, Constants.CHARSET_UTF8);
                    if (CommonUtils.isEmpty(publicKey)) {
                        generateKey();
                    }
                    break;
                case PRIVATE:
                    privateKey = IOUtils.readText(file, Constants.CHARSET_UTF8);
                    if (CommonUtils.isEmpty(privateKey)) {
                        generateKey();
                    }
                    break;
            }
        } else {
            generateKey();
        }
        return type.equals(KeyType.PUBLIC) ? publicKey : privateKey;
    }

    public static String getKeyPath() {
        if (KEY_PATH == null) {
            KEY_PATH = Constants.NZ_USER_HOME;
        }
        return KEY_PATH;
    }

    private static String getPrivateKeyPath() {
        PRIVATE_KEY_PATH = getKeyPath() + File.separator + "id_rsa";
        return PRIVATE_KEY_PATH;
    }

    private static String getPublicKeyPath() {
        PUBLIC_KEY_PATH = getPrivateKeyPath() + ".pub";
        return PUBLIC_KEY_PATH;
    }

    enum KeyType {
        PUBLIC, PRIVATE
    }

}
*/