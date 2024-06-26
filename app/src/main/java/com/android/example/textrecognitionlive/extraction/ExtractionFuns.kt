package com.android.example.textrecognitionlive.extraction

import android.content.ContentValues
import android.util.Log
import com.google.mlkit.vision.text.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt
import java.util.regex.Pattern


object ExtractionFuns {

    //MRP FUNCTION EXTRACTION
    fun extractMrp(text: Text): ArrayList<Any> {

        val recognizedText = text.text
        val wordsArray = recognizedText.split("[\\s:;]".toRegex()).filter { it.isNotEmpty() }
        if (wordsArray.isEmpty()) {
            return arrayListOf("Not found", 0.0, listOf(0.0), listOf("Not found"))
        }
        data class SizeMRP(val size: Double, val element: String)
        val sizesMrp = mutableListOf<SizeMRP>()

        // Extract date and MRP related block
        var resBlock = ""

        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val elementText = element.text

                    // Check for date and MRP related keywords
                    if (
                        resBlock == "" &&
                        elementText.contains("Rs.", ignoreCase = true) ||
                        elementText.contains("MRP", ignoreCase = true) ||
                        elementText.contains("₹") ||
                        elementText.contains("M.R.P", ignoreCase = true) ||
                        elementText.contains("Rs", ignoreCase = true)) {
                        resBlock = block.text
                    }

                    // Check for numeric values
                    if (elementText.matches(Regex("\\d+(\\.\\d+)?"))) {
                        val cornersM = element.cornerPoints
                        val sizeM = if (cornersM != null && cornersM.size == 4) {
                            val dx1 = (cornersM[0].x - cornersM[3].x).toDouble()
                            val dy1 = (cornersM[0].y - cornersM[3].y).toDouble()
                            sqrt(dx1 * dx1 + dy1 * dy1)
                        } else 0.0
                        sizesMrp.add(SizeMRP(sizeM, element.text))
                    }
                }
            }
        }

        val top3MRP = sizesMrp.sortedByDescending { it.size }.take(5).map { it.element }

        val recognizedTextLines = recognizedText.split("\n")
        val blockArray = resBlock.split("[\\s:;]".toRegex()).filter { it.isNotEmpty() }
        val mrpLine = recognizedTextLines.find {
            it.contains(Regex("""\b(?:Rs|MRP|mrp|₹|MR|MRR|MPP|MPR|M.R.P|Rs.|/-|incl of taxes|MAP|inc of taxes|incl of tax)\b""", RegexOption.IGNORE_CASE))
        } ?: "noneNull"

        val mrpLineArray = mrpLine.split("[\\s:;]".toRegex()).filter { it.isNotEmpty() }

        var mrpadder = 0.45
        val mscoreArr = MutableList(wordsArray.size) { 0.0 }
        wordsArray.forEachIndexed { i, word ->
            var mscore = 0.0
            word.toDoubleOrNull()?.let { num ->
                mscore += 0.2
                //MRP characteristics
//increased to 0.6, test
                if (num in 2.0..10000.0) mscore += 0.6
//TEST
                if (num != 9.0 && (num - 99) % 100 == 0.0 || (num - 9) % 10 == 0.0) {
                    mscore += 0.45
                }
                if(num % 5 == 0.0 || num % 10 == 0.0 ||  num % 100 == 0.0  || num==2.0){
                    mscore += 0.3
                }
//End test                //Conditions for address and weights
                if (num == 400.0 && wordsArray.getOrNull(i + 1)?.toDoubleOrNull() != null) mscore -= 0.5
                if (i + 1 < wordsArray.size && wordsArray[i + 1].contains(Regex("""\b(g|Kg|ml|mg|l|per|pe|n|9|k9)\b""", RegexOption.IGNORE_CASE))) {
                    mscore -= 0.5
                }
                //Condition for line
                if (mrpLineArray.contains(word)) mscore += 0.5
                //Condition for years
                if (num in 2020.0..2030.0) mscore -= 0.1
                //Condition for block
                if (blockArray.contains(word)) mscore += 0.3
                top3MRP.forEach { topMrp ->
                    if (topMrp.toDouble() == num) {
                        mscore += mrpadder
                        mrpadder -= 0.08
                    }
                }
                if( i<wordsArray.size - 1 &&(wordsArray[i+1]=="/-" || wordsArray[i+1]=="|-"))
                    mscore += 200.0
                //Condition for before being MRP etc and after being /-
                }
//                if(flag==1) mscore += 0.4
//                else if (flag==2) mscore += 0.5
//                if(word.lowercase(Locale.getDefault()) in listOf("Rs","MRP","mrp","₹","MR","MRR","MPP","MPR").map { it.lowercase(Locale.getDefault()) }) flag = 1
//TEST
                if (word.contains("/-")) {
                    val wordSplitTemp = word.split("/".toRegex()).filter { it.isNotEmpty() }
                    if(wordSplitTemp[0].toDoubleOrNull()!=null)
                        mscore += 2.5
                }

//END TEST                if(word=="/-" || word=="|-") flag=2

                mscoreArr[i] = mscore

        }

        val maxIndex = mscoreArr.indices.maxByOrNull { mscoreArr[it] } ?: -1
        val m1 = wordsArray[maxIndex]
        val m1Score = mscoreArr[maxIndex]
        return arrayListOf(m1, m1Score, blockArray, top3MRP)
    }

    //PRODUCT FUNCTION EXTRACTION
    fun extractProduct(text: Text): ArrayList<Any> {
        val recognizedText = text.text
        val wordsArray = recognizedText.split("[\\s:;.]".toRegex()).filter { it.isNotEmpty() }.toTypedArray()
        val wordsArrayReturn = wordsArray.joinToString(prefix = "[", postfix = "]", separator = ", ")
        if (wordsArray.isEmpty()) {
            return arrayListOf("Not found", 0.0, "Not found")
        }
        data class ElementSize(val size: Double, val element: String)

        val elementSizes = mutableListOf<ElementSize>()

        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val corners = element.cornerPoints
                    val size = if (corners != null && corners.size == 4) {
                        val dx1 = (corners[0].x - corners[3].x).toDouble()
                        val dy1 = (corners[0].y - corners[3].y).toDouble()
                        sqrt(dx1 * dx1 + dy1 * dy1)
                    } else 0.0
                    elementSizes.add(ElementSize(size, element.text))
//TEST
                    if (element.text.contains(Regex("\\b(item|model name|product name|product|tem|roduct|ite|produc|roduc|tfm|name|genereric name|generic|description|model)\\b", RegexOption.IGNORE_CASE)))
                        return arrayListOf(line.text, 10, wordsArrayReturn)

                }
            }
        }

        val top5Elements = elementSizes.sortedByDescending { it.size }.take(3).map { it.element }



        val scoreArr = MutableList(wordsArray.size) { 0.0 }
        val specialCharPattern = Regex("[^a-zA-Z0-9]")
        val moreThanThreeDigitsPattern = Regex("\\d{4,}")
        val alphabetOnlyPattern = Regex("^[a-zA-Z]+$")

        wordsArray.forEachIndexed { i, word ->
            var score = 0.0
            if (word.length >= 3) {
                score += when {
                    word.uppercase() == word && word.toDoubleOrNull() == null -> 0.2
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } == word && word.toDoubleOrNull() == null -> 0.16
                    else -> 0.08
                }
                if (wordsArray.size > 15) score -= 0.4
                if (wordsArray.size < 5) score += 0.2
                if (alphabetOnlyPattern.matches(word)) score += 0.15
                if (specialCharPattern.containsMatchIn(word)) score -= 0.4
                if (moreThanThreeDigitsPattern.containsMatchIn(word)) score -= 0.4
//TEST
//                if (i > 0 && wordsArray[i - 1].contains(Regex("\\b(item|model name|product name|product|tem|roduct|ite|produc|roduc|tfm|name|genereric name|generic|description|model)\\b", RegexOption.IGNORE_CASE)))
//                    return arrayListOf(finalProd, max1Score, wordsArrayReturn)
                top5Elements.forEachIndexed { index, element ->
                    if (element == word) score += 0.5 - index * 0.1
                }
            }
            scoreArr[i] = score
        }

        val maxIndex = scoreArr.indices.maxByOrNull { scoreArr[it] } ?: -1
        val max1 = wordsArray.getOrElse(maxIndex) { "No found" }
        val max1Score = scoreArr.getOrElse(maxIndex) { 0.0 }
        scoreArr[maxIndex] = 0.0
        val maxIndex2 = scoreArr.indices.maxByOrNull { scoreArr[it] } ?: -1
        val max2 = wordsArray.getOrElse(maxIndex2) { "No found" }
