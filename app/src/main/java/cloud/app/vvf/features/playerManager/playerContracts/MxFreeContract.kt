package cloud.app.vvf.features.playerManager.playerContracts

import cloud.app.vvf.R
import cloud.app.vvf.features.playerManager.data.PlayerType

class MxFreeContract(listener: PlayBackListener) : MxProContract(listener) {
    override val packageName: String
        get() = "com.mxtech.videoplayer.ad"

    override val drawable: Int
        get() = R.drawable.mx_player

    override fun getType(): String {
        return PlayerType.MXFree.name
    }
}
