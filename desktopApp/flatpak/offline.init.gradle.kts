val offlineRepository: String = System.getenv("ARGOSY_OFFLINE_REPOSITORY")
    ?: error("ARGOSY_OFFLINE_REPOSITORY must point at the offline Maven repository")

settingsEvaluated {
    pluginManagement.repositories.clear()
    pluginManagement.repositories.maven { url = uri(offlineRepository) }
    dependencyResolutionManagement.repositories.clear()
    dependencyResolutionManagement.repositories.maven { url = uri(offlineRepository) }
    dependencyResolutionManagement.repositories.maven { url = uri("${settings.rootDir}/libs/maven") }
}