//        scoreArr[maxIndex2] = 0.0
//        val maxIndex3 = scoreArr.indices.maxByOrNull { scoreArr[it] } ?: -1
//        val max3 = wordsArray.getOrElse(maxIndex3) { "No found" }


        var finalProd = max1
        if(maxIndex2-maxIndex==1) {
            finalProd = "$max1 $max2"
            return arrayListOf(finalProd, max1Score, wordsArrayReturn)
        }
            //Needs testing
        var checker = 0
        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    if (max1 == element.text) {
                        finalProd = block.text
                        checker = 1
                        break
                    }
                }
                if(checker==1) break
            }
            if (checker==1) break
        }

        val finalProdArray = finalProd.split("\\s".toRegex()).filter { it.isNotEmpty() }.toTypedArray()

        if (specialCharPattern.containsMatchIn(finalProd)) {
            return if(specialCharPattern.containsMatchIn(max1)){
                arrayListOf("Not found", 0.0, wordsArrayReturn)
            } else{
                arrayListOf(max1, max1Score, wordsArrayReturn)
            }
        }
        if(finalProdArray.size > 3){
            return arrayListOf(max1, max1Score, wordsArrayReturn)
        }

        return arrayListOf(finalProd, max1Score, wordsArrayReturn)
    }

    fun extractDates(text: Text): ArrayList<Any> {
        val recognizedText = text.text
        var flag = 0
        val wordsArray = recognizedText.split("\\s:".toRegex()).filter { it.isNotEmpty() }
        val months = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
            "January", "February", "March", "April", "June", "July", "August", "September", "October", "November", "December"
        ).map { it.lowercase(Locale.getDefault()) }
        if (wordsArray.isEmpty()) {
            return arrayListOf("Not found", "Not found",0.0,0.0)
        }
        val scoreArrD = MutableList(wordsArray.size) { 0.0 }
        val alphabetOnlyPattern = Regex("^[a-zA-Z]+$")
        var early1 = ""
        var early2 = ""
        wordsArray.forEachIndexed { i, word ->
            var dScore = 0.0
            val lowerCaseWord = word.lowercase(Locale.getDefault())

            val wordSplit = word.split("[/.-]".toRegex()).filter { it.isNotEmpty() }.toTypedArray()
            if (word.toDoubleOrNull() == null && !alphabetOnlyPattern.matches(word)) {
//                if (word.contains("/") || word.contains("-") || word.contains(".")) {
//                    dScore += 0.5
//                }
                val slashCount = word.count { it == '/' }
                val dashCount = word.count { it == '-' }
                val dotCount = word.count { it == '.' }

                if(word.contains(":")||word.contains("com")) dScore -= 1.0
                if(slashCount in 1..2) dScore += 0.3 * slashCount
                else if(dashCount in 1..2) dScore += 0.22 * dashCount
                else if(dotCount in 1..2) dScore += 0.22 * dotCount

                for(ele in wordSplit){
                    if(ele.toDoubleOrNull()!=null){
                        if(ele.toDouble() in 1.0..30.0){
                            dScore += 0.2
                        }
                        if(ele.toDouble() in 2000.0..2100.0)
                            dScore += 0.4
                    }
                    else{
                        if(ele in months) dScore+=0.4
                    }
                }
            }
            //For emtpy spaced cases
            else {
                if(lowerCaseWord in months){

                    if(flag==0){
                        if(i<wordsArray.size - 1) {
                            early1 = lowerCaseWord + " "+wordsArray[i + 1]
                            flag =1
                        }
                    }
                    else{
                        if(i<wordsArray.size - 1) {
                            flag = 2
                            early2 =lowerCaseWord + wordsArray[i+1]
                        }
                    }
                }
            }
            scoreArrD[i] = dScore
        }
        if(flag==1) return arrayListOf(early1.capitalize(Locale.ROOT),"Null",0.7,0.7)
        else if(flag==2) return arrayListOf(early1.capitalize(Locale.ROOT),
            early2.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },0.9,0.9)
        val maxIndex = scoreArrD.indices.maxByOrNull { scoreArrD[it] } ?: -1
        val m1 = wordsArray[maxIndex]
        val m1Score = scoreArrD[maxIndex]
        scoreArrD[maxIndex] = 0.0
        val maxIndex2 = scoreArrD.indices.maxByOrNull { scoreArrD[it] } ?: -1
        val m2 = wordsArray[maxIndex2]
        val m2Score = scoreArrD[maxIndex2]

        return if(maxIndex>maxIndex2)
            arrayListOf(m1,m2,m1Score,m2Score)
        else
            arrayListOf(m2,m1,m2Score,m1Score)
    }


    //DATE FUNCTION EXTRACTION
