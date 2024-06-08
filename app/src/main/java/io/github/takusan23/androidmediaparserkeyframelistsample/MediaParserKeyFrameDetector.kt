package io.github.takusan23.androidmediaparserkeyframelistsample

import android.media.MediaCodec
import android.media.MediaParser
import android.media.MediaParser.SeekableInputReader
import androidx.core.util.component2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/** MediaParser を使ってコンテナフォーマットを解析する */
object MediaParserKeyFrameDetector {

    /** 解析して、キーフレームの位置を検出する */
    suspend fun detect(onCreateInputStream: () -> InputStream): List<Long> = withContext(Dispatchers.IO) {

        // MediaParser を作る
        var seekMap: MediaParser.SeekMap? = null
        // onSeekFound が来たら解析終わってほしいので
        var isFoundSeekMap = false
        // パース結果コールバック
        val output = object : MediaParser.OutputConsumer {

            // 中身には興味がないので適当に入れ物だけ用意
            private val tempByteArray = ByteArray(4096)

            override fun onSeekMapFound(p0: MediaParser.SeekMap) {
                // 解析してシークできる位置が分かった
                seekMap = p0
                isFoundSeekMap = true
            }

            override fun onTrackCountFound(p0: Int) {
                // do nothing
            }

            override fun onTrackDataFound(p0: Int, p1: MediaParser.TrackData) {
                // do nothing
            }

            override fun onSampleDataFound(p0: Int, p1: MediaParser.InputReader) {
                // SeekMap が欲しいだけなのだが、InputReader#read しないと MediaParser#advance で止まってしまうので
                // サンプル通りに InputReader#read している。
                // SeekMap が欲しいだけで中身には興味がないので tempByteArray に上書きしている
                val readSize = p1.length.toInt()
                p1.read(tempByteArray, 0, minOf(tempByteArray.size, readSize))
            }

            override fun onSampleCompleted(p0: Int, p1: Long, p2: Int, p3: Int, p4: Int, p5: MediaCodec.CryptoInfo?) {
                // do nothing
            }
        }
        // InputStream を MediaParser.InputReader で使う
        val input = InputStreamSeekableInputReader(onCreateInputStream)

        // MP4 と WebM のコンテナを解析する
        val mediaParser = MediaParser.create(output, MediaParser.PARSER_NAME_MP4, MediaParser.PARSER_NAME_MATROSKA)
        while (!isFoundSeekMap && mediaParser.advance(input)) {
            // SeekMap が取れるまで while 回す
        }

        mediaParser.release()
        input.close()

        // 流石にないはず
        seekMap ?: return@withContext emptyList()

        // SeekMap 取れたら、1 ミリ秒ごとにシークできる位置はどこかを問いただす
        return@withContext (0 until seekMap!!.durationMicros step 1_000) // マイクロ秒注意
            .map { timeUs -> seekMap!!.getSeekPoints(timeUs).component2() } // 次のシーク位置が欲しい
            .map { it.timeMicros }
            .distinct() // 同じ値（キーフレーム間隔が 1 秒以上なら同じ値が入ってくることある）
    }

    /** MediaParser.Input の InputStream 実装例 */
    class InputStreamSeekableInputReader(private val onCreateInputStream: () -> InputStream) : SeekableInputReader {

        /** InputStream。[seekToPosition]が呼び出された際には作り直す */
        private var currentInputStream = onCreateInputStream()

        /** read する前に available を呼ぶことでファイルの合計サイズを出す */
        private val fileSize = currentInputStream.available().toLong()

        override fun read(p0: ByteArray, p1: Int, p2: Int): Int = currentInputStream.read(p0, p1, p2)

        override fun getPosition(): Long = fileSize - currentInputStream.available()

        override fun getLength(): Long = fileSize

        override fun seekToPosition(p0: Long) {
            // ContentResolver#openInputStream だと mark/reset が使えない
            // InputStream を作り直す
            currentInputStream.close()
            currentInputStream = onCreateInputStream()
            currentInputStream.skip(p0)
        }

        /** InputStream を閉じる */
        fun close() {
            currentInputStream.close()
        }
    }
}