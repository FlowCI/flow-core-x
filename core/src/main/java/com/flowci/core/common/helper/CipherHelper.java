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

import com.flowci.domain.SimpleKeyPair;
import com.flowci.exception.StatusException;
import com.flowci.util.StringHelper;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.NotImplementedException;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;

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

        public static String encrypt(String source, String sshPublicKey) {
            try {
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE, toPublicKey(sshPublicKey));

                byte[] raw = cipher.doFinal(toBytes(source));
                return Base64.getEncoder().encodeToString(raw);
            } catch (Throwable e) {
                return StringHelper.EMPTY;
            }
        }

        public static String decrypt(String encrypted, String privateKey) {
            try {
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.DECRYPT_MODE, toPrivateKey(privateKey));

                byte[] decoded = Base64.getDecoder().decode(encrypted);
                byte[] raw = cipher.doFinal(decoded);
                return new String(raw);
            } catch (Throwable e) {
                return StringHelper.EMPTY;
            }
        }

        public static String fingerprintMd5(String publicKey) {
            throw new NotImplementedException();
        }

        private static PrivateKey toPrivateKey(String key)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            String content = key.replaceAll("\\n", "").replace(RsaPrivateKeyStart, "").replace(RsaPrivateKeyEnd, "");
            byte[] bytes = Base64.getDecoder().decode(content);

            DerInputStream derReader = new DerInputStream(bytes);
            DerValue[] seq = derReader.getSequence(0);

            // skip version seq[0];
            BigInteger modulus = seq[1].getBigInteger();
            BigInteger publicExp = seq[2].getBigInteger();
            BigInteger privateExp = seq[3].getBigInteger();
            BigInteger prime1 = seq[4].getBigInteger();
            BigInteger prime2 = seq[5].getBigInteger();
            BigInteger exp1 = seq[6].getBigInteger();
            BigInteger exp2 = seq[7].getBigInteger();
            BigInteger crtCoef = seq[8].getBigInteger();

            RSAPrivateCrtKeySpec keySpec =
                new RSAPrivateCrtKeySpec(modulus, publicExp, privateExp, prime1, prime2, exp1, exp2, crtCoef);

            return keyFactory.generatePrivate(keySpec);
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
