/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag2

import com.vanpra.composematerialdialogs.MaterialDialogState
import lib.phonograph.misc.IOpenFileStorageAccess
import org.jaudiotagger.tag.FieldKey
import player.phonograph.R
import player.phonograph.mechanism.tag.edit.selectImage
import player.phonograph.model.RawTag
import player.phonograph.model.TagData
import player.phonograph.model.allFieldKey
import player.phonograph.model.getFileSizeString
import player.phonograph.model.text
import player.phonograph.ui.compose.components.CoverImage
import player.phonograph.ui.compose.components.Title
import player.phonograph.ui.compose.components.VerticalTextItem
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TagBrowserScreen(viewModel: TagBrowserViewModel) {
    val info by viewModel.currentSongInfo.collectAsState()
    val bitmap by viewModel.songBitmap.collectAsState()
    val editable by viewModel.editable.collectAsState()
    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .fillMaxSize()
    ) {
        // cover
        Artwork(viewModel, bitmap, editable)
        // file
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Title(stringResource(R.string.file))
            Item(R.string.label_file_name, info.fileName.value())
            Item(R.string.label_file_path, info.filePath.value())
            Item(R.string.label_file_size, getFileSizeString(info.fileSize.value()))
            for ((key, field) in info.audioPropertyFields) {
                Item(stringResource(key.res), value = field.value().toString())
            }
            // music tags
            Spacer(modifier = Modifier.height(16.dp))
            Title(stringResource(R.string.music_tags))
            Item(stringResource(R.string.tag_format), info.tagFormat.id)
            Spacer(modifier = Modifier.height(8.dp))
            for ((key, field) in info.tagTextOnlyFields) {
                CommonTag(key, field.content, editable, viewModel::process)
            }
            if (editable) AddMoreButton(viewModel)
            Spacer(modifier = Modifier.height(16.dp))
            Title(stringResource(R.string.raw_tags))
            for ((key, rawTag) in info.allTags) {
                RawTag(key, rawTag)
            }
        }
    }
    if (editable) {
        val context = LocalContext.current
        SaveConfirmationDialog(
            viewModel.saveConfirmationDialogState,
            viewModel::diff
        ) { viewModel.save(context) }
        val activity = context as? ComponentActivity
        ExitWithoutSavingDialog(viewModel.exitWithoutSavingDialogState) { activity?.finish() }
    }
}

@Composable
fun Artwork(viewModel: TagBrowserViewModel, bitmap: Bitmap?, editable: Boolean) {
    val context = LocalContext.current
    BoxWithConstraints {
        val coverImageDetailDialogState = remember { MaterialDialogState(false) }
        if (bitmap != null || editable) {
            CoverImage(bitmap, MaterialTheme.colors.primary, Modifier.clickable {
                coverImageDetailDialogState.show()
            })
        }
        CoverImageDetailDialog(
            state = coverImageDetailDialogState,
            artworkExist = bitmap != null,
            onSave = { viewModel.saveArtwork(context) },
            onDelete = { viewModel.process(context, TagEditEvent.RemoveArtwork) },
            onUpdate = {
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    val uri = selectImage((context as IOpenFileStorageAccess).openFileStorageAccessTool)
                    if (uri != null) {
                        viewModel.process(
                            context, TagEditEvent.UpdateArtwork.from(context, uri, viewModel.song.value)
                        )
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, android.R.string.cancel, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                coverImageDetailDialogState.hide()
            },
            editMode = editable
        )
    }
}


@Composable
private fun AddMoreButton(model: TagBrowserViewModel) {
    Box(Modifier.fillMaxWidth()) {
        val current by model.currentSongInfo.collectAsState()
        var showed by remember { mutableStateOf(false) }
        val fieldKeys = allFieldKey.subtract(current.tagTextOnlyFields.keys)

        DropdownMenu(expanded = showed, onDismissRequest = { showed = false }) {
            val context = LocalContext.current
            for (fieldKey in fieldKeys) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showed = false
                        model.process(context, TagEditEvent.AddNewTag(fieldKey))
                    }
                    .padding(8.dp, 16.dp)
                ) {
                    Text(
                        text = fieldKey.text(context.resources),
                        // modifier = Modifier.align(Alignment.Start),
                    )
                    Text(
                        text = fieldKey.name,
                        // modifier = Modifier.align(Alignment.End),
                        fontFamily = FontFamily.Monospace,
                        style = TextStyle(
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.77f),
                            fontSize = 8.sp,
                        )
                    )
                }
            }
        }
        TextButton(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .padding(0.dp, 16.dp),
            onClick = {
                showed = true
            }
        ) {
            Row(
                Modifier.fillMaxWidth()
            )
            {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add_white_24dp),
                    contentDescription = stringResource(id = R.string.add_action),
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.CenterVertically),
                    tint = MaterialTheme.colors.onSurface
                )
                Text(
                    text = stringResource(id = R.string.add_action),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .fillMaxWidth(),
                    color = MaterialTheme.colors.onSurface
                )
            }
        }
    }
}

