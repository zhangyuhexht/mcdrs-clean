package top.mcdrsclean.test

import be.seeseemelk.mockbukkit.MockBukkit
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import top.mcdrsclean.MCDRSClean
import top.mcdrsclean.test.clean.ChunkCleanTest
import top.mcdrsclean.test.clean.DropCleanTest
import top.mcdrsclean.test.clean.LivingCleanTest
import top.mcdrsclean.unit
import trash.TrashcanTest

@DisplayName("清理单元测试")
class MCDRSCleanTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun init() {
            unit = true
            server = MockBukkit.mock()
            plugin = MockBukkit.load(MCDRSClean::class.java)
            world = server.addSimpleWorld("world")
            player = server.addPlayer("mock")
            consoleOut
        }

        @JvmStatic
        @BeforeAll
        fun finalize() {
            MockBukkit.unmock()
        }
    }

    @Nested
    @DisplayName("区块清理单元测试")
    inner class TestChunkClean : ChunkCleanTest()

    @Nested
    @DisplayName("掉落物清理单元测试")
    inner class TestDropClean : DropCleanTest()

    @Nested
    @DisplayName("生物清理单元测试")
    inner class TestLivingClean : LivingCleanTest()

    @Nested
    @DisplayName("垃圾桶单元测试")
    inner class TestTrashcan : TrashcanTest()
}