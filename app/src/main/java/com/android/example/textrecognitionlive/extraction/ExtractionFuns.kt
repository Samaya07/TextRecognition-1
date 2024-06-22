package com.android.example.textrecognitionlive.extraction

import android.content.ContentValues
import android.util.Log
import com.google.mlkit.vision.text.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.sqrt

object ExtractionFuns {

//MRP FUNCTION EXTRACTION
    fun extractMrp(text: Text): ArrayList<Any> {

        val recognizedText = text.text
        data class SizeMRP(val size: Double, val element: String)
        val sizesMrp = mutableListOf<SizeMRP>()

        // Extract date and MRP related block
        val dateRegex = """\b\d{2}\s*[/.\s]\s*\d{2}\s*[/.\s]\s*(?:\d{2}|\d{4})\b|\b\d{2}\s*[A-Z]{3}\s*\d{2}\b""".toRegex()
        val resBlock = mutableListOf<String>()

        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val elementText = element.text

                    // Check for date and MRP related keywords
                    if (dateRegex.containsMatchIn(elementText) ||
                        elementText.contains("Rs.", ignoreCase = true) ||
                        elementText.contains("MRP", ignoreCase = true) ||
                        elementText.contains("₹") ||
                        elementText.contains("M.R.P", ignoreCase = true) ||
                        elementText.contains("Rs", ignoreCase = true)) {

                        resBlock.add(block.text)
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
        val wordsArray = recognizedText.split("[\\s:;.]".toRegex()).filter { it.isNotEmpty() }

        if (wordsArray.isEmpty()) {
            return arrayListOf("Not found", 0.0, listOf(0.0), listOf("Not found"))
        }

        val recognizedTextLines = recognizedText.split("\n")
        val blockArray = resBlock.flatMap { it.split("[\\s:;.]".toRegex()).filter { it.isNotEmpty() } }
        val mrpLine = recognizedTextLines.find {
            it.contains(Regex("""\b(?:Rs|MRP|mrp|₹|MR|MRR|MPP|MPR|M.R.P|Rs.)\b""", RegexOption.IGNORE_CASE))
        } ?: "noneNull"

        val mrpLineArray = mrpLine.split("[\\s:;.]".toRegex()).filter { it.isNotEmpty() }

        var mrpadder = 0.4
        val mscoreArr = MutableList(wordsArray.size) { 0.0 }

        wordsArray.forEachIndexed { i, word ->
            word.toDoubleOrNull()?.let { num ->
                var mscore = 0.05
                if (num in 2.0..5000.0) mscore += 0.5
                if (num == 2.0) mscore += 0.3
                if (num != 9.0 && (num % 5 == 0.0 || num % 10 == 0.0 || (num - 99) % 100 == 0.0 || num % 100 == 0.0 || (num - 9) % 10 == 0.0)) {
                    mscore += 0.3
                }
                if (num == 400.0 && wordsArray.getOrNull(i + 1)?.toDoubleOrNull() != null) mscore -= 0.5
                if (i + 1 < wordsArray.size && wordsArray[i + 1].contains(Regex("""\b(g|Kg|ml|mg|l|per|pe|n|9|k9)\b""", RegexOption.IGNORE_CASE))) {
                    mscore -= 0.5
                }
                if (mrpLineArray.contains(word)) mscore += 0.4
                if (num in 2020.0..2030.0) mscore -= 0.1
                if (blockArray.contains(word)) mscore += 0.3
                top3MRP.forEach { topMrp ->
                    if (topMrp.toDouble() == num) {
                        mscore += mrpadder
                        mrpadder -= 0.1
                    }
                }
                if (word.contains("/-") && i > 1 && wordsArray[i - 1].toDoubleOrNull() != null) {
                    mscoreArr[i - 1] += 0.8
                }
                mscoreArr[i] = mscore
            }
        }

        val maxIndex = mscoreArr.indices.maxByOrNull { mscoreArr[it] } ?: -1
        val m1 = wordsArray[maxIndex]
        val m1Score = mscoreArr[maxIndex]

        return arrayListOf(m1, m1Score, mscoreArr, top3MRP)
    }

//PRODUCT FUNCTION EXTRACTION
    fun extractProduct(text: Text): ArrayList<Any> {
        val recognizedText = text.text
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
                }
            }
        }

        val top5Elements = elementSizes.sortedByDescending { it.size }.take(3).map { it.element }
        val wordsArray = recognizedText.split("[\\s:;.]".toRegex()).filter { it.isNotEmpty() }.toTypedArray()

        if (wordsArray.isEmpty()) {
            return arrayListOf("Not found", 0.0, "Not found")
        }

        val scoreArr = MutableList(wordsArray.size) { 0.0 }
        val specialCharPattern = Regex("[^a-zA-Z0-9]")
        val moreThanThreeDigitsPattern = Regex("\\d{4,}")
        val alphabetOnlyPattern = Regex("^[a-zA-Z]+$")

        wordsArray.forEachIndexed { i, word ->
            var score = 0.0
            if (word.length >= 3) {
                score += when {
                    word.uppercase() == word && word.toDoubleOrNull() == null -> 0.2
                    word.capitalize() == word && word.toDoubleOrNull() == null -> 0.16
                    else -> 0.08
                }
                if (wordsArray.size > 15) score -= 0.3
                if (wordsArray.size < 5) score += 0.2
                if (alphabetOnlyPattern.matches(word)) score += 0.15
                if (specialCharPattern.containsMatchIn(word)) score -= 0.4
                if (moreThanThreeDigitsPattern.containsMatchIn(word)) score -= 0.4
                if (i > 0 && wordsArray[i - 1].contains(Regex("\\b(item|product|tem|roduct|ite|produc|roduc|tfm)\\b", RegexOption.IGNORE_CASE))) score += 1
                top5Elements.forEachIndexed { index, element ->
                    if (element == word) score += 0.5 - index * 0.1
                }
            }
            scoreArr[i] = score
        }

        val maxIndex = scoreArr.indices.maxByOrNull { scoreArr[it] } ?: -1
        val max1 = wordsArray.getOrElse(maxIndex) { "Not found" }
        val max1Score = scoreArr.getOrElse(maxIndex) { 0.0 }

        var finalProd = max1
        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    if (max1 == element.text) {
                        finalProd = block.text
                        break
                    }
                }
            }
        }

        val wordsArrayReturn = wordsArray.joinToString(prefix = "[", postfix = "]", separator = ", ")
        val finalProdArray = finalProd.split("\\s".toRegex()).filter { it.isNotEmpty() }.toTypedArray()

        if (specialCharPattern.containsMatchIn(finalProd) && finalProdArray.size > 3) {
            finalProd = "Not found"
        }

        return arrayListOf(finalProd, max1Score, wordsArrayReturn)
    }

