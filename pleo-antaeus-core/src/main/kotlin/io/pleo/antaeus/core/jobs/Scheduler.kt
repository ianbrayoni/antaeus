package io.pleo.antaeus.core.jobs

import org.quartz.SchedulerException
import org.quartz.Trigger
import org.quartz.impl.StdSchedulerFactory

open class Scheduler<Job : Billing>(
        private val job: Job,
        private val trigger: Trigger
) {

    fun runner() {
        try {
            val scheduler = StdSchedulerFactory().scheduler
            scheduler.context["BillingService"] = this
            scheduler.scheduleJob(job.getJobDetail(), trigger)
            scheduler.start()
        } catch (e: Exception) {
            throw SchedulerException(e)
        }
    }

}