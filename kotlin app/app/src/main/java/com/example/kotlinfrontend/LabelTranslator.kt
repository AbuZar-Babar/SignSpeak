package com.example.kotlinfrontend

object LabelTranslator {
    private val fallbackUrduMap = mapOf(
        "absolutely" to "بالکل",
        "aircrash" to "ہوائی حادثہ",
        "airplane" to "جہاز",
        "all" to "تمام",
        "also" to "بھی",
        "arrival" to "آمد",
        "assalam-o-alaikum" to "السلام علیکم",
        "atm" to "اے ٹی ایم",
        "bald" to "گنجا",
        "beach" to "ساحل",
        "beak" to "چونچ",
        "bear" to "ریچھ",
        "beard" to "داڑھی",
        "bed" to "بستر",
        "bench" to "بینچ",
        "bicycle" to "سائیکل",
        "both" to "دونوں",
        "bridge" to "پل",
        "bring" to "لانا",
        "bulb" to "بلب",
        "cartoon" to "کارٹون",
        "chimpanzee" to "چنپانزی",
        "color_pencils" to "رنگیلی پنسلیں",
        "cow" to "گائے",
        "crow" to "کوا",
        "cupboard" to "الماری",
        "deer" to "ہرن",
        "dog" to "کتا",
        "donttouch" to "ہاتھ نہ لگائیں",
        "door" to "دروازہ",
        "elephant" to "ہاتھی",
        "excuseme" to "معاف کیجئے گا",
        "facelotion" to "فیس لوشن",
        "fan" to "پنکھا",
        "garden" to "باغ",
        "generator" to "جنریٹر",
        "goodbye" to "خدا حافظ",
        "goodmorning" to "صبح بخیر",
        "have_a_good_day" to "آپ کا دن اچھا گزرے",
        "hello" to "ہیلو",
        "ihaveacomplaint" to "مجھے شکایت ہے",
        "left_hand" to "بایاں ہاتھ",
        "lifejacket" to "لائف جیکٹ",
        "mine" to "میرا",
        "mobile_phone" to "موبائل فون",
        "nailcutter" to "ناخن تراش",
        "nothing" to "کچھ نہیں",
        "peacock" to "مور",
        "policecar" to "پولیس کی گاڑی",
        "razor" to "استرا",
        "shampoo" to "شیمپو",
        "shower" to "شاور",
        "sunglasses" to "دھوپ کی عینک",
        "thankyou" to "شکریہ",
        "tissue" to "ٹشو",
        "toothbrush" to "ٹوتھ برش",
        "toothpaste" to "ٹوتھ پیسٹ",
        "umbrella" to "چھتری",
        "water" to "پانی",
        "we" to "ہم",
        "welldone" to "بہت اچھے",
        "you" to "آپ",
        "Waiting…" to "انتظار کریں...",
        "Forming sentence…" to "جملہ بن رہا ہے..."
    )

    fun translate(label: String): String {
        if (label == "--" || label.isBlank()) return label

        TranslationCatalog.lookup(label)?.let { return it }

        val cleaned = label.lowercase().replace('_', ' ').replace('-', ' ').trim()
        
        fallbackUrduMap[cleaned]?.let { return it }
        
        val underscoreLabel = label.lowercase().trim()
        fallbackUrduMap[underscoreLabel]?.let { return it }

        val spacedLabel = label.lowercase().replace('_', ' ').replace('-', ' ').trim()
        fallbackUrduMap[spacedLabel]?.let { return it }

        return label // Fallback to original
    }

    fun formatLabel(label: String, language: SentenceLanguage): String {
        if (label == "--" || label.isBlank()) return label
        
        val urdu = translate(label)
        val english = label.replace('_', ' ')

        return when (language) {
            SentenceLanguage.URDU -> urdu
            SentenceLanguage.ENGLISH -> english
            SentenceLanguage.BOTH -> "$urdu ($english)"
        }
    }

    fun formatTranscript(transcript: String, language: SentenceLanguage): String {
        if (transcript.isBlank() || transcript == "—") return transcript
        
        val words = transcript.split(" ")
        val urduWords = words.map { translate(it) }
        
        return when (language) {
            SentenceLanguage.URDU -> urduWords.joinToString(" ")
            SentenceLanguage.ENGLISH -> transcript
            SentenceLanguage.BOTH -> {
                val urduLine = urduWords.joinToString(" ")
                "$urduLine | $transcript"
            }
        }
    }
}