//    fun extractDates(text: String): Pair<String?, String?> {
//
//        val potentialDates = mutableListOf<String>()
//
//        val dateRegex = Regex(
//            """(?ix)
//        \s*
//        (?:
//          (\d{8}) |        // DDMMYYYY, MMDDYYYY, YYYYMMDD
//          (\d{6})  |
//          (\d{1,2})[/.](\d{1,2})[/.](\d{4}) |  // Format: DD/MM/YYYY, DD.MM.YYYY
//          (\d{1,2})[/.](\d{1,2})[/.](\d{2}) |  // Format: DD/MM/YY, DD.MM.YY
//          (\d{4})[/.](\d{1,2})[/.](\d{1,2}) |  // Format: YYYY/MM/DD, YYYY.MM.DD
//          (\d{1,2})/(\d{4}) |  // Format: MM/YYYY
//          (\d{1,2})/(\d{2}) |  // Format: MM/YY
//          (\d{1,2})-(\d{1,2})-(\d{4}) |  // Format: DD-MM-YYYY
//          (\d{1,2})-(\d{1,2})-(\d{2}) |  // Format: DD-MM-YY
//          (\d{4})-(\d{1,2})-(\d{1,2}) |
//          \d{2}\s*[/.\s]\s*\d{2}\s*[/.\s]\s*(?:\d{2}|\d{4})|\d{2}\s*[A-Z]{3}\s*\d{2} |
//          (\d{1,2})\s*([Jj]an|[Ff]eb|[Mm]ar|[Aa]pr|[Mm]ay|[Jj]un|[Jj]ul|[Aa]ug|[Ss]ep|[Oo]ct|[Nn]ov|[Dd]ec)(\d{4}) |  // DD MMM YYYY
//          ([Jj]an|[Ff]eb|[Mm]ar|[Aa]pr|[Mm]ay|[Jj]un|[Jj]ul|[Aa]ug|[Ss]ep|[Oo]ct|[Nn]ov|[Dd]ec)(\d{4}) | // MMM YYYY
//          ([Jj]an|[Ff]eb|[Mm]ar|[Aa]pr|[Mm]ay|[Jj]un|[Jj]ul|[Aa]ug|[Ss]ep|[Oo]ct|[Nn]ov|[Dd]ec)(\d{2}) | // MMM YY
//          (\d{2}?[/.-]?\s*[A-Za-z]{3,}?[/.-]?\s*\d{2,4})
//        )
//        \s*
//    """.trimMargin()
//        )
//
//        dateRegex.findAll(text).forEach { match ->
//            potentialDates.add(match.value)
//            Log.i(ContentValues.TAG, match.value)
//        }
//
//        if (potentialDates.isEmpty()) {
//            return null to null
//        }
//
//        fun parseDate(dateStr: String): Date? {
//            val formats = listOf(
//                "dd/MM/yyyy", "MM/dd/yyyy", "yyyy/MM/dd",
//                "dd-MM-yyyy", "MM-dd-yyyy", "yyyy-MM-dd",
//                "dd.MM.yyyy", "MM.dd.yyyy", "yyyy.MM.dd",
//                "dd MMM yyyy", "MMM dd yyyy", "yyyy MMM dd",
//                "dd/MM/yy", "MM/dd/yy", "yy/MM/dd",
//                "dd-MM-yy", "MM-dd-yy", "yy-MM-dd",
//                "dd.MM.yy", "MM.dd.yy", "MM/yyyy",
//                "MMM yyyy", "MMM yy", "MM/yy",
//                "ddMMyy", "ddMMyyyy", "MMddyyyy", "MMddyy",
//                "yyyyMMdd", "yyMMdd", "dd-MMM-yyyy", "MMM-yyyy",
//                "MMM-yy", "MMM-yyyy", "MMMyyyy", "MMMyy"
//            ).map { SimpleDateFormat(it, Locale.ENGLISH) }
//
//            return formats.asSequence()
//                .mapNotNull { format -> runCatching { format.parse(dateStr) }.getOrNull() }
//                .firstOrNull()
//        }
//
//        fun levenshteinDistance(lhs: String, rhs: String): Int {
//            val lhsLength = lhs.length
//            val rhsLength = rhs.length
//
//            val dp = Array(lhsLength + 1) { IntArray(rhsLength + 1) }
//
//            for (i in 0..lhsLength) dp[i][0] = i
//            for (j in 0..rhsLength) dp[0][j] = j
//
//            for (i in 1..lhsLength) {
//                for (j in 1..rhsLength) {
//                    dp[i][j] = if (lhs[i - 1] == rhs[j - 1]) {
//                        dp[i - 1][j - 1]
//                    } else {
//                           minOf(dp[i - 1][j - 1] + 1, dp[i - 1][j] + 1, dp[i][j - 1] + 1)
//                    }
//                }
//            }
//
//            return dp[lhsLength][rhsLength]
//        }
//
//        val datePatterns = listOf(
//            "dd/MM/yyyy", "MM/dd/yyyy", "yyyy/MM/dd",
//            "dd-MM-yyyy", "MM-dd-yyyy", "yyyy-MM-dd",
//            "dd.MM.yyyy", "MM.dd.yyyy", "yyyy.MM.dd",
//            "dd MMM yyyy", "MMM dd yyyy", "yyyy MMM dd",
//            "dd/MM/yy", "MM/dd/yy", "yy/MM/dd",
//            "dd-MM-yy", "MM-dd-yy", "yy-MM-dd",
//            "dd.MM.yy", "MM.dd.yy", "MM/yyyy",
//            "MMM yyyy", "MMM yy", "MM/yy",
//            "ddMMyy", "ddMMyyyy", "MMddyyyy", "MMddyy",
//            "yyyyMMdd", "yyMMdd", "dd-MMM-yyyy", "MMM-yyyy",
//            "MMM-yy", "MMM-yyyy", "MMMyyyy", "MMMyy"
//        ).map { SimpleDateFormat(it, Locale.ENGLISH) }
//
//        val scoredDates = potentialDates.mapNotNull { dateStr ->
//            parseDate(dateStr)?.let {
//                val normalizedDateStr = dateStr.replace("[^\\d]".toRegex(), "")
//                val scores = datePatterns.map { format ->
//                    val sampleDateStr = format.format(Date())
//                    val sampleDateNormalized = sampleDateStr.replace("[^\\d]".toRegex(), "")
//                    levenshteinDistance(normalizedDateStr, sampleDateNormalized)
//                }
//                dateStr to scores.minOrNull()!!
//            }
//        }.sortedBy { it.second }
//            .map { it.first }
//
//        val manufacturingDate = scoredDates.firstOrNull()
//        var expiryDate = scoredDates.getOrNull(1)
//
//        // Adjust for cases where text indicates expiry explicitly
//        val expiryPattern = Regex("""(?ix)(Best Before|Use Before)""")
//        val lines = text.split("\n")
//
//        expiryDate = lines.find { it.contains(expiryPattern) } ?: expiryDate
//
//        return manufacturingDate to expiryDate
//    }


    //Date trial kapil