//DATE FUNCTION EXTRACTION
    fun extractDates(text: String): Pair<String?, String?> {
        val potentialDates = mutableListOf<String>()

        val dateRegex = Regex("""(?ix)
        \s*
        (?:
          (\d{1,2})[/.](\d{1,2})[/.](\d{4}) |  # Format: DD/MM/YYYY, DD.MM.YYYY
          (\d{1,2})[/.](\d{1,2})[/.](\d{2}) |  # Format: DD/MM/YY, DD.MM.YY
          (\d{4})[/.](\d{1,2})[/.](\d{1,2}) |  # Format: YYYY/MM/DD, YYYY.MM.DD
          (\d{1,2})/(\d{4}) |  # Format: MM/YYYY
          (\d{1,2})/(\d{2}) |  # Format: MM/YY
          (\d{1,2})-(\d{1,2})-(\d{4}) |  # Format: DD-MM-YYYY
          (\d{1,2})-(\d{1,2})-(\d{2}) |  # Format: DD-MM-YY
          (\d{4})-(\d{1,2})-(\d{1,2}) |
          \d{2}\s*[/.\s]\s*\d{2}\s*[/.\s]\s*(?:\d{2}|\d{4})|\d{2}\s*[A-Z]{3}\s*\d{2} |
          (\d{1,2})\s*([Jj]an|[Ff]eb|[Mm]ar|[Aa]pr|[Mm]ay|[Jj]un|[Jj]ul|[Aa]ug|[Ss]ep|[Oo]ct|[Nn]ov|[Dd]ec)(\d{4}) |  # DD MMM YYYY
          ([Jj]an|[Ff]eb|[Mm]ar|[Aa]pr|[Mm]ay|[Jj]un|[Jj]ul|[Aa]ug|[Ss]ep|[Oo]ct|[Nn]ov|[Dd]ec)(\d{4}) | #MMM YYYY
          ([Jj]an|[Ff]eb|[Mm]ar|[Aa]pr|[Mm]ay|[Jj]un|[Jj]ul|[Aa]ug|[Ss]ep|[Oo]ct|[Nn]ov|[Dd]ec)(\d{2}) #MMM YY
        )
        \s*
    """.trimMargin())

        dateRegex.findAll(text).forEach { match ->
            potentialDates.add(match.value)
            Log.i(ContentValues.TAG, match.value)
        }

        if (potentialDates.isEmpty()) {
            return null to null
        }

        fun parseDate(dateStr: String): Date? {
            val formats = listOf(
                "dd/MM/yyyy", "MM/dd/yyyy", "yyyy/MM/dd",
                "dd-MM-yyyy", "MM-dd-yyyy", "yyyy-MM-dd",
                "dd.MM.yyyy", "MM.dd.yyyy", "yyyy.MM.dd",
                "dd MMM yyyy", "MMM dd yyyy", "yyyy MMM dd",
                "dd/MM/yy", "MM/dd/yy", "yy/MM/dd",
                "dd-MM-yy", "MM-dd-yy", "yy-MM-dd",
                "dd.MM.yy", "MM.dd.yy", "MM/yyyy",
                "MMM yyyy", "MMM yy", "MM/yy"
            ).map { SimpleDateFormat(it, Locale.ENGLISH) }

            return formats.asSequence()
                .mapNotNull { format -> runCatching { format.parse(dateStr) }.getOrNull() }
                .firstOrNull()
        }

        val sortedDates = potentialDates.mapNotNull { dateStr -> parseDate(dateStr)?.let { dateStr to it } }
            .sortedBy { it.second }
            .map { it.first }

        val manufacturingDate = sortedDates.firstOrNull()
        var expiryDate = sortedDates.getOrNull(1)

        val expiry = Regex("""(?ix)(Best Before| Use Before)""")
        val lines = text.split("\n")

        expiryDate = lines.find { it.contains(expiry) } ?: expiryDate

        return manufacturingDate to expiryDate
    }




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