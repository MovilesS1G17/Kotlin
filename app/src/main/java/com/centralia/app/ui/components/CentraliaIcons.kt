package com.centralia.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.centralia.app.domain.library.FolderSymbol

/**
 * SF Symbols have no Android counterpart, so every symbol name the SwiftUI code
 * passed to `Image(systemName:)` is mapped once here to the nearest Material
 * icon. Keeping the mapping in one place means a screen reads the same as its
 * Swift original and the choice of glyph is reviewable in a single file.
 *
 * | SF Symbol                    | Material icon      |
 * |------------------------------|--------------------|
 * | house                        | Home               |
 * | magnifyingglass              | Search             |
 * | plus                         | Add                |
 * | folder                       | Folder             |
 * | person                       | Person             |
 * | play.fill / pause.fill       | PlayArrow / Pause  |
 * | ellipsis                     | MoreHoriz          |
 * | trash                        | Delete             |
 * | square.and.arrow.up          | Share              |
 * | sparkle / sparkles           | AutoAwesome        |
 * | chevron.left                 | ArrowBack          |
 * | chevron.right                | ChevronRight       |
 * | chevron.down                 | KeyboardArrowDown  |
 * | checkmark                    | Check              |
 * | xmark                        | Close              |
 * | xmark.circle.fill            | Cancel             |
 * | eye / eye.slash              | Visibility / Off   |
 * | tray                         | Inbox              |
 * | pencil                       | Edit               |
 * | exclamationmark.triangle     | WarningAmber       |
 * | clock.arrow.circlepath       | History            |
 * | envelope.badge               | Email              |
 * | doc.text                     | Description        |
 * | link                         | Link               |
 * | folder.badge.questionmark    | Help               |
 * | folder.badge.plus            | CreateNewFolder    |
 * | rectangle.stack              | Layers             |
 * | play.rectangle               | PlayCircleOutline  |
 * | tag                          | Sell               |
 * | exclamationmark.bubble       | Report             |
 * | plus.circle                  | AddCircleOutline   |
 * | checkmark.circle.fill        | CheckCircle        |
 * | person.text.rectangle        | AccountCircle      |
 */
object CentraliaIcons {
    val Home = Icons.Outlined.Home
    val Search = Icons.Outlined.Search
    val Add = Icons.Outlined.Add
    val Folder = Icons.Outlined.Folder
    val Person = Icons.Outlined.Person
    val Play = Icons.Filled.PlayArrow
    val Pause = Icons.Filled.Pause
    val More = Icons.Filled.MoreHoriz
    val Delete = Icons.Outlined.Delete
    val Share = Icons.Outlined.Share
    val Sparkle = Icons.Outlined.AutoAwesome
    val Back = Icons.AutoMirrored.Filled.ArrowBack
    val ChevronRight = Icons.Outlined.ChevronRight
    val ChevronDown = Icons.Filled.KeyboardArrowDown
    val Check = Icons.Outlined.Check
    val Close = Icons.Outlined.Close
    val ClearCircle = Icons.Filled.Cancel
    val Show = Icons.Outlined.Visibility
    val Hide = Icons.Outlined.VisibilityOff
    val Tray = Icons.Outlined.Inbox
    val Pencil = Icons.Outlined.Edit
    val Warning = Icons.Outlined.WarningAmber
    val RecentSearch = Icons.Outlined.History
    val Envelope = Icons.Outlined.Email
    val Document = Icons.Outlined.Description
    val Link = Icons.Outlined.Link
    val FolderUnknown = Icons.Outlined.Help
    val NewFolder = Icons.Outlined.CreateNewFolder
    val VideoStack = Icons.Outlined.Layers
    val PlayRectangle = Icons.Outlined.PlayCircleOutline
    val Tag = Icons.Outlined.Sell
    val ReportIssue = Icons.Outlined.Report
    val AddCircle = Icons.Outlined.AddCircleOutline
    val CheckCircle = Icons.Filled.CheckCircle
    val ProfileCard = Icons.Outlined.AccountCircle
}

/**
 * `LibraryFolder.symbolName` stores a raw SF Symbol string in the persisted
 * JSON (`"sun.max"`, `"fork.knife"`, …). The data format is kept byte-compatible
 * with the iOS store, so the name is resolved to an icon at render time.
 */
fun folderSymbolIcon(symbolName: String): ImageVector =
    when (FolderSymbol.fromRawValue(symbolName)) {
        FolderSymbol.FOLDER -> Icons.Outlined.Folder
        FolderSymbol.HOUSE -> Icons.Outlined.Home
        FolderSymbol.PAINT_PALETTE -> Icons.Outlined.Palette
        FolderSymbol.FORK_AND_KNIFE -> Icons.Outlined.Restaurant
        FolderSymbol.BOOKS -> Icons.Outlined.MenuBook
        FolderSymbol.LIGHTBULB -> Icons.Outlined.Lightbulb
        FolderSymbol.BRIEFCASE -> Icons.Outlined.BusinessCenter
        FolderSymbol.HEART -> Icons.Outlined.FavoriteBorder
        FolderSymbol.GRADUATION_CAP -> Icons.Outlined.School
        FolderSymbol.CAMERA -> Icons.Outlined.PhotoCamera
        FolderSymbol.MUSIC_NOTE -> Icons.Outlined.MusicNote
        FolderSymbol.SUN -> Icons.Outlined.WbSunny
    }

fun folderSymbolIcon(symbol: FolderSymbol): ImageVector = folderSymbolIcon(symbol.rawValue)
