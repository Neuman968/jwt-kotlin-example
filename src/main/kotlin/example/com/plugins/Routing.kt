package example.com.plugins

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import example.com.jwt.JWTTokenService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.minutes

val jwtTokenService = JWTTokenService("testkey.key", 1.minutes)

enum class JWTTokenState {
    VALID,
    NOT_VALID,
    EXPIRED,
}

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        get("/qr") {
            call.respondImage(generateQRCode(jwtTokenService.generateToken())!!.toBufferedImage())
        }
        get("/token") {
            call.respondText(jwtTokenService.generateToken())
        }

        post("/verify") {
            call.respondText(jwtTokenService.verifyJwt(call.receiveText()).name)
        }
    }
}

suspend fun ApplicationCall.respondImage(image: BufferedImage) {
    respond(object : OutgoingContent.WriteChannelContent() {
        override val contentType = ContentType.Image.PNG
        override suspend fun writeTo(channel: ByteWriteChannel) {
            withContext(Dispatchers.IO) {
                ImageIO.write(image, "PNG", channel.toOutputStream())
            }
        }
    })
}

// Converts the zxing QR Code BitMatrix into a BufferedImage.
fun BitMatrix.toBufferedImage(): BufferedImage {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val rgb = (if (this[x, y]) 0 else 1) * 0xFFFFFF
            image.setRGB(x, y, rgb)
        }
    }

    return image
}

// Returns a BitMatrix QR Code based on input string.
fun generateQRCode(text: String): BitMatrix? {
    val qrCodeWriter = QRCodeWriter()
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L)
    return qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 350, 350, hints)
}
