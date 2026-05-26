package de.healthforge.common

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class MailService(
    private val sender: JavaMailSender,
    @Value("\${spring.mail.username:noreply@healthforge.endgear.de}") private val from: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendVerificationEmail(to: String, link: String) {
        send(
            to = to,
            subject = "HealthForge — E-Mail bestätigen",
            body = """
                Willkommen bei HealthForge!

                Bitte bestätige deine E-Mail-Adresse, indem du folgenden Link öffnest:

                $link

                Der Link ist 24 Stunden gültig. Wenn du diese Mail nicht erwartet hast, ignoriere sie einfach.

                Liebe Grüße,
                HealthForge
            """.trimIndent(),
        )
    }

    fun sendPasswordResetEmail(to: String, link: String) {
        send(
            to = to,
            subject = "HealthForge — Passwort zurücksetzen",
            body = """
                Du hast eine Passwort-Zurücksetzung angefordert.

                Klicke auf folgenden Link, um ein neues Passwort zu vergeben:

                $link

                Der Link ist 1 Stunde gültig. Wenn du das nicht warst, ignoriere diese Mail.

                HealthForge
            """.trimIndent(),
        )
    }

    private fun send(to: String, subject: String, body: String) {
        try {
            val msg = SimpleMailMessage()
            msg.from = from
            msg.setTo(to)
            msg.subject = subject
            msg.text = body
            sender.send(msg)
            log.info("Sent mail to {} subject='{}'", to, subject)
        } catch (e: Exception) {
            log.error("Failed to send mail to {} subject='{}'", to, subject, e)
        }
    }
}
