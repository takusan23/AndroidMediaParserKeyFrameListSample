package io.github.takusan23.androidmediaparserkeyframelistsample

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.takusan23.androidmediaparserkeyframelistsample.ui.theme.AndroidMediaParserKeyFrameListSampleTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidMediaParserKeyFrameListSampleTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val seekPositionList = remember { mutableStateOf(listOf<Long>()) }

    fun start(uri: Uri?) {
        uri ?: return
        scope.launch {
            // キーフレームの位置を出す
            // 引数は InputStream を作る関数。必要になったら関数が呼ばれるので、InputStream を作って返してください。
            seekPositionList.value = MediaParserKeyFrameDetector.detect(onCreateInputStream = { context.contentResolver.openInputStream(uri)!! })
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> start(uri) }
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) }) }
    ) { innerPadding ->

        Column(Modifier.padding(innerPadding)) {

            Button(onClick = { filePicker.launch(PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.VideoOnly)) }) {
                Text(text = "動画の選択")
            }

            Text(text = "キーフレーム一覧")

            LazyColumn {
                items(seekPositionList.value) {
                    Text(text = "keyFrame = $it us")
                    HorizontalDivider()
                }
            }
        }
    }
}