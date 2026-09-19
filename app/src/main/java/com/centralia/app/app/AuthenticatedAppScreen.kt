package com.centralia.app.app

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.feature.folders.FolderDetailScreen
import com.centralia.app.feature.folders.FoldersScreen
import com.centralia.app.feature.library.LibraryScreen
import com.centralia.app.feature.profile.ProfileScreen
import com.centralia.app.feature.save.SaveVideoScreen
import com.centralia.app.feature.search.SearchScreen
import com.centralia.app.feature.smartorg.SmartOrganizationScreen
import com.centralia.app.feature.videodetail.VideoDetailScreen
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import java.util.UUID

/** Every destination in the authenticated graph. */
private object Routes {
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val FOLDERS = "folders"
    const val PROFILE = "profile"
    const val SAVE = "save"
    const val SMART_ORGANIZATION = "smartOrganization"
    const val VIDEO_DETAIL = "videoDetail/{videoId}"
    const val FOLDER_DETAIL = "folderDetail/{folderId}"

    fun videoDetail(videoID: UUID) = "videoDetail/$videoID"
    fun folderDetail(folderID: UUID) = "folderDetail/$folderID"
}

/** The four routes that keep the bottom bar visible. */
private val topLevelRoutes = setOf(Routes.LIBRARY, Routes.SEARCH, Routes.FOLDERS, Routes.PROFILE)

/**
 * `struct AuthenticatedAppView` — the tabbed shell.
 *
 * iOS uses a `TabView` where each tab owns a `NavigationStack` and a push hides
 * the tab bar. The Compose equivalent is one [NavHost]: the four tab routes show
 * the [NavigationBar] and every pushed destination covers it, which is also how
 * the system back gesture and process death are handled correctly.
 *
 * The centre "Save" tab is not a tab at all in either build — selecting it opens
 * the save flow and leaves the previous tab selected.
 */
@Composable
fun AuthenticatedAppScreen(
    user: AuthenticatedUser,
    container: DependencyContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showsBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CentraliaColors.Canvas,
        contentColor = CentraliaColors.Ink,
        bottomBar = {
            if (showsBottomBar) {
                CentraliaBottomBar(
                    currentRoute = currentRoute,
                    onSelectTab = { route -> navController.switchTab(route) },
                    onPresentSave = { navController.navigate(Routes.SAVE) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARY,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    videoRepository = container.videoItemRepository,
                    folderRepository = container.folderRepository,
                    revisions = container.revisions,
                    onOpenSearchTab = { navController.switchTab(Routes.SEARCH) },
                    onPresentSave = { navController.navigate(Routes.SAVE) },
                    onOpenVideo = { navController.navigate(Routes.videoDetail(it.id)) },
                    onOpenFolder = { navController.navigate(Routes.folderDetail(it.id)) }
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    videoRepository = container.videoItemRepository,
                    folderRepository = container.folderRepository,
                    searchHistoryRepository = container.searchHistoryRepository,
                    revisions = container.revisions,
                    onOpenVideo = { navController.navigate(Routes.videoDetail(it.id)) }
                )
            }

            composable(Routes.FOLDERS) {
                FoldersScreen(
                    videoRepository = container.videoItemRepository,
                    folderRepository = container.folderRepository,
                    revisions = container.revisions,
                    onOpenFolder = { navController.navigate(Routes.folderDetail(it.id)) },
                    onOpenSmartOrganization = {
                        navController.navigate(Routes.SMART_ORGANIZATION)
                    }
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    authenticatedUser = user,
                    userRepository = container.userRepository,
                    videoRepository = container.videoItemRepository,
                    folderRepository = container.folderRepository,
                    exportService = container.libraryExportService,
                    authenticationRepository = container.authenticationRepository,
                    onUserChanged = container.session::updateAuthenticatedUser,
                    onSignedOut = container.session::signOut
                )
            }

            composable(Routes.SAVE) {
                SaveVideoScreen(
                    pipeline = container.videoImportPipeline,
                    videoRepository = container.videoItemRepository,
                    folderRepository = container.folderRepository,
                    onVideoSaved = container.revisions::videoSaved,
                    onClose = { navController.popBackStack() },
                    onOpenVideo = { video ->
                        // "View Video" replaces the save flow with the detail screen.
                        navController.popBackStack()
                        navController.navigate(Routes.videoDetail(video.id))
                    }
                )
            }

            composable(Routes.SMART_ORGANIZATION) {
                SmartOrganizationScreen(
                    videoRepository = container.videoItemRepository,
                    folderRepository = container.folderRepository,
                    suggestionPipeline = container.videoImportPipeline,
                    onBack = { navController.popBackStack() },
                    onLibraryChanged = {
                        container.revisions.libraryChanged()
                        container.revisions.foldersChanged()
                    },
                    onOpenVideo = { navController.navigate(Routes.videoDetail(it.id)) }
                )
            }

            composable(Routes.VIDEO_DETAIL) { entry ->
                val videoID = entry.arguments
                    ?.getString("videoId")
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }

                if (videoID == null) {
                    navController.popBackStack()
                } else {
                    VideoDetailScreen(
                        videoID = videoID,
                        videoRepository = container.videoItemRepository,
                        folderRepository = container.folderRepository,
                        onBack = { navController.popBackStack() },
                        onVideoChanged = container.revisions::videoChanged,
                        onVideoDeleted = container.revisions::videoDeleted
                    )
                }
            }

            composable(Routes.FOLDER_DETAIL) { entry ->
                val folderID = entry.arguments
                    ?.getString("folderId")
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }

                if (folderID == null) {
                    navController.popBackStack()
                } else {
                    FolderDetailScreen(
                        folderID = folderID,
                        videoRepository = container.videoItemRepository,
                        folderRepository = container.folderRepository,
                        onBack = { navController.popBackStack() },
                        onFolderChanged = {
                            container.revisions.libraryChanged()
                            container.revisions.foldersChanged()
                        },
                        onOpenVideo = { navController.navigate(Routes.videoDetail(it.id)) }
                    )
                }
            }
        }
    }
}

