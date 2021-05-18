package com.android.server.spacemanager;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.security.interfaces.RSAPublicKey;
import java.nio.ByteBuffer;
import java.math.BigInteger;
import java.util.Base64;

import android.util.Log;

public class Crypto {
    
    // generate random hex string
    public static String random(int length) {
    	
        // generate secure random (binary format in bytes[] strcuture)
        SecureRandom secureRandom = new SecureRandom();
        byte bytes[] = new byte[length/2];
        secureRandom.nextBytes(bytes);
        
        // convert binary to hex and build it as one string
        StringBuilder random = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            random.append(String.format("%02X", bytes[i]));
        }
        
        // retrun the random value as hex in string format
        return random.toString();
    }

    // generate hash of the input using the specified hashing algorithm
    // available algorithms: MD5, SHA-1, SHA-224, SHA-256, SHA-384, SHA-512
    public static String hash(String input, String algorithm) {
        try {
            // create message digest object for the requested algorithm
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            
            // perform the message digest on the input text
            messageDigest.update(input.getBytes());
            byte[] digest = messageDigest.digest();
            
            // convert the digest to a string
            BigInteger bigInteger = new BigInteger(1, digest);
            String hash = bigInteger.toString(16);
            
            // return the hash value
            return hash;
        
        } catch(Exception e) {
            Log.e(Constants.TAG, "Error: Cannot produce the hash value. Error details: " + e.getMessage() + ". [Crypto.hash()]");
            return null;
        }
    }

    // TODO: create the message as required and ensure the signature verification works as expected
    public static boolean verifySignature(String message, String signature, String signatureAlgorithm, String publicKey) {

        // TODO: to be removed. This can be commented out to check the signature verfification successful process.
        message = "AHMED";
        signature = "Iq83iDnarYDuEjAeX1sy4zeJL4gBnLz7/dkzPoFRB3UUJJ7+X48iLWr6vO/z0vdw4OryFGi8WfV9VmiwD43gvLBZMDcDKRdBZdmH4okiRanEez4xZ+utI8wOaz+b84ODI15VGs80MvEtHlc4t/hFrCn9IzqWP8bvQVCTtErz7jvv0rTarUQYiYgN2HDrUk0A/GI/Ag43u0YQ4ouRVc/jAZL+eWLD9myq7r8Q+VbMbIG0AIXFx06E7I8A3OCPwBNQsSKvaRMR4wtSJInbzhhduSmyRVikwhdLm+7v0Q8FQj4Wt/NX5j0e5OI0kmkSTXsQ9h/uBD26c3Kw+tKS+76F+xMNnKoVNdGhSL5fmKGNEwOx2vXNsIZ/4pQm9F5dGLg1xXBysJ35Tzi35wEFiiycFKFIRqJ+8f9TkgBZtIK0P9qruUHxQPJPO7zNIPkB3Ddi20mSMvo+RgcwWjXBkgMGSqKOB6X+0kKXAW3pdtKHCtwNj02g3GUyf0XFAxXJeR6uFjUxzQM5PKtTASfDA01pnHCa4ROw+NbJFtqZlcBa5NERj9r8Iel4HQUTunUkfKr/uFnTd8UhLRCHxK3T1OBEXs56xRrvOzcFVCZUONEw9QyHe30qClACqUNq0aB171aFdMfKdR9Vkz2fJc8TjVoTIaMObc3FlRZCvD7+HwtCICw=";
        publicKey = "-----BEGIN PUBLIC KEY-----\nMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAyYWMVTKBaI7uktEZCs2c\nNXY9VkaJiOBvrT3ADGytI2lPek3RkxRS/00LcSxYVOONT7RWVbjI40L4rE3dqmi3\nWfS/l9HcaQ0QWporNHpOcBSSC7y3zVi1CPD8lxIljFrJKXAHXOGpgoNBCm1UpEQQ\nau4pciNpZgyZPa6P5NVydciYQchniw79SZzCPVCy3W49Pbe6OIkl/hyCv+OZI6fN\nG8YhNLFrL5vsycpTvLSWXl1bu1OkrGyf9UU4Tzv1Nipvfwivq/ypNs56iC+XzNXD\nxWNRm8DWogYslC/NovRtXMqteRE9wOzVV9MlLRjy0l4jgSraVHQU9KBLMLlh+i7y\nrkI8aTLqSbcrJYgtsQ6XiEPxvylAyEVrfYty41614P0Lvb7AgBXNQ+Jwp8oHYrfu\nOxWtOYKQbayMOrw3GuErWPL47+h6SushcsPeeOXehQjYSZJfVGWBy/kONjKg3B+v\nB74mVHpUx5CpXfGX16wy9BC01oLKURRPr9dVz0S8Yk9cFYlEmI5dPkPJJdN939+9\n9IZZIILRy5rSWd/QyYV9g/qzEHZjMnduCBY4sTn6dPZ8p2pjHAFaZClPJQxENlY2\nVYWcOuL6C3fpQ1KgiLGJuGLA9oJ6+VzsxNaR5FRZs5ISq+7JL7Wvmsrgxz30q1TU\nMrYW/xicIXhyR1l2xyCnx7sCAwEAAQ==\n-----END PUBLIC KEY-----\n";
        Log.d(Constants.TAG, "%%%% message: " + message);
        Log.d(Constants.TAG, "%%%% signature: " + signature);
        Log.d(Constants.TAG, "%%%% signatureAlgorithm: " + signatureAlgorithm);
        Log.d(Constants.TAG, "%%%% publicKey: " + publicKey);
        
        try {
            // read the public key in its required format
            publicKey = publicKey.replaceAll("\\n", "").replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
            
            // read the signature in its required format
            byte[] sig = Base64.getDecoder().decode(signature);

            // build the RSAPublicKey object
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));
            RSAPublicKey pubKey = (RSAPublicKey) keyFactory.generatePublic(keySpecX509);

            // create signer object
            Signature signer = Signature.getInstance(signatureAlgorithm); 
            
            // initialize signer for verification
            signer.initVerify(pubKey);

            // add the message to be verified
            signer.update(message.getBytes());

            // verify the signature
            boolean result = signer.verify(sig);

            Log.d(Constants.TAG, "Signature verification status: " + result);

            // return the result
            return result;

        } catch (Exception e) {
            Log.e(Constants.TAG, "Error: Cannot verify the signature. Error details: " + e.getMessage() + ". [Crypto.verifySignature()]");
            return false;
        }
    }
}