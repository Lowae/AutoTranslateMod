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

fun main(args: Array<String>) {
    println("源文件目录：")
    val srcDir = File(readLine().orEmpty()).let { inFile ->
        if (inFile.exists()) inFile.absolutePath
        else throw IllegalArgumentException("输入文件路径错误")
    }
    println("Input Dir: $srcDir")
    println("输出文件目录(默认同层级，enter默认)：")
    val targetDir = readLine().let { outFile ->
        if (outFile.isNullOrBlank().not()) outFile.toString()
        else srcDir.substring(0, srcDir.lastIndexOf("\\")) + "\\out"
    }
    println("Output Dir: $targetDir")
    println("APPID：")
    val appid = readLine() ?: ""
    println("密钥：")
    val key = readLine() ?: ""

    val apiType = Translate.ApiType.BAIDU

    Translate(apiType, appid, key, srcDir, targetDir).apply {
        println(srcDir)
        File(srcDir).list()?.forEach {
            println("${"-".repeat(30)} 当前翻译文件：$it ${"-".repeat(30)}")
            switchParse(it)
        }
    }
}

class Translate(
    private val apiType: ApiType,
    private val APPID: String,
    private val KEY: String,
    private val srcDir: String,
    private val targetDir: String
) {
    enum class ApiType {
        BAIDU
    }

    /**
     * 匹配中文，如果有翻译则跳过
     */
    private val ChineseMatch: Pattern = Pattern.compile("[\u4e00-\u9fa5]")
    private val ContentMatch =
        Pattern.compile("(?:LegacyInterface.28|RedePlayer|\\[|\\]|\\\\|/)", Pattern.CASE_INSENSITIVE)

    private val NameMatch = Pattern.compile("(?:Chat|UpdateArmorSet)", Pattern.CASE_INSENSITIVE)

    private val PassMatch = Pattern.compile("(?:Chat)")

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

    private fun getHttpUrl(content: String?): String {
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
                parseJson("$srcDir\\BasicBuffFile.json", false, "Description")
            }
            "BasicItemFile.json" -> {
                parseJson("$srcDir\\BasicItemFile.json", false, "Tooltip")
            }
            "BasicNPCFile.json" -> {
                parseJson("$srcDir\\BasicNPCFile.json", true, "")
            }
            "BasicPrefixFile.json" -> {
                parseJson("$srcDir\\BasicPrefixFile.json", true, "")
            }
            "BasicProjectileFile.json" -> {
                parseJson("$srcDir\\BasicProjectileFile.json", true, "")
            }
            "LdstrFile.json" -> {
                parseLdstrFile("$srcDir\\LdstrFile.json")
            }
            else -> return
        }
    }

    private fun parseLdstrFile(path: String) {
        val s = getJson(path).toString()
        val jo = JSONObject(s)
        val k = jo.keys()
        while (k.hasNext()) {
            val jo1 = jo.getJSONObject(k.next())
            jo1.keys().forEach {
                val index = it.lastIndexOf("::")
                val matchStr = it.substring(index + 2, it.length)
                if (NameMatch.matcher(matchStr).find()) {
                    val item = jo1.getJSONObject(it)
                    val instructions = item.getJSONArray("Instructions")
                    for (i in 0 until instructions.length()) {
                        val jsonObj = instructions.getJSONObject(i)
                        val origin = jsonObj.getString("Origin")
                        if (ContentMatch.matcher(origin).find() && !PassMatch.matcher(matchStr).find()) return@forEach
                        val result = if (ChineseMatch.matcher(jsonObj.getString("Translation"))
                                .find()
                        ) return@forEach else getResponse(getHttpUrl(origin))
                        val startTime = System.currentTimeMillis()
                        val sb = StringBuilder()
                        val arr = JSONArray(JSONObject(result).optString("trans_result"))
                        for (item in 0 until arr.length()) {
                            sb.append(JSONObject(arr.get(item).toString()).optString("dst"))
                            if (item != arr.length() - 1) {
                                sb.append("\n")
                            }
                        }

                        standardTrans(jsonObj, origin, sb)

                        val gapTime = System.currentTimeMillis() - startTime
                        Thread.sleep(
                            if (gapTime > 100) 0 else gapTime
                        )
                    }
                }
            }
            val joS = jo.toString()
            val target = File(targetDir)
            if (!target.exists() && !target.isDirectory) {
                target.mkdir()
            }
            File(targetDir).apply { if (isDirectory && exists()) return@apply else mkdir() }
            File(targetDir, path.split("\\").last()).apply {
                if (exists().not()) createNewFile()
            }.printWriter().use {
                it.println(joS)
            }
            println("${"-".repeat(30)} 翻译文件完成：$path ${"-".repeat(30)}")
            println("文本如下：\n$joS")
        }
    }

    private fun parseJson(path: String, isSkipNext: Boolean = false, nextName: String = "") {
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
                val firstTrans = nameJson.optString("Translation")
                val second = nextJson?.optString("Origin") ?: ""
                val secondTrans = nameJson.optString("Translation")
                val startTime = System.currentTimeMillis()
                val originResult =
                    if (ChineseMatch.matcher(firstTrans).find()) "" else getResponse(getHttpUrl(first))
                val endTime = System.currentTimeMillis() - startTime
                Thread.sleep(
                    if (endTime > 100) 0 else 100 - endTime//百度Api限制1秒10次
                )
                val nextResult =
                    if (ChineseMatch.matcher(secondTrans).find() || isSkipNext) "" else getResponse(
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
                    println("$first ----- $sb")
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
                    println("$second ----- $sb")
                    nextJson?.put(
                        "Translation",
                        sb.append("\n" + second)
                    )

                }
            }
            val joS = jo.toString()
            File(targetDir).apply { if (exists()) return@apply else mkdir() }
            File(targetDir, path.split("\\").last()).apply {
                if (exists().not()) createNewFile()
            }.printWriter().use {
                it.println(joS)
            }
            println("${"-".repeat(30)} 翻译文件完成：$path ${"-".repeat(30)}")
            println("文本如下：\n$joS")
        }

    }

    private fun standardTrans(jsonObj: JSONObject, origin: String, trans: StringBuilder) {

        println("$origin ----- $trans")
        print("是否翻译该项Y/N：")
        val isPut = readLine()?.trim()
        if (isPut == "Y" || isPut == "y") {
            jsonObj.put(
                "Translation",
                trans.append("\n" + origin)
            )
        } else {
            println("输入优化后翻译:")
            val opt = readLine()
            jsonObj.put(
                "Translation",
                "$opt\n$origin"
            )
        }

    }

}


