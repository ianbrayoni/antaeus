package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.jobs.Billing
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.quartz.*
import java.math.BigDecimal


class BillingServiceTest {
    private val pendingInvoices = listOf(
            Invoice(1, 5, Money(BigDecimal.valueOf(206.87), Currency.EUR), InvoiceStatus.PENDING),
            Invoice(2, 6, Money(BigDecimal.valueOf(215.83), Currency.GBP), InvoiceStatus.PENDING)
    )

    private val paidInvoices = listOf(
            Invoice(1, 5, Money(BigDecimal.valueOf(206.87), Currency.EUR), InvoiceStatus.PAID),
            Invoice(2, 6, Money(BigDecimal.valueOf(215.83), Currency.GBP), InvoiceStatus.PAID)
    )

    private val job = spyk<Billing> {
        every { execute(any()) } returns Unit
    }

    private fun getBillingCronTrigger(): Trigger {
        return TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(
                        SimpleScheduleBuilder
                                .simpleSchedule()
                                .withIntervalInSeconds(2)
                                .repeatForever())
                .build()
    }

    private val dal = mockk<AntaeusDal> {
        every { fetchInvoicesByStatus(InvoiceStatus.PENDING) } returns pendingInvoices
        every { fetchInvoicesByStatus(InvoiceStatus.PAID) } returns paidInvoices
        every { fetchInvoices() } returns paidInvoices
        every { updateInvoiceStatus(1, InvoiceStatus.PAID) } returns paidInvoices[0]
        every { updateInvoiceStatus(2, InvoiceStatus.PAID) } returns paidInvoices[1]
    }

    private val invoiceService = InvoiceService(dal = dal)

    private val paymentProvider = mockk<PaymentProvider> {
        every { charge(any()) } returns true
    }

    private val billingService = BillingService(
            job = job,
            trigger = getBillingCronTrigger(),
            invoiceService = invoiceService,
            paymentProvider = paymentProvider
    )


    @Test
    fun `accounts are charged`() {
        assertTrue(billingService.pay())
    }

    @Test
    fun `invoices updated to paid`() {
        invoiceService.fetchAll().forEach {
            assertEquals(InvoiceStatus.PAID, it.status)
        }
        assertEquals(invoiceService.fetchInvoicesByStatus(InvoiceStatus.PAID).size, 2)
    }

}

