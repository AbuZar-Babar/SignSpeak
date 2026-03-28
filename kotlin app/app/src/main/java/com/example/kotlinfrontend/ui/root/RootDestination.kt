package com.example.kotlinfrontend.ui.root

sealed interface RootDestination {
    val route: String

    data object Onboarding : RootDestination {
        override val route: String = "root_onboarding"
    }

    data object Auth : RootDestination {
        override val route: String = "root_auth"
    }

    data object Main : RootDestination {
        override val route: String = "root_main"
    }
}
