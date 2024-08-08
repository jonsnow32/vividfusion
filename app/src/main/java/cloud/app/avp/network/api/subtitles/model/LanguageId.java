package cloud.app.avp.network.api.subtitles.model;

import java.util.ArrayList;

public class LanguageId {
    static LanguageId _instance;
    private ArrayList<Language> supportList;

    public static LanguageId getInstance() {
        if (_instance == null) {
            _instance = new LanguageId();
            _instance.supportList = new ArrayList<>();
            _instance.supportList.add(new Language("abk", "-1", "ab", "Abkhazian"));
            _instance.supportList.add(new Language("afr", "-1", "af", "Afrikaans"));
            _instance.supportList.add(new Language("alb", "1", "sq", "Albanian"));
            _instance.supportList.add(new Language("ara", "2", "ar", "Arabic"));
            _instance.supportList.add(new Language("arg", "-1", "an", "Aragonese"));
            _instance.supportList.add(new Language("arm", "73", "hy", "Armenian"));
            _instance.supportList.add(new Language("ast", "-1", "at", "Asturian"));
            _instance.supportList.add(new Language("aze", "55", "az", "Azerbaijani"));
            _instance.supportList.add(new Language("baq", "74", "eu", "Basque"));
            _instance.supportList.add(new Language("bel", "68", "be", "Belarusian"));
            _instance.supportList.add(new Language("ben", "54", "bn", "Bengali"));
            _instance.supportList.add(new Language("bos", "60", "bs", "Bosnian"));
            _instance.supportList.add(new Language("bre", "-1", "br", "Breton"));
            _instance.supportList.add(new Language("bul", "5", "bg", "Bulgarian"));
            _instance.supportList.add(new Language("bur", "61", "my", "Burmese"));
            _instance.supportList.add(new Language("cat", "49", "ca", "Catalan"));
            _instance.supportList.add(new Language("chi", "7", "zh", "Chinese (simplified)"));
            _instance.supportList.add(new Language("cze", "9", "cs", "Czech"));
            _instance.supportList.add(new Language("dan", "10", "da", "Danish"));
            _instance.supportList.add(new Language("dut", "11", "nl", "Dutch"));
            _instance.supportList.add(new Language("eng", "13", "en", "English"));
            _instance.supportList.add(new Language("epo", "47", "eo", "Esperanto"));
            _instance.supportList.add(new Language("est", "16", "et", "Estonian"));
            _instance.supportList.add(new Language("fin", "17", "fi", "Finnish"));
            _instance.supportList.add(new Language("fre", "18", "fr", "French"));
            _instance.supportList.add(new Language("geo", "62", "ka", "Georgian"));
            _instance.supportList.add(new Language("ger", "19", "de", "German"));
            _instance.supportList.add(new Language("gla", "-1", "gd", "Gaelic"));
            _instance.supportList.add(new Language("gle", "-1", "ga", "Irish"));
            _instance.supportList.add(new Language("glg", "-1", "gl", "Galician"));
            _instance.supportList.add(new Language("ell", "21", "el", "Greek"));
            _instance.supportList.add(new Language("heb", "22", "he", "Hebrew"));
            _instance.supportList.add(new Language("hin", "51", "hi", "Hindi"));
            _instance.supportList.add(new Language("hrv", "8", "hr", "Croatian"));
            _instance.supportList.add(new Language("hun", "23", "hu", "Hungarian"));
            _instance.supportList.add(new Language("ibo", "-1", "ig", "Igbo"));
            _instance.supportList.add(new Language("ice", "25", "is", "Icelandic"));
            _instance.supportList.add(new Language("ina", "-1", "ia", "Interlingua"));
            _instance.supportList.add(new Language("ind", "44", "id", "Indonesian"));
            _instance.supportList.add(new Language("ita", "26", "it", "Italian"));
            _instance.supportList.add(new Language("jpn", "27", "ja", "Japanese"));
            _instance.supportList.add(new Language("kan", "78", "kn", "Kannada"));
            _instance.supportList.add(new Language("kaz", "-1", "kk", "Kazakh"));
            _instance.supportList.add(new Language("khm", "-1", "km", "Khmer"));
            _instance.supportList.add(new Language("kor", "28", "ko", "Korean"));
            _instance.supportList.add(new Language("kur", "52", "ku", "Kurdish"));
            _instance.supportList.add(new Language("lav", "29", "lv", "Latvian"));
            _instance.supportList.add(new Language("lit", "43", "lt", "Lithuanian"));
            _instance.supportList.add(new Language("ltz", "-1", "lb", "Luxembourgish"));
            _instance.supportList.add(new Language("mac", "48", "mk", "Macedonian"));
            _instance.supportList.add(new Language("mal", "64", "ml", "Malayalam"));
            _instance.supportList.add(new Language("may", "50", "ms", "Malay"));
            _instance.supportList.add(new Language("mni", "65", "ma", "Manipuri"));
            _instance.supportList.add(new Language("mon", "72", "mn", "Mongolian"));
            _instance.supportList.add(new Language("nav", "-1", "nv", "Navajo"));
            _instance.supportList.add(new Language("nor", "30", "no", "Norwegian"));
            _instance.supportList.add(new Language("oci", "-1", "oc", "Occitan"));
            _instance.supportList.add(new Language("per", "-1", "fa", "Persian"));
            _instance.supportList.add(new Language("pol", "31", "pl", "Polish"));
            _instance.supportList.add(new Language("por", "32", "pt", "Portuguese"));
            _instance.supportList.add(new Language("rus", "34", "ru", "Russian"));
            _instance.supportList.add(new Language("scc", "35", "sr", "Serbian"));
            _instance.supportList.add(new Language("sin", "-1", "si", "Sinhalese"));
            _instance.supportList.add(new Language("slo", "36", "sk", "Slovak"));
            _instance.supportList.add(new Language("slv", "37", "sl", "Slovenian"));
            _instance.supportList.add(new Language("sme", "-1", "se", "Northern Sami"));
            _instance.supportList.add(new Language("snd", "-1", "sd", "Sindhi"));
            _instance.supportList.add(new Language("som", "70", "so", "Somali"));
            _instance.supportList.add(new Language("spa", "38", "es", "Spanish"));
            _instance.supportList.add(new Language("swa", "75", "sw", "Swahili"));
            _instance.supportList.add(new Language("swe", "39", "sv", "Swedish"));
            _instance.supportList.add(new Language("syr", "-1", "sy", "Syriac"));
            _instance.supportList.add(new Language("tam", "59", "ta", "Tamil"));
            _instance.supportList.add(new Language("tat", "-1", "tt", "Tatar"));
            _instance.supportList.add(new Language("tel", "63", "te", "Telugu"));
            _instance.supportList.add(new Language("tgl", "53", "tl", "Tagalog"));
            _instance.supportList.add(new Language("tha", "40", "th", "Thai"));
            _instance.supportList.add(new Language("tur", "41", "tr", "Turkish"));
            _instance.supportList.add(new Language("ukr", "56", "uk", "Ukrainian"));
            _instance.supportList.add(new Language("urd", "42", "ur", "Urdu"));
            _instance.supportList.add(new Language("vie", "45", "vi", "Vietnamese"));
            _instance.supportList.add(new Language("rum", "33", "ro", "Romanian"));
            _instance.supportList.add(new Language("pob", "-1", "pb", "Portuguese (BR)"));
            _instance.supportList.add(new Language("mne", "-1", "me", "Montenegrin"));
            _instance.supportList.add(new Language("zht", "7", "zt", "Chinese (traditional)"));
            _instance.supportList.add(new Language("zhe", "-1", "ze", "Chinese bilingual"));
            _instance.supportList.add(new Language("pom", "-1", "pm", "Portuguese (MZ)"));
            _instance.supportList.add(new Language("ext", "-1", "ex", "Extremaduran"));
        }
        return _instance;
    }

