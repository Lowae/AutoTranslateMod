package main

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
const val USER_AGENT =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36"
const val SRC_DIR = "/srcFiles"
const val TARGET_DIR = "/targetFiles"

fun main(args: Array<String>) {
    val apiType = Translate.ApiType.BAIDU
    val dir = Main()::class.java.protectionDomain.codeSource.location.path.run {
        substring(1, this.lastIndexOf('/'))
    }
    val srcDir = "$dir$SRC_DIR"
    val targetDir = "$dir$TARGET_DIR"
    println("输入APPID:")
    val appid = readLine()
    println("输入密钥:")
    val key = readLine()
    println("输入文件夹：$srcDir")
    println("输出文件夹：$targetDir")
    Translate(apiType, srcDir, targetDir, appid.toString(), key.toString()).apply {
        File(srcDir).list()?.forEach {
            println("$it ${"-".repeat(30)}")
            switchParse(it)
        }
    }
}

class Main

class Translate(
    private val apiType: ApiType,
    private val srcDir: String,
    private val targetDir: String,
    private val APPID: String = "",
    private val KEY: String =""
) {
    enum class ApiType {
        BAIDU
    }

    /**
     * 匹配中文，如果有翻译则跳过
     */
    private val p: Pattern = Pattern.compile("[\u4e00-\u9fa5]")

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

    fun switchParse(fileName: String) {
        when (fileName) {
            "BasicBuffFile.json" -> {
                parseJson("$srcDir/BasicBuffFile.json", false, "Description")
            }
            "BasicItemFile.json" -> {
                parseJson("$srcDir/BasicItemFile.json", false, "Tooltip")
            }
            "BasicNPCFile.json" -> {
                parseJson("$srcDir/BasicNPCFile.json", true, "")
            }
            "BasicPrefixFile.json" -> {
                parseJson("$srcDir/BasicPrefixFile.json", true, "")
            }
            "BasicProjectileFile.json" -> {
                parseJson("$srcDir/BasicProjectileFile.json", true, "")
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
            jo1.keys().forEach {
                val item = jo1.getJSONObject(it)
                val nameJson = item.getJSONObject("Name")
                val nextJson = if (isSkipNext) null else item.getJSONObject(nextName)
                val first = nameJson.optString("Origin")
                val second = nextJson?.optString("Origin") ?: ""
                val startTime = System.currentTimeMillis()
                val originResult =
                    if (p.matcher(first).find()) "" else getResponse(getHttpUrl(first))
                val endTime = System.currentTimeMillis() - startTime
                Thread.sleep(
                    if (endTime > 100) 0 else 100 - endTime
                )
                val nextResult =
                    if (p.matcher(second).find() || isSkipNext) "" else getResponse(
                        getHttpUrl(second)
                    )
                if (originResult.isNotBlank()) {
                    val sb = StringBuilder()
                    val arr = JSONArray(JSONObject(originResult).optString("trans_result"))
                    for (item in 0 until arr.length()) {
                        sb.append(JSONObject(arr.get(item).toString()).optString("dst"))
                        if (item != arr.length() - 1) {
                            sb.append("\n")
                        }
                    }
                    nameJson.put(
                        "Translation",
                        sb.append("\n" + first)
                    )
                }
                if (nextResult.isNotBlank()) {
                    val sb = StringBuilder()
                    val arr = JSONArray(JSONObject(nextResult).optString("trans_result"))
                    for (item in 0 until arr.length()) {
                        sb.append(JSONObject(arr.get(item).toString()).optString("dst"))
                        if (item != arr.length() - 1) {
                            sb.append("\n")
                        }
                    }
                    nextJson?.put(
                        "Translation",
                        sb.append("\n" + second)
                    )

                }
                if (apiType == ApiType.BAIDU) {
                    //百度Api限制1秒10次，这里单纯简单粗暴sleep 100ms
                    Thread.sleep(100)
                }
            }
            val joS = jo.toString()
            File(targetDir, path.split("/").last()).printWriter().use {
                it.println(joS)
            }
            println("Json --- Completed !\n $joS")
        }

    }
}

