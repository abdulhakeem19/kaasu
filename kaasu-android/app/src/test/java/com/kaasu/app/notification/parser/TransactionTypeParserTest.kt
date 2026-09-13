package com.kaasu.app.notification.parser

import com.kaasu.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionTypeParserTest {

    private fun parse(text: String) = TransactionTypeParser.parse(text)

    @Test fun debited_keyword() = assertEquals(TransactionType.EXPENSE, parse("A/c XX1234 debited by Rs.500"))

    @Test fun paid_keyword() = assertEquals(TransactionType.EXPENSE, parse("₹350 paid to Zomato"))

    @Test fun payment_keyword() = assertEquals(TransactionType.EXPENSE, parse("Payment of ₹500 successful"))

    @Test fun sent_keyword() = assertEquals(TransactionType.EXPENSE, parse("₹250 Sent to merchant@upi"))

    @Test fun withdrawn_keyword() = assertEquals(TransactionType.EXPENSE, parse("Rs.1000 withdrawn from ATM"))

    @Test fun credited_keyword() = assertEquals(TransactionType.INCOME, parse("INR 2500 credited to your account"))

    @Test fun received_keyword() = assertEquals(TransactionType.INCOME, parse("₹2,500 received from Priya Kumar"))

    @Test fun added_keyword() = assertEquals(TransactionType.INCOME, parse("Rs.1000 added to Paytm Wallet"))

    @Test fun deposited_keyword() = assertEquals(TransactionType.INCOME, parse("Amount deposited: ₹5,000"))

    @Test fun refund_keyword() = assertEquals(TransactionType.REFUND, parse("Refund of ₹199 processed"))

    @Test fun reversed_keyword() = assertEquals(TransactionType.REFUND, parse("₹500 reversed to your account"))

    @Test fun cashback_keyword() = assertEquals(TransactionType.CASHBACK, parse("₹50 cashback credited"))

    @Test fun cashback_two_words() = assertEquals(TransactionType.CASHBACK, parse("You earned ₹25 cash back"))

    @Test fun transferred_keyword() = assertEquals(TransactionType.TRANSFER, parse("₹1,000 transferred to savings account"))

    @Test fun unknown_when_no_keyword() = assertEquals(TransactionType.UNKNOWN, parse("₹500 transaction alert"))

    @Test fun cashback_takes_priority_over_credited() =
        assertEquals(TransactionType.CASHBACK, parse("₹50 cashback credited to your account"))

    @Test fun refund_takes_priority_over_credited() =
        assertEquals(TransactionType.REFUND, parse("Refund of ₹199 credited to your account"))

    @Test fun spent_keyword() =
        assertEquals(TransactionType.EXPENSE, parse("INR 254.00 spent on your Credit Card at Swiggy"))

    // When a notification mentions both "debited" (your account) and "credited" (recipient),
    // the transaction type should be EXPENSE since it is a debit from your perspective.
    @Test fun debit_wins_over_credited_recipient() =
        assertEquals(TransactionType.EXPENSE, parse("A/c XX1234 debited by Rs. 50.00 on 09/05/26; Mohan K credited."))

    // "paid you" = income (someone paid you), NOT expense — "paid" alone would match DEBIT
    @Test fun paid_you_is_income() =
        assertEquals(TransactionType.INCOME, parse("MEENAKSHI . paid you ₹1.00 for lunch"))

    @Test fun sent_you_is_income() =
        assertEquals(TransactionType.INCOME, parse("Rahul sent you ₹500"))

    // "paid to" still expense — only "paid you" triggers income
    @Test fun paid_to_is_still_expense() =
        assertEquals(TransactionType.EXPENSE, parse("₹350 paid to Zomato"))
}
