package maestro.orchestra.debug

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import maestro.Maestro
import maestro.TreeNode
import maestro.ViewHierarchy
import maestro.orchestra.MaestroCommand
import maestro.orchestra.ScrollCommand
import okio.Buffer
import okio.Sink
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class IndependentCaptureTest {
    @TempDir
    lateinit var directory: Path

    private fun device(): Maestro = mockk(relaxed = true) {
        coEvery { takeScreenshot(any<Sink>(), any()) } answers {
            val sink = firstArg<Sink>()
            val bytes = Buffer().write(byteArrayOf(1, 2, 3))
            sink.write(bytes, bytes.size)
            sink.flush()
        }
        coEvery { viewHierarchy(any()) } returns ViewHierarchy(TreeNode(attributes = mutableMapOf()))
    }

    @Test
    fun `successful boundary capture does not start recording`() {
        val maestro = device()
        val generator = ArtifactsGenerator(directory, maestro, captureStepScreenshots = true)
        val command = MaestroCommand(scrollCommand = ScrollCommand())
        generator.onFlowStart()
        generator.onCommandStart(command, 1)
        generator.onCommandFinished(command, CommandOutcome.Completed, 10, 20)
        generator.onFlowEnd()
        val artifact = generator.debugOutput.executedSteps.single().artifacts.single()
        assertThat(artifact.metadata["phase"]).isEqualTo("before_action")
        assertThat(artifact.metadata["capturedAtEpochMs"]!!.toLong()).isGreaterThan(0L)
        assertThat(directory.resolve("screenshots/final.png").toFile().exists()).isTrue()
        coVerify(exactly = 0) { maestro.startScreenRecording(any()) }
    }

    @Test
    fun `recording can be disabled with full artifacts enabled`() {
        val maestro = device()
        val generator = ArtifactsGenerator(directory, maestro,
            captureFullArtifacts = true, captureScreenRecording = false)
        generator.onFlowStart()
        generator.onFlowEnd()
        coVerify(exactly = 0) { maestro.startScreenRecording(any()) }
    }

    @Test
    fun `failed replacement preserves original phase`() {
        val maestro = device()
        val generator = ArtifactsGenerator(directory, maestro, captureStepScreenshots = true)
        val command = MaestroCommand(scrollCommand = ScrollCommand())
        generator.onFlowStart()
        generator.onCommandStart(command, 1)
        coEvery { maestro.takeScreenshot(any<Sink>(), any()) } throws RuntimeException("capture failed")
        generator.onCommandFinished(command, CommandOutcome.Failed(RuntimeException("step failed")), 10, 20)
        generator.onFlowEnd()
        val artifact = generator.debugOutput.executedSteps.single().artifacts.first { it.path.endsWith(".png") }
        assertThat(artifact.metadata["phase"]).isEqualTo("before_action")
    }

    @Test
    fun `warning replacement records actual outcome phase`() {
        val generator = ArtifactsGenerator(directory, device(), captureStepScreenshots = true)
        val command = MaestroCommand(scrollCommand = ScrollCommand())
        generator.onFlowStart()
        generator.onCommandStart(command, 1)
        generator.onCommandFinished(command, CommandOutcome.Warned, 10, 20)
        generator.onFlowEnd()
        val artifact = generator.debugOutput.executedSteps.single().artifacts.first { it.path.endsWith(".png") }
        assertThat(artifact.metadata["phase"]).isEqualTo("warning")
    }
}
