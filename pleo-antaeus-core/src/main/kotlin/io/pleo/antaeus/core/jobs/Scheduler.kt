package io.pleo.antaeus.core.jobs

import mu.KotlinLogging
import org.quartz.SchedulerException
import org.quartz.Trigger
import org.quartz.impl.StdSchedulerFactory

open class Scheduler<Job : Billing>(
        private val job: Job,
        private val trigger: Trigger
) {

    private val logger = KotlinLogging.logger {}

    fun runner() {
        try {
            val scheduler = StdSchedulerFactory().scheduler
            scheduler.context["BillingService"] = this
            scheduler.scheduleJob(job.getJobDetail(), trigger)
            scheduler.start()
        } catch (e: SchedulerException) {
            logger.error(e) { "Job Scheduling Error: $e" }
        } catch (e: Exception) {
            logger.error(e) { "Job Scheduling Error: $e" }
        }
    }

}