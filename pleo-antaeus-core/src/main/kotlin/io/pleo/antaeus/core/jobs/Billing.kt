package io.pleo.antaeus.core.jobs

import io.pleo.antaeus.core.services.BillingService
import mu.KotlinLogging
import org.quartz.*


class Billing : Job {
    private val logger = KotlinLogging.logger {}

    override fun execute(context: JobExecutionContext?) {
        try {
            val schedulerContext = context?.scheduler?.context
            val billingService = schedulerContext?.get("BillingService") as BillingService
            billingService.pay()
        } catch (e: JobExecutionException) {
            // This will force quartz to shutdown this job so that it does not run again
            // until the exception is addressed
            e.setUnscheduleAllTriggers(true)

            logger.error(e) { "Job Execution Error" }
        } catch (e: Exception) {
            logger.error(e) { "Job Execution Error" }
        }

    }

    fun getJobDetail(): JobDetail {
        return JobBuilder.newJob(this::class.java)
                .withIdentity("BillingService")
                .build()
    }

}