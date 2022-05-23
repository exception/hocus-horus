package com.joinhocus.horus.account;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

import java.util.Date;
import java.util.regex.Pattern;

public final class AccountAuth {

    private static final String SECRET = "<redacted>";
    private static final Algorithm HMAC = Algorithm.HMAC256(SECRET);
    private static final String ISSUER = "horus";

    public static final Argon2 ARGON_2 = Argon2Factory.create(
            Argon2Factory.Argon2Types.ARGON2id,
            32,
            64
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])");

    public static String getToken(UserAccount account) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withClaim("account", account.getUsernames().getName())
                .withIssuedAt(new Date())
                .sign(HMAC);
    }

    public static String getAccountFromJwt(String token) {
        JWTVerifier verifier = JWT.require(HMAC)
                .withIssuer(ISSUER)
                .build();

        DecodedJWT jwt = verifier.verify(token);
        Claim account = jwt.getClaim("account");
        return account.asString();
    }

    public static boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

}
