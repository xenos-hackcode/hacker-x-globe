package com.xhacker.cedal.services

import com.xhacker.cedal.db.PhoneVerifications
import com.xhacker.cedal.db.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.security.SecureRandom
import java.util.UUID

// Settings > More > Security - "1 account per phone number" (see
// Users.phoneNumber's doc comment). Real SMS delivery via SmsService/Twilio
// when configured; falls back to the same dev "log + echo the code back"
// pattern AuthService already uses for email, so this still works locally
// without Twilio credentials.
object SecurityService {
    private const val CODE_TTL_MS = 10L * 60 * 1000 // 10 minutes
    private val random = SecureRandom()

    private fun generateCode(): String = (100000 + random.nextInt(900000)).toString()

    // Loose E.164-ish check - a leading + and 8-15 digits. Real validity
    // (line actually exists, can receive SMS) is proven by the code arriving,
    // not by format alone. Also used by AuthService.signup - phone number is
    // now a required signup field, not just a later Settings add-on.
    fun isValidPhoneFormat(phone: String): Boolean =
        Regex("^\\+[1-9]\\d{7,14}$").matches(phone)

    // Returns the dev code IF Twilio isn't configured (so the app can show
    // it directly, matching AuthService's SignupResponse.devVerificationCode
    // pattern) - null once real SMS delivery is live, since the code
    // shouldn't be visible anywhere but the actual text message.
    suspend fun requestPhoneCode(userId: String, phoneNumber: String): String? {
        val trimmed = phoneNumber.trim()
        if (!isValidPhoneFormat(trimmed)) throw AuthException("Enter a valid phone number, e.g. +14155551234")
        val uid = UUID.fromString(userId)

        transaction {
            val takenByAnother = Users.selectAll()
                .where { (Users.phoneNumber eq trimmed) and (Users.phoneVerified eq true) and (Users.id neq uid) }
                .any()
            if (takenByAnother) throw AuthException("This phone number is already linked to another account")
            // Godmode "Ban" permanently blocks this number from ever being
            // verified onto ANY account again - see BannedIdentities' own
            // doc comment.
            val bannedPhone = com.xhacker.cedal.db.BannedIdentities.selectAll()
                .where { com.xhacker.cedal.db.BannedIdentities.phoneNumber eq trimmed }
                .any()
            if (bannedPhone) throw AuthException("This phone number has been banned and can't be used on Cedal again")
        }

        val code = generateCode()
        val now = System.currentTimeMillis()
        transaction {
            PhoneVerifications.deleteWhere { PhoneVerifications.userId eq uid }
            PhoneVerifications.insert {
                it[PhoneVerifications.userId] = uid
                it[PhoneVerifications.phoneNumber] = trimmed
                it[PhoneVerifications.code] = code
                it[expiresAt] = now + CODE_TTL_MS
            }
        }

        val sent = SmsService.sendSms(trimmed, "Your Cedal verification code is $code. It expires in 10 minutes.")
        println("[cedal-server] Phone verification code for $trimmed: $code (sms sent: $sent)")
        return if (sent) null else code
    }

    fun verifyPhoneCode(userId: String, code: String): Unit = transaction {
        val uid = UUID.fromString(userId)
        val row = PhoneVerifications.selectAll().where { PhoneVerifications.userId eq uid }.firstOrNull()
            ?: throw AuthException("No pending verification for this account - request a code first")
        if (row[PhoneVerifications.code] != code) throw AuthException("Invalid code")
        if (row[PhoneVerifications.expiresAt] < System.currentTimeMillis()) throw AuthException("Code expired - request a new one")
        val phone = row[PhoneVerifications.phoneNumber]

        val takenByAnother = Users.selectAll()
            .where { (Users.phoneNumber eq phone) and (Users.phoneVerified eq true) and (Users.id neq uid) }
            .any()
        if (takenByAnother) throw AuthException("This phone number is already linked to another account")

        Users.update({ Users.id eq uid }) {
            it[Users.phoneNumber] = phone
            it[Users.phoneVerified] = true
        }
        PhoneVerifications.deleteWhere { PhoneVerifications.userId eq uid }
        AchievementService.checkSecure(userId)
    }

    fun getPhoneStatus(userId: String): Pair<String?, Boolean> = transaction {
        val row = Users.selectAll().where { Users.id eq UUID.fromString(userId) }.first()
        row[Users.phoneNumber] to row[Users.phoneVerified]
    }
}
