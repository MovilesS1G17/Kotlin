package com.centralia.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * The SwiftUI screens build their view model inline in `init` and hold it in
 * `@State`, which keeps it alive for the lifetime of the view.
 *
 * `@State` has no direct Compose counterpart that also survives a configuration
 * change, so each screen's model is a real [ViewModel] and this helper supplies
 * one built with the repositories the screen was handed. [key] distinguishes
 * models of the same type that belong to different rows — a folder detail screen
 * per folder, for instance — the way a fresh SwiftUI view instance would.
 */
@Composable
inline fun <reified VM : ViewModel> centraliaViewModel(
    key: String? = null,
    crossinline build: () -> VM
): VM = viewModel(
    key = key,
    factory = viewModelFactory { initializer { build() } }
)
