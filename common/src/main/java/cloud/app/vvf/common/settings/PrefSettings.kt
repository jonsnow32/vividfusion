package cloud.app.vvf.common.settings

interface PrefSettings {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
    fun getInt(key: String): Int?
    fun putInt(key:String, value: Int?)
    fun getBoolean(key: String): Boolean?
    fun putBoolean(key:String, value: Boolean?)
}