//        fun extractDates(text: String): ArrayList<Any> {
//        if(text.length < 2){
//            return arrayListOf("Not found","Not found",10,10)
//        }
//
//        fun levenshteinDistance(s1: CharSequence, s2: CharSequence): Int {
//            val len1 = s1.length + 1
//            val len2 = s2.length + 1
//
//            var cost = Array(len1) { it }
//            var newcost = Array(len1) { 0 }
//
//            for (j in 1 until len2) {
//                newcost[0] = j
//
//                for (i in 1 until len1) {
//                    val match = if (s1[i - 1] == s2[j - 1]) 0 else 1
//
//                    val costReplace = cost[i - 1] + match
//                    val costInsert = cost[i] + 1
//                    val costDelete = newcost[i - 1] + 1
//
//                    newcost[i] = minOf(costInsert, costDelete, costReplace)
//                }
//
//                val swap = cost
//                cost = newcost
//                newcost = swap
//            }
//
//            return cost[len1 - 1]
//        }
//
//        //Get subStr which are of 2 length
//        fun extractSubstrings(text: String): List<String> {
//            val words = text.split("\\s+".toRegex())
//            val substrings = mutableListOf<String>()
//
//            for (i in 0 until words.size - 1) {
//                val substring = "${words[i]} ${words[i + 1]}"
//                substrings.add(substring)
//            }
//
//            return substrings
//        }
//
//
//
////        val wordsArray = text.split("\\s".toRegex()).filter { it.isNotEmpty() }.toTypedArray()
//
//        val formats = listOf(
//            "25/06/2024", "25-06-2024", "25.06.2024",
//            "25/06/24", "25-06-24", "25.06.24", "06/2024",
//            "25-Jun-2024","Jun-2024"
////          "25 Jun 2024","Jun 2024","Jun 24",
//            //            "Jun-24", "Jun2024", "Jun24,  "06/24","
//        )
//
//
//        val substrings = extractSubstrings(text)
//        val distances = substrings.map { substring ->
//            val minDistance = formats.minOf { format ->
//                levenshteinDistance(substring, format)
//            }
//            substring to minDistance
//        }.sortedBy { it.second }
//
//
//        val top2 = distances.take(2)
//        return if(top2.size>=2) {
//            val (firstMatch, firstDistance) = top2[0]
//            val (secondMatch, secondDistance) = top2[1]
//            arrayListOf(firstMatch,secondMatch,firstDistance,secondDistance)
//        } else{
//            arrayListOf("NotFound","NotFound",10,10)
//
//        }
//    }
//    fun extractDates(text: String): Pair<String?, String?> {
//    val potentialDates = mutableListOf<String>()
//
//    val dateRegex = Regex(
//        """(?ix)
//        \s*
//        (?:
//          (\d{8}) |        // DDMMYYYY, MMDDYYYY, YYYYMMDD
//          (\d{6})  |
//          (\d{1,2})[/.](\d{1,2})[/.](\d{4}) |  # Format: DD/MM/YYYY, DD.MM.YYYY
//          (\d{1,2})[/.](\d{1,2})[/.](\d{2}) |  # Format: DD/MM/YY, DD.MM.YY
//          (\d{4})[/.](\d{1,2})[/.](\d{1,2}) |  # Format: YYYY/MM/DD, YYYY.MM.DD
//          (\d{1,2})/(\d{4}) |  # Format: MM/YYYY
//          (\d{1,2})/(\d{2}) |  # Format: MM/YY
//          (\d{1,2})-(\d{1,2})-(\d{4}) |  # Format: DD-MM-YYYY
//          (\d{1,2})-(\d{1,2})-(\d{2}) |  # Format: DD-MM-YY
//          (\d{4})-(\d{1,2})-(\d{1,2}) |
//          \d{2}\s*[/.\s]\s*\d{2}\s*[/.\s]\s*(?:\d{2}|\d{4})|\d{2}\s*[A-Z]{3}\s*\d{2} |
//          (\d{1,2})\s*([Jj]an|[Ff]eb|[Mm]ar|[Aa]pr|[Mm]ay|[Jj]un|[Jj]ul|[Aa]ug|[Ss]ep|[Oo]ct|[Nn]ov|[Dd]ec)(\d{4}) |  # DD MMM YYYY
//          ([Jj]an|[Ff]eb|[Mm]ar|[Aa]pr|[Mm]ay|[Jj]un|[Jj]ul|[Aa]ug|[Ss]ep|[Oo]ct|[Nn]ov|[Dd]ec)(\d{4}) | #MMM YYYY
//          ([Jj]an|[Ff]eb|[Mm]ar|[Aa]pr|[Mm]ay|[Jj]un|[Jj]ul|[Aa]ug|[Ss]ep|[Oo]ct|[Nn]ov|[Dd]ec)(\d{2}) |#MMM YY
//          (\d{2}?[/.-]?\s*[A-Za-z]{3,}?[/.-]?\s*\d{2,4})
//        )
//        \s*
//    """.trimMargin()
//    )
//
//    dateRegex.findAll(text).forEach { match ->
//        potentialDates.add(match.value)
//        Log.i(ContentValues.TAG, match.value)
//    }
//
//    if (potentialDates.isEmpty()) {
//        return null to null
//    }
//
//    fun parseDate(dateStr: String): Date? {
//        val formats = listOf(
//            "dd/MM/yyyy", "MM/dd/yyyy", "yyyy/MM/dd",
//            "dd-MM-yyyy", "MM-dd-yyyy", "yyyy-MM-dd",
//            "dd.MM.yyyy", "MM.dd.yyyy", "yyyy.MM.dd",
//            "dd MMM yyyy", "MMM dd yyyy", "yyyy MMM dd",
//            "dd/MM/yy", "MM/dd/yy", "yy/MM/dd",
//            "dd-MM-yy", "MM-dd-yy", "yy-MM-dd",
//            "dd.MM.yy", "MM.dd.yy", "MM/yyyy",
//            "MMM yyyy", "MMM yy", "MM/yy",
//            "ddMMyy", "ddMMyyyy", "MMddyyyy", "MMddyy",
//            "yyyyMMdd", "yyMMdd", "dd-MMM-yyyy", "MMM-yyyy",
//            "MMM-yy", "MMM-yyyy", "MMMyyyy", "MMMyy"
//        ).map { SimpleDateFormat(it, Locale.ENGLISH) }
//
//        return formats.asSequence()
//            .mapNotNull { format -> runCatching { format.parse(dateStr) }.getOrNull() }
//            .firstOrNull()
//    }
//
//        val sortedDates = potentialDates.mapNotNull { dateStr -> parseDate(dateStr)?.let { dateStr to it } }
//            .sortedBy { it.second }
//            .map { it.first }
//
//        val manufacturingDate = sortedDates.firstOrNull()
//        var expiryDate = sortedDates.getOrNull(1)
//
//        val expiry = Regex("""(?ix)(Best Before| Use Before)""")
//        val lines = text.split("\n")
//
//        expiryDate = lines.find { it.contains(expiry) } ?: expiryDate
//
//        return manufacturingDate to expiryDate
//    }




