package cloud.app.vvf.extension.tmdb.services.tmdb.model

class WatchProviders {
    var id: Int? = null

    /**
     * Mapped by ISO 3166-1 two letter country code, e.g. DE and US.
     */

    var results: MutableMap<String?, CountryInfo?> = HashMap<String?, CountryInfo?>()

    class CountryInfo {
        /**
         * Link to this page to display all options and provide deep links to the actual providers (and to support
         * TMDB).
         */
        var link: String? = null


        var flatrate: MutableList<WatchProvider> = ArrayList<WatchProvider>()


        var rent: MutableList<WatchProvider> = ArrayList<WatchProvider>()


        var free: MutableList<WatchProvider> = ArrayList<WatchProvider>()


        var ads: MutableList<WatchProvider> = ArrayList<WatchProvider>()


        var buy: MutableList<WatchProvider> = ArrayList<WatchProvider>()
    }

    class WatchProvider {
        var display_priority: Int? = null
        var logo_path: String? = null
        var provider_id: Int? = null
        var provider_name: String? = null
    }
}
