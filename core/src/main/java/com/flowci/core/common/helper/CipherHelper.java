/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.common.helper;

import com.flowci.common.helper.StringHelper;
import com.flowci.domain.SimpleKeyPair;
import com.flowci.common.exception.StatusException;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

public abstract class CipherHelper {

    public static abstract class RSA {

        private static final String RsaPrivateKeyStart = "-----BEGIN RSA PRIVATE KEY-----";

        private static final String RsaPrivateKeyEnd = "-----END RSA PRIVATE KEY-----";

        public static boolean isPrivateKey(String src) {
            src = src.trim();
            return src.startsWith(RsaPrivateKeyStart) && src.endsWith(RsaPrivateKeyEnd);
        }

        public static SimpleKeyPair gen(String email) {
            try (ByteArrayOutputStream pubKeyOS = new ByteArrayOutputStream()) {
                try (ByteArrayOutputStream prvKeyOS = new ByteArrayOutputStream()) {
                    JSch jsch = new JSch();
                    SimpleKeyPair rsa = new SimpleKeyPair();

                    KeyPair kpair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 2048);
                    kpair.writePrivateKey(prvKeyOS);
                    kpair.writePublicKey(pubKeyOS, email);

                    rsa.setPublicKey(pubKeyOS.toString());
                    rsa.setPrivateKey(prvKeyOS.toString());

                    kpair.dispose();
                    return rsa;
                }
            } catch (IOException | JSchException e) {
                throw new StatusException("Unable to generate RSA key pair");
            }
        }

        public static String fingerprintMd5(String publicKey) throws NoSuchAlgorithmException {
            String derFormat = publicKey.split(" ")[1].trim();
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digest = messageDigest.digest(Base64.getDecoder().decode(derFormat));
            final StringBuilder toRet = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                if (i != 0) {
                    toRet.append(":");
                }

                int b = digest[i] & 0xff;
                String hex = Integer.toHexString(b);

                if (hex.length() == 1) {
                    toRet.append("0");
                }
                toRet.append(hex);
            }
            return toRet.toString();
        }

        /**
         * from <type><space><base64data><space><comment> to public key
         */
        private static PublicKey toPublicKey(String sshPublicKey)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            String[] line = sshPublicKey.trim().split(" ", 3);
            String type = line[0];
            String content = line[1];

            ByteBuffer buf = ByteBuffer.wrap(Base64.getDecoder().decode(content));

            // format of decoded content is: <type><keyparams>
            // where type and each param is a DER string
            String decodedType = new String(readDERString(buf));
            if (!decodedType.equals(type)) {
                throw new IllegalArgumentException("expected " + type + ", got " + decodedType);
            }

            if (type.equals("ssh-rsa")) {
                BigInteger e = new BigInteger(readDERString(buf));
                BigInteger y = new BigInteger(readDERString(buf));
                return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(y, e));
            }

            throw new InvalidKeySpecException("Unknown key type '" + type + "'");
        }

        static byte[] readDERString(ByteBuffer buf) {
            int length = buf.getInt();
            if (length > 8192) {
                throw new IllegalArgumentException("DER String Length " + length + " > 8192");
            }
            byte[] bytes = new byte[length];
            buf.get(bytes);
            return bytes;
        }
    }

    public static abstract class AES {

        public static String encrypt(String source, String secret) {
            try {
                SecretKeySpec key = new SecretKeySpec(toBytes(secret), "AES");
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
                cipher.init(Cipher.ENCRYPT_MODE, key);

                byte[] bytes = cipher.doFinal(toBytes(source));
                return Base64.getEncoder().encodeToString(bytes);
            } catch (Throwable e) {
                return StringHelper.EMPTY;
            }
        }

        public static String decrypt(String encrypted, String secret) {
            try {
                SecretKeySpec key = new SecretKeySpec(toBytes(secret), "AES");

                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
                cipher.init(Cipher.DECRYPT_MODE, key);

                byte[] source = cipher.doFinal(Base64.getDecoder().decode(encrypted));
                return new String(source);
            } catch (Throwable e) {
                return StringHelper.EMPTY;
            }
        }
    }

    private static byte[] toBytes(String val) {
        return val.getBytes(StandardCharsets.UTF_8);
    }
}
