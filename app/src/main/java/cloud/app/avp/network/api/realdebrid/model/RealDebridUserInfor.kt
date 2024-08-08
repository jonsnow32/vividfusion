package cloud.app.avp.network.api.realdebrid.model

import cloud.app.avp.utils.TimeUtils
import org.threeten.bp.Instant

class RealDebridUserInfor(
    var avatar: String,
    var email: String,
    var expiration: String,
    var id: Int,
    var locale: String,
    var points: Int,
    var premium: Int,
    var type: String,
    var username: String,
) {
    override fun toString(): String {

        var expired = TimeUtils.format(Instant.parse(expiration))
        return "Email: '$email', " +
                "\nExpiration: '$expired', " +
                "\nId: $id, " +
                "\nLocale: '$locale', " +
                "\nPoints: $points, " +
                "\nPremium: $premium, " +
                "\nType: '$type', " +
                "\nUsername: '$username'"
    }
}
