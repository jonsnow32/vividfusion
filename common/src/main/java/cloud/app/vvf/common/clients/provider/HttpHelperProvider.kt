package cloud.app.vvf.common.clients.provider

import cloud.app.vvf.common.helpers.network.HttpHelper

interface HttpHelperProvider {
  fun setHttpHelper(httpHelper: HttpHelper)
}
