package cloud.app.avp.plugin.getlink

import cloud.app.avp.extension.provider.M4UFree


class Providers (
    val m4uFree: M4UFree
) {

    fun getList(): List<BaseScaper> {
        return listOf(
            m4uFree,
        )
    }
}