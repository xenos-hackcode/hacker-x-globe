package com.xhacker.cedal.services

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// Round 3 (2026-08-10) self-hosted mode - the free alternative to
// hostingMode "cedal". Generates a small platform client the user runs
// themselves; its only cedal-facing call is POST /bots/{id}/converse
// (BotBrainService.converse, via BotRoutes' secretToken-gated path) - the
// AI brain still lives on cedal-server either way, only the platform
// connection (Telegram/WhatsApp) runs on the user's own machine.
object BotTemplateService {

    fun buildZip(botId: String, secretToken: String, serverUrl: String, botType: String, telegramToken: String?, whatsappPhoneNumberId: String?, whatsappAccessToken: String?): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            val needsTelegram = botType == "telegram" || botType == "both"
            val needsWhatsapp = botType == "whatsapp" || botType == "both"

            if (needsTelegram) {
                writeEntry(zip, "telegram_bot.py", telegramTemplate(botId, secretToken, serverUrl, telegramToken.orEmpty()))
            }
            if (needsWhatsapp) {
                writeEntry(zip, "whatsapp_bot.py", whatsappTemplate(botId, secretToken, serverUrl, whatsappPhoneNumberId.orEmpty(), whatsappAccessToken.orEmpty()))
            }
            writeEntry(zip, "requirements.txt", "requests\n" + if (needsWhatsapp) "flask\n" else "")
            writeEntry(zip, "README.txt", readme(needsTelegram, needsWhatsapp))
        }
        return out.toByteArray()
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray())
        zip.closeEntry()
    }

    private fun readme(needsTelegram: Boolean, needsWhatsapp: Boolean) = buildString {
        appendLine("Cedal Bot - self-hosted platform client")
        appendLine("=========================================")
        appendLine()
        appendLine("This talks to your bot's AI brain on cedal-server for you - it never")
        appendLine("thinks for itself. All it does is relay messages between Telegram/WhatsApp")
        appendLine("and cedal-server's /bots/{id}/converse endpoint.")
        appendLine()
        appendLine("Setup: pip install -r requirements.txt")
        appendLine()
        if (needsTelegram) {
            appendLine("Telegram: run `python telegram_bot.py`. It long-polls Telegram for new")
            appendLine("messages - just needs to keep running somewhere (a laptop, a small VPS,")
            appendLine("a Raspberry Pi). No public IP or domain needed.")
            appendLine()
        }
        if (needsWhatsapp) {
            appendLine("WhatsApp: run `python whatsapp_bot.py`. This is a webhook receiver, so")
            appendLine("Meta needs to be able to reach it over HTTPS - it needs a public URL")
            appendLine("(e.g. ngrok while testing, or a real domain for production). Set that URL")
            appendLine("plus the verify token printed when the script starts as your app's")
            appendLine("webhook in the Meta developer dashboard.")
            appendLine()
        }
        appendLine("Want Cedal to run this for you instead, with nothing to host yourself?")
        appendLine("Ask the admin about cedal-hosted mode (premium).")
    }

    private fun telegramTemplate(botId: String, secretToken: String, serverUrl: String, telegramToken: String) = """
        |import time
        |import requests
        |
        |BOT_ID = "$botId"
        |SECRET_TOKEN = "$secretToken"
        |SERVER_URL = "$serverUrl"
        |TELEGRAM_TOKEN = "$telegramToken"
        |
        |def converse(chat_id, message):
        |    resp = requests.post(
        |        f"{SERVER_URL}/bots/{BOT_ID}/converse",
        |        headers={"Authorization": f"Bearer {SECRET_TOKEN}"},
        |        json={"chatId": str(chat_id), "message": message},
        |        timeout=30,
        |    )
        |    resp.raise_for_status()
        |    return resp.json()["reply"]
        |
        |def main():
        |    print("Cedal Telegram bot running - polling for messages...")
        |    offset = None
        |    while True:
        |        params = {"timeout": 30}
        |        if offset is not None:
        |            params["offset"] = offset
        |        try:
        |            updates = requests.get(
        |                f"https://api.telegram.org/bot{TELEGRAM_TOKEN}/getUpdates",
        |                params=params, timeout=35,
        |            ).json().get("result", [])
        |        except Exception as e:
        |            print(f"Poll error: {e}")
        |            time.sleep(3)
        |            continue
        |        for update in updates:
        |            offset = update["update_id"] + 1
        |            message = update.get("message")
        |            if not message or "text" not in message:
        |                continue
        |            chat_id = message["chat"]["id"]
        |            try:
        |                reply = converse(chat_id, message["text"])
        |            except Exception as e:
        |                reply = "Sorry, something went wrong on my end."
        |                print(f"Converse error: {e}")
        |            requests.post(
        |                f"https://api.telegram.org/bot{TELEGRAM_TOKEN}/sendMessage",
        |                json={"chat_id": chat_id, "text": reply}, timeout=15,
        |            )
        |
        |if __name__ == "__main__":
        |    main()
        |""".trimMargin()

    private fun whatsappTemplate(botId: String, secretToken: String, serverUrl: String, phoneNumberId: String, accessToken: String) = """
        |from flask import Flask, request
        |import requests
        |
        |BOT_ID = "$botId"
        |SECRET_TOKEN = "$secretToken"
        |SERVER_URL = "$serverUrl"
        |PHONE_NUMBER_ID = "$phoneNumberId"
        |ACCESS_TOKEN = "$accessToken"
        |VERIFY_TOKEN = "$secretToken"
        |
        |app = Flask(__name__)
        |
        |def converse(chat_id, message):
        |    resp = requests.post(
        |        f"{SERVER_URL}/bots/{BOT_ID}/converse",
        |        headers={"Authorization": f"Bearer {SECRET_TOKEN}"},
        |        json={"chatId": str(chat_id), "message": message},
        |        timeout=30,
        |    )
        |    resp.raise_for_status()
        |    return resp.json()["reply"]
        |
        |def send_message(to, text):
        |    requests.post(
        |        f"https://graph.facebook.com/v21.0/{PHONE_NUMBER_ID}/messages",
        |        headers={"Authorization": f"Bearer {ACCESS_TOKEN}"},
        |        json={"messaging_product": "whatsapp", "to": to, "type": "text", "text": {"body": text}},
        |        timeout=15,
        |    )
        |
        |@app.route("/webhook", methods=["GET"])
        |def verify():
        |    if request.args.get("hub.verify_token") == VERIFY_TOKEN:
        |        return request.args.get("hub.challenge", ""), 200
        |    return "Forbidden", 403
        |
        |@app.route("/webhook", methods=["POST"])
        |def incoming():
        |    data = request.get_json(force=True, silent=True) or {}
        |    for entry in data.get("entry", []):
        |        for change in entry.get("changes", []):
        |            for message in change.get("value", {}).get("messages", []):
        |                if message.get("type") != "text":
        |                    continue
        |                sender = message["from"]
        |                text = message["text"]["body"]
        |                try:
        |                    reply = converse(sender, text)
        |                except Exception as e:
        |                    reply = "Sorry, something went wrong on my end."
        |                    print(f"Converse error: {e}")
        |                send_message(sender, reply)
        |    return "OK", 200
        |
        |if __name__ == "__main__":
        |    print(f"Cedal WhatsApp bot running - verify token: {VERIFY_TOKEN}")
        |    app.run(host="0.0.0.0", port=5000)
        |""".trimMargin()
}
