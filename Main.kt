import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

data class WeatherData(
    val YYMMDDHHMI: String,   // 시간
    val TA: String,           // 기온
    val HM: String,           // 습도
    val WS: String,           // 풍속
    val TPW: String,          // 총강수량
    val UPRESS: String,       // 상층운 기압
    val MPRESS: String,       // 중층운 기압
    val LPRESS: String        // 하층운 기압
)

fun main() {
    val jsonFilePath = "weather_data.json"
    val resultFilePathBase = "weather_analysis_result" // 파일 기본 이름
    val allOutputs = mutableListOf<String>() // 누적 출력 내용을 저장

    try {
        // API 호출 및 JSON 파일 생성
        apiToJson(jsonFilePath)

        // JSON 파일 로드
        val weatherData = loadJson(jsonFilePath)

        while (true) {
            val inputTime = userInput()

            // 'q' 입력 시 종료
            if (inputTime?.lowercase() == "q") {
                handleExit(allOutputs, resultFilePathBase)
                break
            }

            // 입력 값이 유효하지 않으면 재검색 요청
            if (inputTime == null) continue

            // 조건 분석 및 결과 출력
            val output = anlzWD(inputTime, weatherData)
            if (output != null) {
                allOutputs.add(output) // 출력 결과 누적 저장
            }
        }
    } catch (e: Exception) {
        println("오류 발생: ${e.message}")
    }
}

// JSON 파일 저장을 위한 API 호출
fun apiToJson(filePath: String) {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiUrl = "https://apihub.kma.go.kr/api/typ01/url/upp_idx.php?tm1=2023030100&tm2=2023053100&stn=47169&authKey=VWWldOgqQRSlpXToKqEUew"

    try {
        val request = Request.Builder().url(apiUrl).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("API 호출 실패: ${response.code} - ${response.message}")
        }

        val responseData = response.body!!.string()
        saveJson(filePath, responseData)

    } catch (e: IOException) {
        println("네트워크 오류: ${e.message}")
    } catch (e: Exception) {
        println("알 수 없는 오류 발생: ${e.message}")
    }
}

// API 응답 데이터를 JSON으로 저장
fun saveJson(filePath: String, responseData: String) {
    val lines = responseData.split("\n").filter { it.isNotBlank() && !it.startsWith("#") }
    if (lines.isEmpty()) throw IOException("API 데이터가 비어 있습니다.")
    val headerLine = "YYMMDDHHMI   STN       TA       HM       WD       WS       TPW       UPRESS       MPRESS       LPRESS"
    val headers = headerLine.split("\\s+".toRegex()).filter { it.isNotBlank() }
    val jsonResult = JsonArray()

    lines.forEach { line ->
        val values = line.split(",").map { it.trim() }
        if (values.size >= headers.size) {
            val jsonObject = JsonObject()
            headers.forEachIndexed { index, header ->
                jsonObject.addProperty(header, values[index])
            }
            jsonResult.add(jsonObject)
        }
    }
    File(filePath).writeText(Gson().toJson(jsonResult))
    println("JSON 파일 저장 완료: $filePath")
}

// JSON 파일 로드 및 WeatherData 객체로 변환
fun loadJson(filePath: String): List<WeatherData> {
    val file = File(filePath)
    if (!file.exists()) throw IOException("JSON 파일이 존재하지 않습니다: $filePath")

    val jsonArray = Gson().fromJson(file.readText(), JsonArray::class.java)
    return jsonArray.map { jsonElement ->
        val obj = jsonElement.asJsonObject
        WeatherData(
            YYMMDDHHMI = obj["YYMMDDHHMI"].asString,
            TA = obj["TA"].asString,
            HM = obj["HM"].asString,
            WS = obj["WS"].asString,
            TPW = obj["TPW"].asString,
            UPRESS = obj["UPRESS"].asString,
            MPRESS = obj["MPRESS"].asString,
            LPRESS = obj["LPRESS"].asString
        )
    }
}

// 사용자에게 철새의 이동을 분석할 날짜 입력받는 function, q입력 전까지 계속 입력가능
fun userInput(): String? {
    println("===== 흑산도(전라남도 신안군 흑산면) 지역 철새 이동 데이터 입력 =====")
    println("검색을 종료하려면 'q'를 입력해주세요.")
    print("철새의 이동을 분석할 날짜를 입력하세요. 시간은 6시간 단위로 입력해주세요. (예: 202303010000 ~ 202305310000): ")
    val inputTime = readLine()?.trim()

    if (inputTime.isNullOrBlank()) {
        println("입력이 비어 있습니다. 다시 입력해주세요.\n")
        return null
    }

    if (inputTime.lowercase() == "q") {
        return "q"
    }

    if (!inputTime.matches(Regex("\\d{12}"))) {
        println("잘못된 형식입니다. 형식에 맞게 다시 입력해주세요 (YYYYMMDDHH).\n")
        return null
    }
    // 6시간 단위 확인 (시간 부분이 "00", "06", "12", "18" 중 하나인지 검증)
    if (inputTime.substring(8, 10) !in listOf("00", "06", "12", "18")) {
        println("시간은 6시간 단위로 입력해주세요 (00, 06, 12, 18).")
        return null
    }
    return inputTime
}

fun anlzWD(inputTime: String, weatherDataList: List<WeatherData>): String? {
    val matchingData = weatherDataList.firstOrNull { it.YYMMDDHHMI == inputTime }

    if (matchingData == null) {
        println("입력한 시간에 해당하는 데이터가 없습니다. 다시 검색해주세요.\n")
        return null
    }

    val analysisResults = weatherAnlz(matchingData)

    val output = buildString {
        appendLine("===== 분석 결과 =====")
        appendLine("입력 시간: $inputTime")
        appendLine("조회된 데이터:")
        appendLine(matchingData.toString())
        appendLine("분석 결과:")
        analysisResults.forEach { (key, value) ->
            appendLine("  $key: $value")
        }
        val overallStatus = Overall(analysisResults)
        appendLine("  종합 평가: $overallStatus")
        appendLine("=========================")
    }

    // 결과 출력
    println(output)
    return output
}

