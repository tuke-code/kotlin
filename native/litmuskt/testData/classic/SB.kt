// KIND: STANDALONE

import kotlin.test.*
//import komem.litmus.*
//import komem.litmus.testsuite.SB

@Test
fun sb() {
    println("SB is running")
    assertTrue(false)
//    val test = SB
//    val runner = WorkerRunner()
//    val params = LitmusRunParams(
//        batchSize = 1_000_000,
//        syncPeriod = 100,
//        affinityMap = null,
//        barrierProducer = ::CinteropBarrier,
//    )
//    val result = runner.runTest(params, test)
//    assertTrue(result.none{ it.type == LitmusOutcomeType.FORBIDDEN })
}
