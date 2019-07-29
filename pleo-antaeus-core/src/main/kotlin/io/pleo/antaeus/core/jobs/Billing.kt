package io.pleo.antaeus.core.jobs

import io.pleo.antaeus.core.services.BillingService
import org.quartz.*


class Billing : Job {

    override fun execute(context: JobExecutionContext?) {
        try {
            val schedulerContext = context?.scheduler?.context
            val billingService = schedulerContext?.get("BillingService") as BillingService
            billingService.pay()
        } catch (e: Exception) {
            val exc = JobExecutionException(e)
            // This will force quartz to shutdown this job so that it does not run again
            // until the exception is addressed
            exc.setUnscheduleAllTriggers(true)
            throw exc
        }

    }

    fun getJobDetail(): JobDetail {
        return JobBuilder.newJob(this::class.java)
                .withIdentity("BillingService")
                .build()
    }

}