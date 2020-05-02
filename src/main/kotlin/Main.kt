import io.reactivex.rxjava3.core.Observable
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.regex.Pattern
import kotlin.random.Random

const val BAIDU_API = "http://api.fanyi.baidu.com/api/trans/vip/translate"
const val APPID = "百度APPID"
const val KEY = "百度KEy"
const val USER_AGENT =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36"
const val SRC_DIR = "src/srcFiles"
const val TARGET_DIR = "src/targetFiles"

fun main() {
    val apiType = Translate.ApiType.BAIDU
    Translate(apiType).apply {
        File(SRC_DIR).list()?.forEach {
            println("$it ${"-".repeat(30)}")
            switchParse(SRC_DIR, it)
        }
    }
}

class Translate(private val apiType: ApiType) {
    enum class ApiType {
        BAIDU
    }

    /**
     * 匹配中文，如果有翻译则跳过
     */
    private val p: Pattern = Pattern.compile("[\u4e00-\u9fa5]")

    data class MapData(
        val nameJson: JSONObject,
        val nameBefore: String,
        val nameOrigin: String,
        val nextJson: JSONObject? = null,
        val nextBefore: String,
        val nextOrigin: String = ""
    )

    private fun getJson(path: String): StringBuilder {
        try {
            val sb = StringBuilder()
            val br = File(path).inputStream()
            val temp = ByteArray(1024)
            var len: Int
            do {
                len = br.read(temp)
                if (len > 0) {
                    sb.append(String(temp, 0, len))
                }
            } while (len > 0)
            br.close()
            return sb
        } finally {

        }
    }

    fun getHttpUrl(content: String?): String {
        if (content.isNullOrBlank()) {
            return ""
        }
        val contentEnc = URLEncoder.encode(content, "UTF-8")
        val salt = Random.nextInt(55555)
        val key1 = "$APPID$content$salt$KEY"
        return "$BAIDU_API?q=$contentEnc&from=en&to=zh&appid=$APPID&salt=$salt&sign=${md5(key1)}"

    }

    @Throws(Exception::class)
    fun getResponse(path: String?): String {
        if (path.isNullOrBlank()) return ""
        val url = URL(path)
        val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.connectTimeout = 5 * 1000
        conn.readTimeout = 5 * 1000
        conn.useCaches = false
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connect()
        val statusCode = conn.responseCode
        if (statusCode == 200) {
            val s = streamToString(conn.inputStream)
            conn.disconnect()
            return s
        }
        conn.disconnect()
        return ""
    }

    private fun streamToString(iS: InputStream): String {
        val sb = java.lang.StringBuilder()
        val temp = ByteArray(1024)
        var len: Int
        do {
            len = iS.read(temp)
            if (len > 0) {
                sb.append(String(temp, 0, len))
            }
        } while (len > 0)
        iS.close()
        return sb.toString()
    }

    /**
     * md5加密字符串，百度API需要
     * md5使用后转成16进制变成32个字节
     */
    private fun md5(str: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val result = digest.digest(str.toByteArray())
        println("result${result.size}")
        return toHex(result)
    }

    private fun toHex(byteArray: ByteArray): String {
        //转成16进制后是32字节
        return with(StringBuilder()) {
            byteArray.forEach {
                val hex = it.toInt() and (0xFF)
                val hexStr = Integer.toHexString(hex)
                if (hexStr.length == 1) {
                    append("0").append(hexStr)
                } else {
                    append(hexStr)
                }
            }
            toString()
        }
    }

    fun switchParse(dir: String, fileName: String) {
        when (fileName) {
            "BasicBuffFile.json" -> {
                parseJson("$dir/BasicBuffFile.json", false, "Description")
            }
            "BasicItemFile.json" -> {
                parseJson("$dir/BasicItemFile.json", false, "Tooltip")
            }
            "BasicNPCFile.json" -> {
                parseJson("$dir/BasicNPCFile.json", true, "")
            }
            "BasicPrefixFile.json" -> {
                parseJson("$dir/BasicPrefixFile.json", true, "")
            }
            "BasicProjectileFile.json" -> {
                parseJson("$dir/BasicProjectileFile.json", true, "")
            }
            "LdstrFile.json" -> {

            }
            else -> return
        }
    }

    fun parseJson(path: String, isSkipNext: Boolean = false, nextName: String = "") {
        val s = getJson(path).toString()
        val jo = JSONObject(s)
        val k = jo.keys()
        while (k.hasNext()) {//NPC
            val jo1 = jo.getJSONObject(k.next())
            Observable.fromArray(jo1.keys())
                .map { map ->
                    val result = ArrayList<MapData>()
                    map.forEach {
                        val item = jo1.getJSONObject(it)
                        val nameJson = item.getJSONObject("Name")
                        val nextJson = if (isSkipNext) null else item.getJSONObject(nextName)
                        val first = nameJson.optString("Origin")
                        val second = nextJson?.optString("Origin") ?: ""
                        val originResult =
                            if (p.matcher(first).find()) "" else getResponse(getHttpUrl(first))
                        val nextResult =
                            if (p.matcher(second).find() || isSkipNext) "" else getResponse(
                                getHttpUrl(second)
                            )
                        result.add(
                            MapData(
                                nameJson,
                                first,
                                originResult,
                                nextJson,
                                second,
                                nextResult
                            )
                        )
                        if (apiType == ApiType.BAIDU) {
                            //百度Api限制1秒10次，这里单纯简单粗暴sleep 100ms
                            Thread.sleep(100)
                        }
                    }
                    result
                }
                .subscribe({ result ->
                    result.forEach {
                        if (it.nameOrigin.isNotBlank()) {
                            val sb = StringBuilder()
                            val arr = JSONArray(JSONObject(it.nameOrigin).optString("trans_result"))
                            for (item in 0 until arr.length()) {
                                sb.append(JSONObject(arr.get(item).toString()).optString("dst"))
                                if (item != arr.length() - 1) {
                                    sb.append("\n")
                                }
                            }
                            it.nameJson.put(
                                "Translation",
                                sb.append("\n" + it.nameBefore)
                            )
                        }
                        if (it.nextOrigin.isNotBlank()) {
                            val sb = StringBuilder()
                            val arr = JSONArray(JSONObject(it.nextOrigin).optString("trans_result"))
                            for (item in 0 until arr.length()) {
                                sb.append(JSONObject(arr.get(item).toString()).optString("dst"))
                                if (item != arr.length() - 1) {
                                    sb.append("\n")
                                }
                            }
                            it.nextJson?.put(
                                "Translation",
                                sb.append("\n" + it.nextBefore)
                            )

                        }
                    }
                    val joS = jo.toString()
                    File(TARGET_DIR, path.split("/").last()).printWriter().use {
                        it.println(joS)
                    }
                    println("Json --- Completed !\n $joS")
                }, { println(it) })
        }

    }
}


