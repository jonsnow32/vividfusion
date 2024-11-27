package cloud.app.vvf.common.helpers.network.utils

import java.util.regex.Matcher
import java.util.regex.Pattern

object RegexUtils {
    /* renamed from: 龘 */
    private val regex = Pattern.compile("[-+]?\\d+")

    /* renamed from: 龘 */
    fun check(str: String?): Boolean {
        return regex.matcher(str).matches()
    }

    fun getGroup(text: String, pattern: String, groupIndex: Int): String {
        val match = pattern.toRegex().find(text)
        if (match != null && match.groupValues.size > groupIndex) {
            return match.groupValues.get(groupIndex)
        }
        return ""
    }

    fun getGroup(text: String, pattern: String, groupIndex: Int, option: RegexOption): String {
        val match = Regex(pattern, option).find(text)
        if (match != null && match.groupValues.size > groupIndex) {
            return match.groupValues.get(groupIndex)
        }
        return ""
    }

    fun getGroup(str: String, str2: String, g: Int, z: Boolean): ArrayList<String> {
        val arrayList: ArrayList<String> = ArrayList<String>()
        if (str.isEmpty()) {
            return arrayList
        }
      val matcher: Matcher = if (z) {
          Pattern.compile(str2, 32).matcher(str)
      } else {
          Pattern.compile(str2).matcher(str)
      }
        while (matcher.find()) {
            if (!arrayList.contains(matcher.group(g))) arrayList.add(matcher.group(g))
        }
        return arrayList
    }

    fun getGroupArray(str: String?, str2: String, i: Int, i2: Int): ArrayList<ArrayList<String>> {
        val matcher: Matcher? = str?.let { Pattern.compile(str2, i2).matcher(it) }
        val max: Int = matcher?.let { Math.max(it.groupCount(), i) } ?: i
        val arrayList: ArrayList<ArrayList<String>> = ArrayList()

        for (i3 in 0 until max) {
            arrayList.add(ArrayList())
        }
        if (arrayList.isEmpty()) {
            arrayList.add(ArrayList())
        }
        if (matcher == null || str.isEmpty()) {
            return arrayList
        }
        while (matcher.find()) {
            try {
                if (i == 0) {
                    arrayList[0].add(matcher.group(0))
                } else {
                    for (i3 in 0 until i) {
                        arrayList[i3].add(matcher.group(i3 + 1))
                    }
                }
            } catch (e: Throwable) {
                // Handle the exception if needed
            }
        }
        if (arrayList.isEmpty()) {
            arrayList.add(ArrayList())
        }
        return arrayList
    }

    fun getGroupArray(
        str: String?,
        str2: String?,
        i: Int,
        z: Boolean?
    ): ArrayList<ArrayList<String>> {
        var i2: Int
        val matcher =
            if (str == null) null else if (z == true) Pattern.compile(str2, Pattern.DOTALL)
                .matcher(str) else Pattern.compile(str2).matcher(str)
        val max = if (matcher == null) i else Math.max(matcher.groupCount(), i)
        val arrayList: ArrayList<ArrayList<String>> = ArrayList()
        if (max == 0) {
            arrayList.add(0, ArrayList())
        } else {
            i2 = 0
            while (i2 < max) {
                arrayList.add(i2, ArrayList())
                i2++
            }
        }
        if (arrayList.isEmpty()) {
            arrayList.add(ArrayList())
        }
        if (matcher == null || str!!.isEmpty()) {
            return arrayList
        }
        while (matcher.find()) {
            try {
                if (i == 0) {
                    (arrayList[0] )!!.add(matcher.group(0))
                } else {
                    i2 = 0
                    while (i2 < i) {
                        (arrayList[i2] )!!.add(matcher.group(i2 + 1))
                        i2++
                    }
                    continue
                }
            } catch (e: Throwable) {
                //Log.e("RegexUtils", "getGroupArray: ", e)
            }
        }
        if (arrayList.isEmpty()) {
            arrayList.add(ArrayList())
        }
        return arrayList
    }

    fun getGroup(str: String?, str2: String?, i: Int, i2: Int): String? {
        if (str == null || str.isEmpty()) {
            return ""
        }
        val matcher = Pattern.compile(str2, i2).matcher(str)
        return if (matcher.find()) {
            matcher.group(i)
        } else ""
    }

}
