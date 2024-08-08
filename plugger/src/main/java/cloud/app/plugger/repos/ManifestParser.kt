package cloud.app.plugger.repos

import cloud.app.plugger.PluginMetadata

interface ManifestParser<TInputData> {
  fun parse(data: TInputData): PluginMetadata
}
