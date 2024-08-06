import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import example.com.plugins.JWTTokenService
import example.com.plugins.JWTTokenState
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.FileReader
import java.security.interfaces.ECPrivateKey
import java.util.*
import kotlin.test.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class JWTTokenServiceTest {

    val jwtTokenService = JWTTokenService("unittest.key", 1.minutes)

    private val testKeyPair: PEMKeyPair = PEMParser(FileReader("unittest.key")).readObject() as PEMKeyPair


    // Generates an expired JWT Token.
    fun generateExpiredToken(): String {
        val privateKeyInfo = testKeyPair.privateKeyInfo as PrivateKeyInfo
        val privateKey = JcaPEMKeyConverter().getPrivateKey(privateKeyInfo)
        val ecKey = privateKey as ECPrivateKey

        val signer: JWSSigner = ECDSASigner(ecKey)

        // expiration time is expired.
        val claimSet = JWTClaimsSet.Builder()
            .subject("testing")
            .issuer("http://localhost")
            .expirationTime(Date(System.currentTimeMillis() - (2000)))
            .build()

        val header = JWSHeader.Builder(JWSAlgorithm.ES256)
            .keyID("${UUID.randomUUID()}")
            .build()

        val signedJWT = SignedJWT(header, claimSet)
        signedJWT.sign(signer)

        return signedJWT.serialize()
    }


    @Test
     fun `test token generates expecting VALID`() {
        val tokenState = jwtTokenService.verifyJwt(jwtTokenService.generateToken())
        assert(tokenState == JWTTokenState.VALID) {
            "Token state was not VALID was: $tokenState"
        }
     }

    @Test
    fun `test token verify with expired token expecting EXPIRED`() {
        val tokenState = jwtTokenService.verifyJwt(generateExpiredToken())
        assert(tokenState == JWTTokenState.EXPIRED) {
            "Token state was not EXPIRED was: $tokenState"
        }
    }

    @Test
    fun `test token verify passing non jwt token expecting NOT_VALID`() {
        val tokenState = jwtTokenService.verifyJwt("abcdefg")
        assert(tokenState == JWTTokenState.NOT_VALID) {
            "Token state was not NOT_VALID was: $tokenState"
        }
    }

    @Test
    fun `test token verify passing jwt token signed by different key expecting NOT_VALID`() {
        val tokenState = jwtTokenService.verifyJwt(JWTTokenService("unittest-alt.key", 10.hours).generateToken())
        assert(tokenState == JWTTokenState.NOT_VALID) {
            "Token state was not NOT_VALID was: $tokenState"
        }
    }
}