package cloud.app.vvf.network.api.alldebrid.model

import cloud.app.vvf.utils.TimeUtils


data class ADUserInfo(
    val `data`: Data,
    val status: String
)

data class Data(
    val user: User
)

data class User(
    val email: String,
    val fidelityPoints: Int,
    val isPremium: Boolean,
    val isSubscribed: Boolean,
    val isTrial: Boolean,
    val lang: String,
    val limitedHostersQuotas: LimitedHostersQuotas,
    val notifications: List<Any>,
    val preferedDomain: String,
    val premiumUntil: Long,
    val username: String
) {
    override fun toString(): String {
        val expired = TimeUtils.convertTimestampToLocalTime(premiumUntil)
        return "Username: $username " +
                "\nEmail: $email " +
                "\nIsPremium: $isPremium " +
                "\nIsTrial: $isTrial" +
                "\nPremiumUntil: $expired"
    }

    fun toShortString(): String {
        val expired = TimeUtils.convertTimestampToLocalTime(premiumUntil)
        return "Username: $username " +
                "\nPremiumUntil: $expired"
    }
}
