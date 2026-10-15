package com.example.util

import com.example.data.model.Belt
import com.example.data.model.JudoGroup
import org.json.JSONObject

data class ExtractedMemberInfo(
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val phone: String = "",
    val notes: String = "",
    val qrCode: String = "",
    val cardNumber: String = "",
    val season: String = "2024-2025",
    val photoUrl: String = "",
    val group: JudoGroup = JudoGroup.POUSSINS,
    val belt: Belt = Belt.BLANCHE,
    val rawScannedData: String = ""
)

object CardDataParser {
    fun parseCardData(raw: String): ExtractedMemberInfo {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ExtractedMemberInfo()

        // 1. Try parsing JSON format
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val json = JSONObject(trimmed)
                val firstName = json.optString("firstName", json.optString("prenom", ""))
                val lastName = json.optString("lastName", json.optString("nom", ""))
                val birthDate = json.optString("birthDate", json.optString("dateNaissance", ""))
                val phone = json.optString("phone", json.optString("telephone", ""))
                val cardNumber = json.optString("cardNumber", json.optString("licence", json.optString("numero", "")))
                val qrCode = json.optString("qrCode", if (cardNumber.isNotBlank()) "JCS-$cardNumber" else "")
                val beltStr = json.optString("belt", json.optString("ceinture", "BLANCHE"))
                val groupStr = json.optString("group", json.optString("categorie", ""))
                val season = json.optString("season", json.optString("saison", "2024-2025"))
                val photoUrl = json.optString("photoUrl", json.optString("photo", ""))
                val notes = json.optString("notes", "")

                val group = if (groupStr.isNotBlank()) JudoGroup.fromString(groupStr)
                    else if (birthDate.isNotBlank()) JudoGroup.fromBirthDate(birthDate)
                    else JudoGroup.POUSSINS

                return ExtractedMemberInfo(
                    firstName = firstName,
                    lastName = lastName,
                    birthDate = birthDate,
                    phone = phone,
                    notes = notes,
                    qrCode = qrCode,
                    cardNumber = cardNumber,
                    season = season,
                    photoUrl = photoUrl,
                    group = group,
                    belt = Belt.fromString(beltStr),
                    rawScannedData = trimmed
                )
            } catch (_: Exception) {}
        }

        // 2. Try parsing Delimited Key-Value strings (e.g., KEY:VALUE;KEY:VALUE or KEY=VALUE)
        if (trimmed.contains(";") || trimmed.contains("|")) {
            val delimiter = if (trimmed.contains(";")) ";" else "|"
            val parts = trimmed.split(delimiter)
            val map = mutableMapOf<String, String>()
            for (part in parts) {
                val kv = part.split(":", "=")
                if (kv.size == 2) {
                    map[kv[0].trim().uppercase()] = kv[1].trim()
                }
            }
            if (map.isNotEmpty()) {
                val firstName = map["FIRSTNAME"] ?: map["PRENOM"] ?: ""
                val lastName = map["LASTNAME"] ?: map["NOM"] ?: ""
                val birthDate = map["BIRTHDATE"] ?: map["DOB"] ?: map["NAISSANCE"] ?: ""
                val phone = map["PHONE"] ?: map["TEL"] ?: ""
                val cardNumber = map["CARD"] ?: map["LICENCE"] ?: map["ID"] ?: ""
                val qr = map["QR"] ?: if (cardNumber.isNotBlank()) "JCS-$cardNumber" else ""
                val beltStr = map["BELT"] ?: map["CEINTURE"] ?: "BLANCHE"
                val season = map["SEASON"] ?: map["SAISON"] ?: "2024-2025"

                val group = if (birthDate.isNotBlank()) JudoGroup.fromBirthDate(birthDate) else JudoGroup.POUSSINS

                return ExtractedMemberInfo(
                    firstName = firstName,
                    lastName = lastName,
                    birthDate = birthDate,
                    phone = phone,
                    cardNumber = cardNumber,
                    qrCode = qr,
                    season = season,
                    group = group,
                    belt = Belt.fromString(beltStr),
                    rawScannedData = trimmed
                )
            }
        }

        // 3. Fallback for Simple Code / Card Number (e.g., JCS-0012, 123456, etc.)
        val cleanNumber = trimmed.removePrefix("JCS-").removePrefix("MEM-")
        return ExtractedMemberInfo(
            qrCode = if (trimmed.startsWith("JCS-")) trimmed else "JCS-$cleanNumber",
            cardNumber = cleanNumber,
            rawScannedData = trimmed
        )
    }
}