@Composable
private fun CommonTag(
    key: FieldKey,
    field: TagData,
    editable: Boolean,
    onEdit: (Context, TagEditEvent) -> Unit,
) {
    val context = LocalContext.current
    val tagName = key.text(context.resources)
    val tagValue = field.text()

    Box(modifier = Modifier.fillMaxWidth()) {
        if (editable) {
            EditableItem(key, tagName, tagValue, onEdit = { onEdit(context, it) })
        } else {
            if (tagValue.isNotEmpty()) Item(tagName, tagValue)
        }
    }
}

@Composable
private fun EditableItem(
    key: FieldKey,
    tagName: String,
    value: String,
    alternatives: Collection<String> = emptyList(),
    onEdit: (TagEditEvent) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // tag name
        Text(
            text = tagName,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            ),
            modifier = Modifier
                .align(Alignment.Start),
        )
        // content
        val originalValue = remember(key) { value }
        var currentValue by remember(key) { mutableStateOf(value) }
        var hasEdited by remember(key) { mutableStateOf(false) }


        var indicatorColor by remember(key, originalValue) {
            mutableStateOf(Color(0xFF707070))
        }


        fun updateValue(newValue: String) {
            currentValue = newValue
            hasEdited = newValue != originalValue
            indicatorColor = if (hasEdited) Color(0xFFF59C01) else Color(0xFF707070)
        }

        fun submit() {
            onEdit.invoke(
                TagEditEvent.UpdateTag(key, currentValue)
            )
            hasEdited = false
            indicatorColor = Color(0xFF00C72C)
        }

        var showDropdownMenu by remember { mutableStateOf(false) }

        TextField(
            value = currentValue,
            onValueChange = ::updateValue,
            modifier = Modifier
                .align(Alignment.Start)
                .fillMaxWidth(),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.background,
                textColor = MaterialTheme.colors.onSurface,
                focusedIndicatorColor = indicatorColor.copy(alpha = TextFieldDefaults.IconOpacity),
                unfocusedIndicatorColor = Color.Transparent,
            ),
            textStyle = TextStyle(fontSize = 14.sp),
            trailingIcon = {
                Row {
                    if (hasEdited) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(id = android.R.string.ok),
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable { submit() }
                        )
                    }
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(id = R.string.more_actions),
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable { showDropdownMenu = !showDropdownMenu }
                    )
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.delete_action),
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                onEdit.invoke(TagEditEvent.RemoveTag(key))
                            }
                    )
                }
            }
        )
        DropdownMenu(
            expanded = showDropdownMenu,
            modifier = Modifier.fillMaxWidth(0.7f),
            onDismissRequest = {
                showDropdownMenu = false
            }
        ) {
            fun onClick(value: String) {
                showDropdownMenu = false
                updateValue(value)
            }
            for (alternative in alternatives) {
                DropdownMenuItem(onClick = { onClick(alternative) }) {
                    Text(text = alternative)
                }
            }
            DropdownMenuItem(onClick = { onClick(originalValue) }) {
                Text(text = " [${stringResource(id = R.string.reset_action)}] ")
            }
        }
    }

}

@Composable
private fun RawTag(key: String, rawTag: RawTag) {
    val (
        id: String,
        name: String,
        value: TagData,
        description: String?,
    ) = rawTag

    Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // name and id
        Row(
            modifier = Modifier
                .align(Alignment.Start)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Text(
                text = name,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
                textAlign = TextAlign.Left,
                modifier = Modifier.weight(8f),
            )
            Text(
                text = id,
                style = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                ),
                textAlign = TextAlign.Right,
                modifier = Modifier.weight(2f),
            )
        }
        // description
        if (description != null) {
            Text(
                text = description,
                style = TextStyle(
                    fontWeight = FontWeight.Light,
                    fontSize = 9.sp,
                ),
                modifier = Modifier.align(Alignment.Start),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        // content
        SelectionContainer {
            Text(
                text = value.text(),
                style = TextStyle(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.92f),
                    fontSize = 14.sp,
                ),
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}

@Composable
fun Title(
    title: String,
    color: Color = MaterialTheme.colors.primary,
    horizontalPadding: Dp = 8.dp,
) {
    Text(
        title,
        style = TextStyle(fontWeight = FontWeight.Bold, color = color),
        modifier = Modifier.padding(horizontal = horizontalPadding)
    )
}

@Composable
private fun Item(@StringRes tagStringRes: Int, value: String) = Item(stringResource(tagStringRes), value)

@Composable
internal fun Item(tag: String, value: String) = VerticalTextItem(title = tag, value = value)

