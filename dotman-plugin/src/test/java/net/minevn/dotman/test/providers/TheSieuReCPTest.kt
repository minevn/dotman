package net.minevn.dotman.test.providers

import net.minevn.dotman.DotMan
import net.minevn.dotman.card.Card
import net.minevn.dotman.card.CardPrice
import net.minevn.dotman.card.CardType
import net.minevn.dotman.config.MainConfig
import net.minevn.dotman.providers.types.TheSieuReCP
import net.minevn.dotman.test.utils.setInstance
import net.minevn.dotman.test.utils.setInternal
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.MockitoAnnotations

class TheSieuReCPTest {
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    lateinit var mockedTheSieuReCP: TheSieuReCP

    @Mock
    lateinit var mockedMain: DotMan

    @Mock
    lateinit var mockedConfig: MainConfig

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        initMocked()
    }

    @Test
    fun testApiUrl() {
        println(mockedTheSieuReCP.getApiUrl())
        assertNotNull(mockedTheSieuReCP.getApiUrl())
    }

    @Test
    fun testCard() {
        setInstance(MainConfig::class.java, mockedConfig)
        doReturn("testserver").`when`(mockedConfig).server
        mockedTheSieuReCP.setInternal("main", mockedMain)

        val card = Card("69420", "67", CardPrice.CP_50K, CardType.VIETTEL)
        val result = mockedTheSieuReCP.doRequest("testplayer", card)
        assertNotNull(result)
        println(result)
    }

    private fun initMocked() {
        // sample env vars
        // THESIEURE_PARTNER_ID=xxx;THESIEURE_PARTNER_KEY=xxx
        mockedTheSieuReCP.setInternal("partnerId", System.getenv("THESIEURE_PARTNER_ID") ?: "test_partner_id")
        mockedTheSieuReCP.setInternal("partnerKey", System.getenv("THESIEURE_PARTNER_KEY") ?: "test_partner_key")
    }
}
