package cloud.app.avp.network.api.realdebrid.model

import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject

class InstantAvailabilityObject {
    private var hashInstances: Map<String, InstanceObj>? = null
    fun getHashInstances(): Map<String, InstanceObj>? {
        return hashInstances
    }

    fun setHashInstances(hashInstances: Map<String, InstanceObj>?) {
        this.hashInstances = hashInstances
    }

    class InstanceObj {
        var rd: List<Map<String, RdBean>>? = null

        class RdBean {
            var filename: String? = null
            var filesize: Long = 0
        }
    }

    companion object {
        /**
         * b448bd34e525d2e6067a6714cec48a66cd81492d : {"rd":[{"1":{"filename":"Ghost.Whisperer.S01E01.avi","filesize":365644398},"2":{"filename":"Ghost.Whisperer.S01E02.avi","filesize":365593536},"3":{"filename":"Ghost.Whisperer.S01E03.avi","filesize":365560334},"4":{"filename":"Ghost.Whisperer.S01E04.avi","filesize":365576182},"5":{"filename":"Ghost.Whisperer.S01E05.avi","filesize":365366798},"6":{"filename":"Ghost.Whisperer.S01E06.avi","filesize":367955968},"7":{"filename":"Ghost.Whisperer.S01E07.avi","filesize":366845952},"8":{"filename":"Ghost.Whisperer.S01E08.avi","filesize":366978078},"9":{"filename":"Ghost.Whisperer.S01E09.avi","filesize":367214592},"10":{"filename":"Ghost.Whisperer.S01E10.avi","filesize":365723446},"11":{"filename":"Ghost.Whisperer.S01E11.avi","filesize":368582656},"12":{"filename":"Ghost.Whisperer.S01E12.avi","filesize":366888960},"13":{"filename":"Ghost.Whisperer.S01E13.avi","filesize":352258048}},{"11":{"filename":"Ghost.Whisperer.S01E11.avi","filesize":368582656}}]}
         */
        @Throws(JSONException::class)
        fun deserialize(json: String?): InstantAvailabilityObject {
            val jsonObject = JSONObject(json)
            val names = jsonObject.names()
            val demons: MutableMap<String, InstanceObj> = HashMap()
            val gson = Gson()
            var demon: InstanceObj
            for (i in 0 until names.length()) {
                if (jsonObject[names.getString(i)] is JSONObject) {
                    demon = gson.fromJson(
                        jsonObject[names.getString(i)].toString(),
                        InstanceObj::class.java
                    )
                    demons[names.getString(i)] = demon
                }
            }
            val instantAvailabilityObject = InstantAvailabilityObject()
            instantAvailabilityObject.hashInstances = demons
            return instantAvailabilityObject
        }
    }
}
