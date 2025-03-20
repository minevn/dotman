package net.minevn.dotman.test.providers

import net.minevn.dotman.DotMan
import net.minevn.dotman.card.Card
import net.minevn.dotman.card.CardPrice
import net.minevn.dotman.card.CardType
import net.minevn.dotman.config.MainConfig
import net.minevn.dotman.providers.types.Card2KCP
import net.minevn.dotman.test.utils.setInstance
import net.minevn.dotman.test.utils.setInternal
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class Card2KCPTest {
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    lateinit var mockCard2K: Card2KCP

    @Mock
    lateinit var mockedMain: DotMan

    @Mock
    lateinit var mockedConfig: MainConfig

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        initMockedCard2K()
    }

    @Test
    fun testStatus() {
        println(mockCard2K.getStatusUrl())
        assertNotNull(mockCard2K.getStatusUrl())
    }

    @Test
    fun testCard() {
        setInstance(MainConfig::class.java, mockedConfig)
        Mockito.doReturn("testserver").`when`(mockedConfig).server
        mockCard2K.setInternal("main", mockedMain)

        val card = Card("zzz", "xxx", CardPrice.CP_10K, CardType.VIETTEL)
        val result = mockCard2K.doRequest("testplayer", card)
        assertNotNull(result)
    }

    private fun initMockedCard2K() {
        mockCard2K.setInternal("partnerId", System.getenv("CARD2K_PARTNER_ID") ?: "test_partner_id")
        mockCard2K.setInternal("partnerKey", System.getenv("CARD2K_PARTNER_KEY") ?: "test_partner_key")
    }
}