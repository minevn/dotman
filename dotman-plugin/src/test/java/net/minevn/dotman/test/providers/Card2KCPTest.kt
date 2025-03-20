package net.minevn.dotman.test.providers

import net.minevn.dotman.DotMan
import net.minevn.dotman.card.Card
import net.minevn.dotman.card.CardPrice
import net.minevn.dotman.card.CardType
import net.minevn.dotman.config.MainConfig
import net.minevn.dotman.providers.types.Card2KCP
import net.minevn.dotman.providers.types.TheSieuTocCP
import net.minevn.dotman.test.utils.setInstance
import net.minevn.dotman.test.utils.setInternal
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
        initMockedTST()
    }

    @Test
    fun testStatus() {
        println(mockCard2K.getStatusUrl())
        // todo: print status
    }

    @Test
    fun testCard() {
        setInstance(MainConfig::class.java, mockedConfig)
        Mockito.doReturn("testserver").`when`(mockedConfig).server
        mockCard2K.setInternal("main", mockedMain)

        val card = Card("zzz", "xxx", CardPrice.CP_10K, CardType.VIETTEL)
        println(mockCard2K.doRequest("testplayer", card))

        // fail sample: {"msg":"Th\u1ebb \u0111\u00e3 t\u1ed3n t\u1ea1i trong h\u1ec7 th\u1ed1ng!","status":2,"title":"Th\u1ea5t B\u1ea1i"}
        // waiting sample: {"status":"00","transaction_id":"testplayer from testserver","amount":1,"title":"Tha\u0300nh c\u00f4ng","msg":"Th\u1ebb \u0111\u00e3 \u0111\u01b0\u1ee3c g\u1eedi l\u00ean h\u1ec7 th\u1ed1ng vui l\u00f2ng ch\u1edd x\u1eed l\u00fd"}
    }

    private fun initMockedTST() {
        mockCard2K.setInternal("partnerId", System.getenv("CARD2K_PARTNER_ID"))
        mockCard2K.setInternal("partnerKey", System.getenv("CARD2K_PARTNER_KEY"))
    }
}