    public ArrayList<Language> getSupportList() {
        return supportList;
    }

    public String[] toLangArray() {
        String[] langArrays = new String[supportList.size()];
        for (int i = 0; i < supportList.size(); i++) {
            langArrays[i] = supportList.get(i).enName;
        }
        return langArrays;
    }

    ;

    public String[] toISO639LangArray() {
        String[] langArrays = new String[supportList.size()];
        for (int i = 0; i < supportList.size(); i++) {
            langArrays[i] = supportList.get(i).iSO639;
        }
        return langArrays;
    }

    ;

    public String[] toSubsceneLangArray() {
        String[] langArrays = new String[supportList.size()];
        for (int i = 0; i < supportList.size(); i++) {
            langArrays[i] = supportList.get(i).subsceneID;
        }
        return langArrays;
    }

    ;

    public String getOpenLangIDByIsoLang(String isoLang) {
        for (int i = 0; i < supportList.size(); i++) {
            if (supportList.get(i).iSO639.equals(isoLang))
                return supportList.get(i).getOpensubtitleLangID();
        }
        return "";
    }

    public String getSubsceneIDByIsoLang(String isoLang) {
        for (int i = 0; i < supportList.size(); i++) {
            if (supportList.get(i).iSO639.equals(isoLang) && !supportList.get(i).getSubsceneID().equals("-1"))
                return supportList.get(i).getSubsceneID();
        }
        return "";
    }

    public String getIso639ByLangName(String langName) {
        for (int i = 0; i < supportList.size(); i++) {
            if (supportList.get(i).enName.equals(langName))
                return supportList.get(i).iSO639;
        }
        return "unknown";
    }

    public String getLangNameByIso639(String iso639) {
        for (int i = 0; i < supportList.size(); i++) {
            if (supportList.get(i).iSO639.equals(iso639))
                return supportList.get(i).enName;
        }
        return "";
    }

    public static class Language {
        private String opensubtitleLangID;
        private String subsceneID;
        private String iSO639;
        private String enName;
        private boolean enable = false;

        public Language(String opensubtitleLangID, String subsceneID, String iSO639, String enName) {
            this.opensubtitleLangID = opensubtitleLangID;
            this.subsceneID = subsceneID;
            this.iSO639 = iSO639;
            this.enName = enName;
        }

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public String getOpensubtitleLangID() {
            return opensubtitleLangID;
        }

        public String getSubsceneID() {
            return subsceneID;
        }

        public String getiSO639() {
            return iSO639;
        }

        public String getEnName() {
            return enName;
        }
    }
}