// 기상 데이터 분석 수행
fun weatherAnlz(data: WeatherData): Map<String, String> {
    val temperature = data.TA.toDoubleOrNull() ?: Double.NaN
    val windSpeed = data.WS.toDoubleOrNull() ?: Double.NaN
    val precipitation = data.TPW.toDoubleOrNull() ?: Double.NaN
    val humidity = data.HM.toDoubleOrNull() ?: Double.NaN
    val upress = data.UPRESS.toDoubleOrNull()
    val mpress = data.MPRESS.toDoubleOrNull()
    val lpress = data.LPRESS.toDoubleOrNull()

    return mapOf(
        "기온 분석" to anlzTem(temperature),
        "풍속 분석" to anlzWindS(windSpeed),
        "강수량 분석" to anlzRainfall(precipitation),
        "습도 분석" to anlzHum(humidity),
        "고도 분석" to anlzAtt(upress, mpress, lpress)
    )
}
// 종합 평가 수행
fun Overall(analysisResults: Map<String, String>): String {
    val suitableConditions = listOf(
        analysisResults["기온 분석"]?.contains("적합") ?: false,
        analysisResults["풍속 분석"]?.contains("적합") ?: false,
        analysisResults["강수량 분석"]?.contains("적합") ?: false,
        analysisResults["습도 분석"]?.contains("적합") ?: false
    )
    return if (suitableConditions.all { it }) {
        "철새가 이동하기에 적합한 조건입니다."
    } else {
        "철새가 이동하기에 적합하지 않은 조건입니다."
    }
}
// 조건 분석 함수
fun analyzeCondition(value: Double, thresholds: List<Pair<Double, String>>): String {
    if (value.isNaN()) return "데이터가 유효하지 않습니다."
    thresholds.forEach { (limit, message) ->
        if (value <= limit) return message
    }
    return "조건이 불명확합니다."
}
// 개별 조건 분석 함수
fun anlzTem(temp: Double): String {
    val thresholds = listOf(
        Pair(10.0, "기온이 적당히 낮아 이동에 적합합니다."),
        Pair(20.0, "기온이 너무 높아 이동이 어려울 수 있습니다.")
    )
    return analyzeCondition(temp, thresholds)
}

fun anlzWindS(speed: Double): String {
    val thresholds = listOf(
        Pair(5.0, "풍속이 약해 이동에 적합합니다."),
        Pair(20.0, "풍속이 약간 강하나 이동에 적합합니다."),
        Pair(Double.MAX_VALUE, "역풍이라 이동이 어려울 수 있습니다.")
    )
    return analyzeCondition(speed, thresholds)
}

fun anlzRainfall(rain: Double): String {
    val thresholds = listOf(
        Pair(1000.0, "비가 내리지 않아 이동에 적합합니다."),
        Pair(5000.0, "강수량이 적어 이동에 적합합니다."),
        Pair(Double.MAX_VALUE, "강수량이 많아 이동이 어려울 수 있습니다.")
    )
    return analyzeCondition(rain, thresholds)
}

fun anlzHum(humidity: Double): String {
    val thresholds = listOf(
        Pair(40.0, "습도가 낮아 이동이 어려울 수 있습니다."),
        Pair(70.0, "습도가 적정하여 이동에 적합합니다."),
        Pair(85.0, "습도가 약간 높으나 이동에 적합합니다."),
        Pair(Double.MAX_VALUE, "습도가 높아 이동이 어려울 수 있습니다.")
    )
    return analyzeCondition(humidity, thresholds)
}

fun anlzAtt(upress: Double?, mpress: Double?, lpress: Double?): String {
    if (upress == null || mpress == null || lpress == null) {
        return "고도별 기압 데이터가 부족합니다."
    }

    return when {
        upress > mpress && mpress > lpress -> "기압이 안정적이며 새들이 높은 고도를 선호할 가능성이 높습니다."

        upress < mpress && mpress < lpress -> "기압이 불안정하며 새들이 낮은 고도를 선호할 가능성이 높습니다."

        else -> "기압 경향이 복잡하여 고도 분석이 어렵습니다."
    }
}
// 종료 처리
fun handleExit(allOutputs: List<String>, resultFilePathBase: String) {
    if (allOutputs.isNotEmpty()) {
        print("출력된 내용을 저장하시겠습니까? (y/n): ")
        if (readLine()?.trim()?.lowercase() == "y") {
            print("저장할 파일 이름을 입력하세요 (기본 이름 사용하려면 Enter): ")
            val userFileName = readLine()?.trim()
            val filePath = if (!userFileName.isNullOrEmpty()) {
                "$userFileName.txt"
            } else {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                "${resultFilePathBase}_$timestamp.txt"
            }
            FileManager(filePath).saveText(allOutputs.joinToString("\n\n"))
            println("출력 내용이 저장되었습니다: $filePath")
        } else {
            println("저장하지 않았습니다.")
        }
    }
    println("프로그램을 종료합니다.")
}

class FileManager(private val filePath: String) {
    fun saveText(data: String) {
        Direct()
        File(filePath).writeText(data)
        println("텍스트 파일 저장 완료: $filePath")
    }

    private fun Direct() {
        val file = File(filePath).parentFile
        if (file != null && !file.exists()) {
            file.mkdirs()
        }
    }
}