package app.kairos.backup

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

class DriveException(val code: Int, message: String) : IOException(message)

/**
 * Minimal Google Drive v3 client for the app data folder: a hidden, per-app folder
 * in the user's own Drive. With the drive.appdata scope Kairos can see nothing else
 * in their Drive. This is the only network code in Kairos. Blocking; call off main.
 * (Adapted from Stagira's DriveClient.)
 */
class DriveClient(private val accessToken: String) {

    fun findFile(): String? {
        val q = URLEncoder.encode("name = '$FILE_NAME'", "UTF-8")
        val body = request("GET", "$API/files?spaces=appDataFolder&fields=files(id,modifiedTime)&q=$q")
        val files = JSONObject(body).getJSONArray("files")
        return if (files.length() == 0) null else files.getJSONObject(0).getString("id")
    }

    fun download(id: String): String = request("GET", "$API/files/$id?alt=media")

    fun upload(content: String) {
        val existing = findFile()
        if (existing != null) {
            request("PATCH", "$UPLOAD/files/$existing?uploadType=media", content, "application/json; charset=UTF-8")
            return
        }
        val boundary = "kairos-" + UUID.randomUUID().toString().replace("-", "")
        val metadata = JSONObject().put("name", FILE_NAME).put("parents", JSONArray().put("appDataFolder"))
        val body = "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n" +
            "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$content\r\n--$boundary--"
        request("POST", "$UPLOAD/files?uploadType=multipart", body, "multipart/related; boundary=$boundary")
    }

    private fun request(method: String, url: String, body: String? = null, type: String? = null): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            if (method == "PATCH") {
                conn.requestMethod = "POST"
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            } else {
                conn.requestMethod = method
            }
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", type)
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw DriveException(code, "Drive returned $code ${err.take(200)}")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val FILE_NAME = "kairos-backup.json"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
    }
}