/**
 * The five items of the SwiftUI `TabView`, tinted ink. "Save" sits in the middle
 * and opens the save flow rather than selecting a tab, which is what the
 * `onChange(of: selectedTab)` handler achieves on iOS.
 */
@Composable
private fun CentraliaBottomBar(
    currentRoute: String?,
    onSelectTab: (String) -> Unit,
    onPresentSave: () -> Unit
) {
    NavigationBar(
        containerColor = CentraliaColors.Surface,
        contentColor = CentraliaColors.Ink
    ) {
        BottomBarItem(
            label = "Library",
            icon = CentraliaIcons.Home,
            isSelected = currentRoute == Routes.LIBRARY,
            onClick = { onSelectTab(Routes.LIBRARY) }
        )

        BottomBarItem(
            label = "Search",
            icon = CentraliaIcons.Search,
            isSelected = currentRoute == Routes.SEARCH,
            onClick = { onSelectTab(Routes.SEARCH) }
        )

        BottomBarItem(
            label = "Save",
            icon = CentraliaIcons.Add,
            isSelected = false,
            onClick = onPresentSave
        )

        BottomBarItem(
            label = "Folders",
            icon = CentraliaIcons.Folder,
            isSelected = currentRoute == Routes.FOLDERS,
            onClick = { onSelectTab(Routes.FOLDERS) }
        )

        BottomBarItem(
            label = "Profile",
            icon = CentraliaIcons.Person,
            isSelected = currentRoute == Routes.PROFILE,
            onClick = { onSelectTab(Routes.PROFILE) }
        )
    }
}

@Composable
private fun RowScope.BottomBarItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
        label = { Text(text = label, style = CentraliaType.caption) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = CentraliaColors.Surface,
            selectedTextColor = CentraliaColors.Ink,
            indicatorColor = CentraliaColors.Ink,
            unselectedIconColor = CentraliaColors.SecondaryText,
            unselectedTextColor = CentraliaColors.SecondaryText
        )
    )
}

/**
 * Switching tabs keeps each tab's own back stack and scroll state, which is what
 * a SwiftUI `TabView` does for free.
 */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
