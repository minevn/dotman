package net.minevn.dotman.test.providers

import net.minevn.dotman.DotMan
import net.minevn.dotman.card.Card
import net.minevn.dotman.card.CardPrice
import net.minevn.dotman.card.CardType
import net.minevn.dotman.config.MainConfig
import net.minevn.dotman.providers.types.GameBankCP
import net.minevn.dotman.test.utils.setInstance
import net.minevn.dotman.test.utils.setInternal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.MockitoAnnotations


class GameBankCPTest {
	@Mock(answer = Answers.CALLS_REAL_METHODS)
	lateinit var mockedGameBankCP: GameBankCP

	@Mock
	lateinit var mockedMain: DotMan

	@Mock
	lateinit var mockedConfig: MainConfig

	@BeforeEach
	fun setUp() {
		MockitoAnnotations.openMocks(this)
		initMockedGameBank()
	}

	@Test
	fun testStatus() {
		println(mockedGameBankCP.getStatusUrl())
		println(mockedGameBankCP.getStatusString())
	}

	@Test
	fun testStatusMapping() {
		setInstance(MainConfig::class.java, mockedConfig)
		val mockedCardTypes = CardType.entries.associateWith { true }.toMutableMap()
		mockedCardTypes[CardType.GARENA] = false
		doReturn(mockedCardTypes).`when`(mockedConfig).cardTypes

		val json = "[{\"Viettel\":\"1\",\"Mobiphone\":\"1\",\"Vinaphone\":\"1\",\"Gate\":\"1\",\"VN-Mobile\":\"1\"," +
				"\"Zing\":\"1\",\"Garena\":\"0\"}]"
		val statusMap = mockedGameBankCP.getStatusSet(json)
		mockedGameBankCP.setInternal("statusCards", statusMap)
		assertEquals(true, mockedGameBankCP.getStatus(CardType.VIETTEL)) // enum valueOf
		assertEquals(true, mockedGameBankCP.getStatus(CardType.MOBIFONE)) // enum alternative
		assertEquals(true, mockedGameBankCP.getStatus(CardType.VIETNAMOBILE)) // enum alternative
		assertEquals(false, mockedGameBankCP.getStatus(CardType.GARENA)) // enum valueOf, false value
		assertEquals(false, mockedGameBankCP.getStatus(CardType.MEGACARD)) // not included in json
	}

	@Test
	fun testCard() {
		setInstance(MainConfig::class.java, mockedConfig)
		doReturn("testserver").`when`(mockedConfig).server
		mockedGameBankCP.setInternal("main", mockedMain)

		val card = Card("20000255314776", "221574224126557", CardPrice.CP_10K, CardType.VIETTEL)
		println(mockedGameBankCP.doRequest("testplayer", card))
	}

	private fun initMockedGameBank() {
		// sample env vars
		// GAMEBANK_API_PASSWORD=123;GAMEBANK_API_USER=123;GAMEBANK_MERCHANT_ID=123

		mockedGameBankCP.setInternal("merchantId", System.getenv("GAMEBANK_MERCHANT_ID").toInt())
		mockedGameBankCP.setInternal("apiUser", System.getenv("GAMEBANK_API_USER"))
		mockedGameBankCP.setInternal("apiPassword", System.getenv("GAMEBANK_API_PASSWORD"))
	}
}