package top.pmaru.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.RandomAccessFile
import java.net.Proxy
import java.net.URL
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class Main {
    companion object {
        private const val url =
            "https://par-fr-ping.vultr.com/vultr.com.100MB.bin"
        private val proxy: Proxy = Proxy.NO_PROXY
        private const val direction = "F:files/1.bin"
        private const val threadCount = 8

        @OptIn(ExperimentalTime::class)
        @JvmStatic
        fun main(args: Array<String>) = runBlocking {
            val time = measureTime {
                val web = URL(url).openConnection(proxy)
                val totalSize = web.contentLengthLong
                println("文件大小: ${totalSize / 1024}kb")
                val perThreadSize = totalSize / threadCount
                var schedule = 0.00

                val s = 1.00 / threadCount.toDouble()

                CoroutineScope(Dispatchers.IO).launch {
                    repeat(threadCount) {
                        launch(Dispatchers.IO) {
                            val access = RandomAccessFile(direction, "rw")
                            access.seek(perThreadSize * it)

                            var pointer = access.filePointer.toInt()
                            pointer = if (pointer > totalSize) totalSize.toInt() else pointer
                            val bytes = getRangeData(pointer, pointer + perThreadSize)
                            access.write(bytes)
                            access.close()
                            schedule += s
                            println("進度:${schedule * 100}%")
                        }
                    }
                }.join()
            }
            println("完成 用時:$time")
        }

        private fun getRangeData(start: Number, end: Number): ByteArray {
            return URL(url).openConnection(proxy).run {
                addRequestProperty("Range", "bytes=$start-$end")
                getInputStream().use { it.readAllBytes() }
            }
        }
    }
}