//    fun extractMrp(text: Text):  ArrayList<Any>    {
//
//        val recognizedText = text.text
//        data class SizeMRP(val size: Double, val element: String)
//        val sizesMrp = mutableListOf<SizeMRP>()
//
//        for (block in text.textBlocks) {
//            for (line in block.lines) {
//                for (element in line.elements) {
//                    val elementText = element.text
//                    if (elementText.matches(Regex("\\d+(\\.\\d+)?"))) {
//                        var sizeM = 0.0
//                        val cornersM = element.cornerPoints
//                        if (cornersM != null && cornersM.size == 4) {
//                            val dx1 = (cornersM[0].x - cornersM[3].x).toDouble()
//                            val dy1 = (cornersM[0].y - cornersM[3].y).toDouble()
//                            val len1M = sqrt(dx1 * dx1 + dy1 * dy1)
//                            sizeM = len1M
//                        }
//                        sizesMrp.add(SizeMRP(sizeM, element.text))
//                    }
//                }
//            }
//        }
//
//        val top3MRP: List<String> =
//            sizesMrp.sortedByDescending { it.size }.take(5).map { it.element }
//
//        val wordsArray = recognizedText.split("[\\s:;.]".toRegex()).filter { it.isNotEmpty() }.toTypedArray()
//        if(wordsArray.isEmpty()){
////            return arrayListOf("Not found",0.0,"Not found",0.0,listOf(0.0),"Not found",listOf("Not found"),"Not found")
//            return arrayListOf("Not found", 0.0, listOf(0.0), listOf("Not found"))
//
//        }
//
//        val recognizedTextLines = recognizedText.split("\n").toTypedArray()
//        var mscore = 0.0
//        val mscoreArr = mutableListOf<Double>()
//
//        val blockOfMrp = extractDateMrpBlock(text).toString()
//        val blockArray = blockOfMrp.split("[\\s:;.]".toRegex()).filter { it.isNotEmpty() }.toTypedArray()
//        //3rd condition MRP function
//        var mrpLine = "noneNull"
//        for (line in recognizedTextLines) {
//            if (line.contains(Regex("""\b(?:Rs|MRP|mrp|₹|MR|MRR|MPP|MPR|M.R.P|Rs.|)\b""",RegexOption.IGNORE_CASE))) {
//                mrpLine = line
//                break
//            }
//        }
//        val mrpLineArray = mrpLine.split("[\\s:;.]".toRegex()).filter { it.isNotEmpty() }.toTypedArray()
//        var mrpadder = 0.4
//        for (i in wordsArray.indices) {
//            if(wordsArray[i].toDoubleOrNull()!=null) {
//                val num = wordsArray[i].toDouble()
//                mscore += 0.05
//                if (num in 2.0..5000.0) {
//                    mscore += 0.5
//                }
//                if(num==2.0){
//                    mscore+=0.3
//                }
//                //2nd condition
//                if ((num!= 9.0) && (num % 5 == 0.0 || num % 10 == 0.0 || (num - 99) % 100 == 0.0 || num % 100 == 0.0  || (num - 9) % 10 == 0.0 )) {
//                    mscore += 0.3
//                }
//                if((num==400.00)&&wordsArray[i+1].toDoubleOrNull()!=null){
//                    mscore -= 0.5
//                }
//                //3rd condition
//                if( i+1<wordsArray.size-1 && wordsArray[i+1].contains(Regex("""\b(g|Kg|ml|mg|l|per|pe|n|9|k9)\b""",RegexOption.IGNORE_CASE))){
//                    mscore -= 0.5
//                }
//                if (mrpLineArray.contains(wordsArray[i])) {
//                    //showToast("Line")
//                    mscore += 0.4
//                }
//                if (2020 < num && num < 2030) {
//                    mscore -= 0.1
//                }
//                //4th condition
//                if (wordsArray[i] in blockArray) {
//                    mscore += 0.3
//                }
////                if (i<(len*(2/3)) && i > (len*(1/3))) {
////                    mscore += 0.1
////                }
//                for (j in top3MRP.indices) {
//                    if (top3MRP[j].toDouble() == num) {
//                        mscore += mrpadder
//                        mrpadder -= 0.1
//                    }
//                }
//            }
//
//            //Might need changes
//            if(wordsArray[i].contains("/-")){
//                if(i>1 && wordsArray[i-1].toDoubleOrNull()!=null){
//                    mscoreArr[i-1] += 0.8
//                }
//                else{
//                    mscore += 2.0
//                }
//            }
//            mscoreArr.add(mscore)
//            mscore=0.0
//        }
//
//        val j1 = mscoreArr.indexOf(mscoreArr.maxOrNull())
//        val m1 = wordsArray[j1]
//        val m1Score = mscoreArr[j1]
//
//        return arrayListOf(m1, m1Score, mscoreArr, top3MRP)
//    }
//    fun extractProduct(text: Text): ArrayList<Any> {
//        val recognizedText = text.text
//        data class ElementSize(val size: Double, val element: String)
//
//        val elementSizes = mutableListOf<ElementSize>()
//
//        for (block in text.textBlocks) {
//            for (line in block.lines) {
//                for (element in line.elements) {
//                    var size = 0.0
//                    val corners = element.cornerPoints
//                    //val elementText = element.text
//                    if (corners != null && corners.size == 4) {
//                        val dx1 = (corners[0].x - corners[3].x).toDouble()
//                        val dy1 = (corners[0].y - corners[3].y).toDouble()
//                        val len1 = sqrt(dx1 * dx1 + dy1 * dy1)
//                        size = len1
//                    }
//                    elementSizes.add(ElementSize(size, element.text))
//                }
//            }
//        }
//        val top5Elements: List<String> =
//            elementSizes.sortedByDescending { it.size }.take(3).map { it.element }
//
//        //Score calculation
//        //val wordsArray = recognizedText.split("(\\s+|:|;|.)".toRegex()).toTypedArray()
//
//        val wordsArray = recognizedText.split("[\\s:;.]".toRegex()).filter { it.isNotEmpty() }.toTypedArray()
//        if(wordsArray.isEmpty()){
//            return arrayListOf("Not found",0.0,"Not found")
//        }
//
//        //val recognizedTextLines = recognizedText.split("\n").toTypedArray()
//
//        var score = 0.0
//        val scoreArr = mutableListOf<Double>()
//        val len = wordsArray.size.toDouble()
//        var adder = 0.5
//
//        //4th condition
//        val specialCharPattern = Pattern.compile("[^a-zA-Z0-9]")
//        val moreThanThreeDigitsPattern = Pattern.compile("\\d{4,}")
//        val alphabetOnlyPattern = Regex("^[a-zA-Z]+$")
//
//        for (i in wordsArray.indices) {
//            if (wordsArray[i].length  >= 3) {
//                //Higher uppercase socre for local products
//                if (wordsArray[i].uppercase() == wordsArray[i] && wordsArray[i].toDoubleOrNull() == null) {
//                    score += 0.2
//                } else {
//                    //Higher capitalizaton score for famous and sophisticated products
//                    if (wordsArray[i].capitalize(Locale.ROOT) == wordsArray[i] && wordsArray[i].toDoubleOrNull() == null) {
//                        score += 0.16
//                    } else {
//                        score += 0.08
//                    }
//                }
//                //Lower for causal products(generally rare)
//                if (len > 15) {
//                    score -= 0.3
//                }
//                if (len < 5) {
//                    score += 0.2
//                }
//                //Score for zoomed in scanning
//
//                if (alphabetOnlyPattern.matches(wordsArray[i])) {
//                    score += 0.15
//                }
//                if (specialCharPattern.matcher(wordsArray[i]).find()) {
//                    score -= 0.4
//                }
//                if (moreThanThreeDigitsPattern.matcher(wordsArray[i]).find()) {
//                    score -= 0.4
//                }
//                if (i > 0 && wordsArray[i - 1].contains(Regex("""\b(item|product|tem|roduct|ite|produc|roduc|tfm)\b""", RegexOption.IGNORE_CASE))) {
//                    score += 1
//                }
//                for (j in top5Elements.indices) {
//                    if (top5Elements[j] == wordsArray[i]) {
//                        score += adder
//                        adder -= 0.1
//                    }
//                }
//            }
//
//            scoreArr.add(score)
//            score = 0.0
//        }
////        var adder = 0.5
////            for (j in wordsArray.indices) {
////                for (i in top5Elements.indices) {
////                if (top5Elements[i] == wordsArray[j])
////                {
////                    scoreArr[j] += adder
////                    adder -= 0.1
////                }
////            }
////        }
//
//
//        //Setting up max scorers
//        val i1 = scoreArr.indexOf(scoreArr.maxOrNull())
//        val max1 = wordsArray[i1]
//        var max1Score = scoreArr[i1]
//        //scoreArr[i1] = 0.0
//        //val i2 = scoreArr.indexOf(scoreArr.maxOrNull())
//        //val max2 = wordsArray[i2]
//
//        var finalProd = max1
////        var flag = 0
//        for (block in text.textBlocks) {
////            if(flag==1){
////                if(block.text.toString().contains(max2)){
////                    finalProd = max1 + block.text
////                }
////                break
////        }
//            for (line in block.lines) {
//                for (element in line.elements) {
//                    if (max1 == element.text) {
//                        finalProd = block.text
////                        flag = 1
//                    }
//                }
//            }
//        }
//        val wordsArrayReturn = wordsArray.joinToString(prefix = "[", postfix = "]", separator = ", ")
//
//
////Final Prod Return conditions
//        val finaProdArray = finalProd.split("\\s".toRegex()).filter { it.isNotEmpty() }.toTypedArray()
//
//        if (specialCharPattern.matcher(finalProd).find() && finaProdArray.size>3) {
//            finalProd = "Not found"
//            max1Score = 0.0
//        }
//
//        return arrayListOf(finalProd, max1Score, wordsArrayReturn)
//
//    }
//
//    private fun extractDateMrpBlock(text: Text): ArrayList<Any>  {
//        val resBlock = ArrayList<Any>()
//        val dateRegex = """\b\d{2}\s*[/.\s]\s*\d{2}\s*[/.\s]\s*(?:\d{2}|\d{4})\b|\b\d{2}\s*[A-Z]{3}\s*\d{2}\b""".toRegex()
//
//        for (block in text.textBlocks) {
//            outer@ for (line in block.lines) {
//                for (element in line.elements) {
//                    val s = element.text
//
//                    if (dateRegex.containsMatchIn(s) ||
//                        s.contains("Rs.", ignoreCase = true) ||
//                        s.contains("MRP", ignoreCase = true) ||
//                        s.contains("₹") ||
//                        //  s.contains("/") ||
//                        s.contains("M.R.P", ignoreCase = true) ||
//                        s.contains("Rs", ignoreCase = true)) {
//
//                        resBlock.add(block.text)
//                        break@outer
//                    }
//                }
//            }
//        }
//        Log.i(ContentValues.TAG, resBlock.toString())
//        return resBlock
//    }
//
//    fun extractDates(text: String): Pair<String?, String?> {
//        val potentialDates = mutableListOf<String>()
//
//        val dateRegex = Regex("""(?ix)
//\s*
//(?:
//  (\d{1,2})[/.](\d{1,2})[/.](\d{4}) |  # Format: DD/MM/YYYY, DD.MM.YYYY
//  (\d{1,2})[/.](\d{1,2})[/.](\d{2}) |  # Format: DD/MM/YY, DD.MM.YY
//  (\d{4})[/.](\d{1,2})[/.](\d{1,2}) |  # Format: YYYY/MM/DD, YYYY.MM.DD
//
//  (\d{1,2})/(\d{4}) |  # Format: MM/YYYY
//  (\d{1,2})/(\d{2}) |  # Format: MM/YY
//
//
//  (\d{1,2})-(\d{1,2})-(\d{4}) |  # Format: DD-MM-YYYY
//  (\d{1,2})-(\d{1,2})-(\d{2}) |  # Format: DD-MM-YY
//  (\d{4})-(\d{1,2})-(\d{1,2}) |
//
//  \d{2}\s*[/.\s]\s*\d{2}\s*[/.\s]\s*(?:\d{2}|\d{4})|\d{2}\s*[A-Z]{3}\s*\d{2} |
//
//  # Formats with Month Names
//  (\d{1,2})\s*([Jj]an|[Ff]eb|[Mm]ar|[Aa]pr|[Mm]ay|[Jj]un|[Jj]ul|[Aa]ug|[Ss]ep|[Oo]ct|[Nn]ov|[Dd]ec)(\d{4}) |  # DD MMM YYYY
//  ([Jj]an|[Ff]eb|[Mm]ar|[Aa]pr|[Mm]ay|[Jj]un|[Jj]ul|[Aa]ug|[Ss]ep|[Oo]ct|[Nn]ov|[Dd]ec)(\d{4}) | #MMM YYYY
//  ([Jj]an|[Ff]eb|[Mm]ar|[Aa]pr|[Mm]ay|[Jj]un|[Jj]ul|[Aa]ug|[Ss]ep|[Oo]ct|[Nn]ov|[Dd]ec)(\d{2}) | #MMM YY
//)
//\s*
//""".trimMargin())
//
//        // Find all potential dates using the regex
//        dateRegex.findAll(text).forEach { match ->
//            potentialDates.add(match.value)
//            Log.i(ContentValues.TAG, match.value)
//        }
//
//        // If no dates are found, return null for both
//        if (potentialDates.isEmpty()) {
//            return null to null
//        }
//
//        // Helper function to convert date strings to Date objects for comparison
//        fun parseDate(dateStr: String): Date? {
//            val formats = listOf(
//                SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
//                SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH),
//                SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH),
//                SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
//                SimpleDateFormat("MM-dd-yyyy", Locale.ENGLISH),
//                SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
//                SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH),
//                SimpleDateFormat("MM.dd.yyyy", Locale.ENGLISH),
//                SimpleDateFormat("yyyy.MM.dd", Locale.ENGLISH),
//                SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
//                SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH),
//                SimpleDateFormat("yyyy MMM dd", Locale.ENGLISH),
//                SimpleDateFormat("dd/MM/yy", Locale.ENGLISH),
//                SimpleDateFormat("MM/dd/yy", Locale.ENGLISH),
//                SimpleDateFormat("yy/MM/dd", Locale.ENGLISH),
//                SimpleDateFormat("dd-MM-yy", Locale.ENGLISH),
//                SimpleDateFormat("MM-dd-yy", Locale.ENGLISH),
//                SimpleDateFormat("yy-MM-dd", Locale.ENGLISH),
//                SimpleDateFormat("dd.MM.yy", Locale.ENGLISH),
//                SimpleDateFormat("MM.dd.yy", Locale.ENGLISH),
//                SimpleDateFormat("MM/yyyy", Locale.ENGLISH),
//                SimpleDateFormat("MMM yyyy", Locale.ENGLISH),
//                SimpleDateFormat("MMM yy", Locale.ENGLISH),
//                SimpleDateFormat("MM/yy", Locale.ENGLISH),
////                SimpleDateFormat("MM-yy", Locale.ENGLISH),
////                SimpleDateFormat("MM-yyyy", Locale.ENGLISH),
//            )
//
//            for (format in formats) {
//                try {
//                    return format.parse(dateStr)
//                } catch (e: Exception) {
//                    // Continue to the next format
//                }
//            }
//            return null
//        }
//
//
//
//        // Sort the potential dates by their parsed Date objects
//        val sortedDates = potentialDates.mapNotNull { dateStr -> parseDate(dateStr)?.let { dateStr to it } }
//            .sortedBy { it.second }
//            .map { it.first }
//
//        // Assuming the first sorted date is manufacturing and the second is expiry (adjust logic if needed)
//        val manufacturingDate = sortedDates.firstOrNull()
//        var expiryDate = sortedDates.getOrNull(1)
//
//        val expiry = Regex("""(?ix)(Best Before| Use Before)""")
//
//        val lines = text.split("\n").toTypedArray()
//
//        for (line in lines){
//            expiryDate = if(line.contains(expiry))
//                line
//            else if(manufacturingDate != expiryDate && manufacturingDate!=null && expiryDate!=null)
//                sortedDates.getOrNull(1)
//            else
//                null
//        }
//
//        return manufacturingDate to expiryDate
//        //return finalMFG to finalEXP
//    }
}