
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.quartz.CronScheduleBuilder
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import java.math.BigDecimal
import java.util.*
import kotlin.random.Random

// This will create all schemas and setup initial data
internal fun setupInitialData(dal: AntaeusDal) {
    val customers = (1..100).mapNotNull {
        dal.createCustomer(
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    customers.forEach { customer ->
        (1..10).forEach {
            dal.createInvoice(
                amount = Money(
                    value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                    currency = customer.currency
                ),
                customer = customer,
                status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
            )
        }
    }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
                return Random.nextBoolean()
        }
    }
}

/*
* CronTrigger set to run on 1st of every month at 6 am
* See section on `Daylight Saving Time and Triggers` in
* https://www.quartz-scheduler.net/documentation/faq.html
*
* */
internal fun getBillingCronTrigger(): Trigger {
    return TriggerBuilder
            .newTrigger()
            .withSchedule(
                    CronScheduleBuilder
                            .cronSchedule("0 0 6 1 1/1 ? *")
                            .inTimeZone(TimeZone.getTimeZone("UTC"))
                            .withMisfireHandlingInstructionFireAndProceed())
            .build()
}