## Billing Service
This service contains logic to pay invoices on the first of the month in the different markets that Pleo operates.

### Approach
To achieve recurrent billing of invoices, I considered libraries that had a [cron facility](https://en.wikipedia.org/wiki/Cron) and an asynchronous background task processor.
_Quartz_ did fit this criteria hence its usage herein. Its also open-source and actively maintained.

The solution has three components: _scheduler_, _job_ and a _trigger_:
i) Job
   Task to be executed, i.e, billing of invoices.
ii) Scheduler
   Coordinates the execution of the job.
iii) Trigger
   Sets up the interval and frequency with which the job will run. I chose a [CronTrigger](https://www.quartz-scheduler.net/documentation/quartz-2.x/tutorial/crontriggers.html) which is used to execute a job using a cron expression. The job in our case will run on 1st of every month at 6 am, this task can be tied to a notification service that sends out communication via text or email during non-intrusive hours.

![Alt text](./resources/images/billing.png?raw=true "Billing Service")

The diagram above demonstrates how they work together. The Billing Service is started by the main module of the app, _pleo-antaeus-app_ via a scheduler whose context is aware of the job. The trigger fires if it is time, 1st of every month, and the job processes the invoices that are due.

#### Why?
1. _Asynchronous processing_
The scheduler spins a worker thread pool, the default is 10 threads. Workers pick up tasks and execute them asynchronously. This task therefore does not need to affect interfere with other tasks running within the service.
2. _Concurrent Processing_
Billing invoices `1..10` ,for instance, can be done on different threads by each of the workers at the same time at best.
3. Quartz supports [load balancing and clustering](http://www.quartz-scheduler.org/documentation/2.3.1-SNAPSHOT/tutorials/tutorial-lesson-11.html#TutorialLesson11-Clustering) when scaling needs arise.


#### Limitations
- For storage of jobs and triggers, I do use the default RAMJobStore which has a more finite limit on how many Jobs and Triggers can be stored. This is because one is more likely to have less RAM than hard-drive space for a database for the JDBC-JobStore. There is also potential for high memory consumption issues.
- Making the cron expression and the jobs configurable via a UI could be vulnerable to sql injection attacks on the `Quartz` database.

#### Assumptions
* Pleo runs a monthly subscription model that runs from beginning of the month to the end, e.g 1st to 30th.
* All invoices marked PENDING are due for payment.
* Due invoices are payable on 1st of the month.
* All customers whose invoices are PENDING are also active customers.
* When Network Exceptions occur when charging an invoice, the status of the payment shall be considered _unknown_ and marked for reconciliation.
* `JobException`s, `SchedulerException`s and general `Exception`s preventing successful billing of the invoices will page an on-call Engineer for investigation and fixing.


### Learnings
- One is at risk of running into bugs when using Java 12 alongside the mockk library.
- This is a first and I am happy to have had a taste of Kotlin, Gradle, Kotlin Logging, mockk, JUnit 5 and Quartz Job Scheduler.

### References
[Quartz Enterprise Job Scheduler](http://www.quartz-scheduler.org/overview/)