package cloud.app.avp.features.player.playerContracts

import cloud.app.avp.R
import cloud.app.avp.features.player.data.PlayerType

class MxFreeContract(listener: PlayBackListener) : MxProContract(listener) {
    override val packageName: String
        get() = "com.mxtech.videoplayer.ad"

    override val drawable: Int
        get() = R.drawable.mx_player

    override fun getType(): String {
        return PlayerType.MXFree.name
    }
}
