package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.jobs.Billing
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

import io.pleo.antaeus.core.jobs.Scheduler
import mu.KotlinLogging
import org.quartz.Trigger


class BillingService(
        job: Billing,
        trigger: Trigger,
        private val invoiceService: InvoiceService,
        private val paymentProvider: PaymentProvider
) : Scheduler<Billing>(job = job, trigger = trigger) {
    private val logger = KotlinLogging.logger {}

    fun pay(): Boolean {
        var isCharged = false

        val pendingInvoices = invoiceService.fetchInvoicesByStatus(InvoiceStatus.PENDING)

        pendingInvoices.forEach {
            try {
                isCharged = this.paymentProvider.charge(it)
            } catch (e: CurrencyMismatchException) {
                currencyMismatchExceptionHandler(it)
            } catch (e: CustomerNotFoundException) {
                customerNotFoundExceptionHandler(it)
            } catch (e: NetworkException) {
                networkExceptionHandler(it)
            } catch (e: InvoiceNotFoundException) {
                invoiceNotFoundExceptionHandler(it)
            } catch (e: Exception) {
                generalExceptionHandler(e, it)
            }


            if (isCharged) {
                successHandler(it)
            }
        }

        return isCharged

    }

    private fun successHandler(invoice: Invoice) {
        val updatedInvoice = invoiceService.updateInvoiceStatusById(invoice.id, InvoiceStatus.PAID)

        if (updatedInvoice.status == InvoiceStatus.PAID) {
            logger.info("Processed successfully: invoiceId ${updatedInvoice.id}")
        } else {
            logger.info("Failed to update: invoiceId ${updatedInvoice.id}")
        }
    }

    private fun currencyMismatchExceptionHandler(invoice: Invoice) {
        logger.error("Processing Error - CurrencyMismatchException occurred: invoiceId ${invoice.id}, currency ${invoice.amount.currency}")
    }

    private fun customerNotFoundExceptionHandler(invoice: Invoice) {
        logger.error("Processing Error - CustomerNotFoundException occurred: invoiceId ${invoice.id}, customerId ${invoice.customerId}")
    }

    private fun invoiceNotFoundExceptionHandler(invoice: Invoice) {
        logger.error("Processing Error - InvoiceNotFoundException occurred: invoiceId ${invoice.id}")
    }

    private fun networkExceptionHandler(invoice: Invoice) {
        logger.error("Processing Error - Network Exception occurred: invoiceId ${invoice.id}")
    }

    private fun generalExceptionHandler(e: Exception, invoice: Invoice) {
        logger.error(e) { "Processing Error - Exception occurred: invoiceId ${invoice.id}" }
    }
}