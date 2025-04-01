package cloud.app.vvf.common.exceptions

class AppPermissionRequiredException(
  val extensionId: String,
  val clientName: String,
  val permissionName: String
) : Exception("Permission $permissionName is required for extension ($extensionId : $clientName)")

