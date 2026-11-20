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



@DisplayName("cleanup unit tests")

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

    @DisplayName("chunk cleanup unit tests")

    inner class TestChunkClean : ChunkCleanTest()



    @Nested

    @DisplayName("dropped item cleanup unit tests")

    inner class TestDropClean : DropCleanTest()



    @Nested

    @DisplayName("living entity cleanup unit tests")

    inner class TestLivingClean : LivingCleanTest()



    @Nested

    @DisplayName("trash can unit tests")

    inner class TestTrashcan : TrashcanTest()

}