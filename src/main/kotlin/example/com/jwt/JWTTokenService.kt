package example.com.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import example.com.plugins.JWTTokenState
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.FileReader
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.*
import kotlin.time.Duration


class JWTTokenService(
    private val filePath: String,
    private val tokenExpiration: Duration,
) {

    // Key Pair for extracting public and private key info.
    private val keyPair: PEMKeyPair = PEMParser(FileReader(filePath)).readObject() as PEMKeyPair

    private fun getECPrivateKey(): ECPrivateKey {

        val privateKeyInfo = keyPair.privateKeyInfo as PrivateKeyInfo
        val privateKey = JcaPEMKeyConverter().getPrivateKey(privateKeyInfo)
        return privateKey as ECPrivateKey
    }

    // SignedJWT.parse will throw an exception if the input string is not valid.
    // Return null if there is an exception parsing the input string.
    private fun parseOrNull(token: String): SignedJWT? = try {
        SignedJWT.parse(token)
    } catch (e: Exception) {
        null
    }

    // verify the JWT using the public key.
    fun verifyJwt(token: String): JWTTokenState {

        val publicKey =
            JcaPEMKeyConverter().getPublicKey(keyPair.publicKeyInfo)
        // Create an ECKey instance with the public key
        val ecJWK = ECKey.Builder(Curve.P_256, publicKey as ECPublicKey?).build()

        // Create a verifier with the public EC key
        val verifier: JWSVerifier = ECDSAVerifier(ecJWK)

        // Parse the JWT from the string
        val signedJWT = parseOrNull(token)

        val now = Date()

        val tokenExpired: Boolean = null != signedJWT && signedJWT.jwtClaimsSet.expirationTime.before(now)

        val validAndNotExpired: Boolean = null != signedJWT && signedJWT.verify(verifier) && !tokenExpired

        // Verify the JWT
        return when {
            validAndNotExpired -> JWTTokenState.VALID
            tokenExpired -> JWTTokenState.EXPIRED
            else -> JWTTokenState.NOT_VALID
        }
    }

    fun generateToken(): String {
        val signer: JWSSigner = ECDSASigner(getECPrivateKey())

        // additional data can be encoded into the token.
        val claimSet = JWTClaimsSet.Builder()
            .subject("testing")
            .issuer("http://localhost")
            .expirationTime(Date(System.currentTimeMillis() + (tokenExpiration.inWholeMilliseconds)))
            .build()

        val header = JWSHeader.Builder(JWSAlgorithm.ES256)
            .keyID("${UUID.randomUUID()}")
            .build()

        val signedJWT = SignedJWT(header, claimSet)
        signedJWT.sign(signer)

        return signedJWT.serialize()
    }

}
