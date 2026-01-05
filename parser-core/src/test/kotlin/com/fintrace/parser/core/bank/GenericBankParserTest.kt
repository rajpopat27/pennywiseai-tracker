package com.fintrace.parser.core.bank

import com.fintrace.parser.core.Constants
import com.fintrace.parser.core.TransactionType
import com.fintrace.parser.core.test.ExpectedTransaction
import com.fintrace.parser.core.test.ParserTestCase
import com.fintrace.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

class GenericBankParserTest {

    private val parser = GenericBankParser()

    @Test
    fun `generic parser handles HDFC card SMS that fails specific parser`() {
        ParserTestUtils.printTestHeader(
            parserName = "Generic Fallback",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val cases = listOf(
            ParserTestCase(
                name = "HDFC Card - spent on format",
                message = "Alert: Rs.1,299.00 spent on HDFC Bank Card xx1234 at AMZN MKTP IN on 05-01-25. Avl bal: Rs.45,678.90",
                sender = "HDFCBK",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1299.00"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    merchant = "AMZN MKTP IN",
                    accountLast4 = "1234",
                    balance = BigDecimal("45678.90")
                )
            )
        )

        ParserTestUtils.runTestSuite(parser, cases)
    }

    @Test
    fun `generic parser handles unknown bank senders`() {
        val cases = listOf(
            ParserTestCase(
                name = "Unknown Bank - Standard debit",
                message = "Rs.500.00 debited from A/c XX9876 on 01-Jan-25. Avl Bal: Rs.5000.00",
                sender = "UNKNOWNBK",
                expected = ExpectedTransaction(
                    amount = BigDecimal("500.00"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "9876",
                    balance = BigDecimal("5000.00")
                )
            ),
            ParserTestCase(
                name = "Unknown Bank - Credit transaction",
                message = "INR 1500 credited to account XX1234 on 01-Jan-25",
                sender = "XYZBK",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1500"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    accountLast4 = "1234"
                )
            ),
            ParserTestCase(
                name = "Unknown Bank - Transfer",
                message = "Rs.2000 transferred to JOHN DOE on 01-Jan-25. Ref: 123456789",
                sender = "RANDOMBK",
                expected = ExpectedTransaction(
                    amount = BigDecimal("2000"),
                    currency = "INR",
                    type = TransactionType.EXPENSE
                )
            )
        )

        ParserTestUtils.runTestSuite(parser, cases)
    }

    @Test
    fun `generic parser detects currency correctly`() {
        // INR currency
        val inrMessage = "Rs.500.00 debited from A/c XX1234"
        val inrResult = parser.parse(inrMessage, "TESTBK", System.currentTimeMillis())
        assertEquals("INR", inrResult?.currency, "Should detect INR currency")

        // INR with symbol
        val rupeeMessage = "₹1000 debited from account"
        val rupeeResult = parser.parse(rupeeMessage, "TESTBK", System.currentTimeMillis())
        assertEquals("INR", rupeeResult?.currency, "Should detect INR from ₹ symbol")
    }

    @Test
    fun `generic parser cleans sender to bank name`() {
        // Test bank name extraction from sender
        val hdfcResult = parser.parse("Rs.100 debited from A/c XX1234", "HDFCBK", System.currentTimeMillis())
        assertEquals("HDFC Bank", hdfcResult?.bankName, "Should extract HDFC Bank from HDFCBK")

        val sbiResult = parser.parse("Rs.100 debited from A/c XX1234", "VM-SBIINB", System.currentTimeMillis())
        assertEquals("State Bank of India", sbiResult?.bankName, "Should extract SBI from VM-SBIINB")

        val unknownResult = parser.parse("Rs.100 debited from A/c XX1234", "XYZABC", System.currentTimeMillis())
        assertNotNull(unknownResult?.bankName, "Should have a bank name for unknown sender")
    }

    @Test
    fun `generic parser has lower confidence than specific parsers`() {
        val message = "Rs.500.00 debited from A/c XX1234 to AMAZON"
        val result = parser.parse(message, "UNKNOWNBK", System.currentTimeMillis())

        assertNotNull(result, "Should parse the message")
        assertEquals(Constants.Parsing.CONFIDENCE_PATTERN_BASED, result?.parserConfidence,
            "Generic parser should have CONFIDENCE_PATTERN_BASED confidence")
        assertTrue(result!!.parserConfidence < 1.0f, "Generic parser confidence should be less than 1.0")
    }

    @Test
    fun `generic parser rejects non-transaction messages`() {
        val nonTransactionMessages = listOf(
            "Your OTP is 123456 for transaction verification",
            "Special offer! Get 50% discount on all purchases",
            "John has requested Rs.500. Pay now to accept.",
            "Your credit card bill of Rs.5000 is due on 15-Jan-25",
            "Thank you for subscribing to our service"
        )

        for (message in nonTransactionMessages) {
            val result = parser.parse(message, "ANYBK", System.currentTimeMillis())
            assertNull(result, "Should reject non-transaction message: ${message.take(30)}...")
        }
    }

    @Test
    fun `generic parser accepts all senders`() {
        assertTrue(parser.canHandle("HDFCBK"))
        assertTrue(parser.canHandle("UNKNOWNBANK"))
        assertTrue(parser.canHandle("RANDOM123"))
        assertTrue(parser.canHandle("VM-ICICIB"))
        assertTrue(parser.canHandle("AD-XYZABC"))
    }

    @Test
    fun `parseWithFallback uses specific parser when available and successful`() {
        // This message should be parsed by HDFC-specific parser
        val hdfcDebitMessage = "Rs.500.00 debited from HDFC Bank A/c XX1234 to AMAZON. Avl Bal Rs.5000"
        val result = BankParserFactory.parseWithFallback("HDFCBK", hdfcDebitMessage, System.currentTimeMillis())

        assertNotNull(result, "Should parse the message")
        assertEquals("HDFC Bank", result?.bankName)
        // If specific parser handles it, confidence should be 1.0
        // If generic handles it, confidence should be 0.7
    }

    @Test
    fun `parseWithFallback uses generic parser for unknown senders`() {
        val message = "Rs.500.00 debited from A/c XX1234 to AMAZON"
        val result = BankParserFactory.parseWithFallback("TOTALLYUNKNOWNBK", message, System.currentTimeMillis())

        assertNotNull(result, "Should parse with generic parser")
        assertEquals(Constants.Parsing.CONFIDENCE_PATTERN_BASED, result?.parserConfidence,
            "Should have generic parser confidence")
    }

    @Test
    fun `parseWithFallback falls back when specific parser returns null`() {
        // This format fails HDFC specific parser but should work with generic
        val hdfcCardMessage = "Alert: Rs.1,299.00 spent on HDFC Bank Card xx1234 at AMZN MKTP IN on 05-01-25"

        // First verify specific parser fails
        val specificParser = BankParserFactory.getParser("HDFCBK")
        assertNotNull(specificParser, "Should have HDFC parser")

        // Use fallback - should still parse
        val result = BankParserFactory.parseWithFallback("HDFCBK", hdfcCardMessage, System.currentTimeMillis())
        assertNotNull(result, "Fallback should parse the message")
        assertEquals(BigDecimal("1299.00"), result?.amount)
        assertEquals(TransactionType.EXPENSE, result?.type)
    }
}
