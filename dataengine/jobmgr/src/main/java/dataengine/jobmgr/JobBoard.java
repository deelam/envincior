package dataengine.jobmgr;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import dataengine.apis.JobBoardInput_I;
import dataengine.apis.JobBoardOutput_I;
import dataengine.apis.JobDTO;
import dataengine.apis.JobListDTO;
import dataengine.jobmgr.JobBoard.JobItem.JobState;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * JobProducer calls:
 * -addJob(id,completionAddr,job)
 * -getProgress(id)
 * -removeJob(id)
 * 
 * JobConsumer calls:
 * -register(workerAddr)
 * -setProgress(job)
 * -done(job)
 * -fail(job)
 * 
 * JobConsumers register and JobProducers addJobs.  For each of these events, a list of available jobs are sent to the next idle Worker to choose a job.
 * (If no job is chosen and the availableJobList has changed, the latest list is sent to the Worker.)
 * When a Worker is done or has failed the job, it is sent the latest availableJobList.
 * 
 * Workers can submit setProgress to update the job JsonObject.  
 * Calling done(job) will send a notification to the completionAddress set in the job JsonObject.
 * Upon completion, a job is simply marked as DONE.  To delete the job entry, call removeJob(id). 
 * 
 * If job fails, a failCount is incremented and the JOB_FAILEDCOUNT_ATTRIBUTE property set on the job JsonObject.
 * 
 * --
 * 
 * JobBoard negotiates with one worker at a time so multiple workers cannot choose the same job.
 * pickyWorkers holds workers who did not pick any existing job, and so will not be notified until a new job is added.
 * When a new job is added, all workers in pickyIdleWorkers will be added to idleWorkers.
 * 
 */
@Slf4j
@ToString
@RequiredArgsConstructor
public class JobBoard implements JobBoardInput_I, JobBoardOutput_I {

  private final Consumer<JobDTO> newJobNotifier;

  // jobId -> JobItem
  private Map<String, JobItem> jobItems = new LinkedHashMap<>();

  private int removeCounter=0;

  private long timeOfLastJobAdded = System.currentTimeMillis();

  @Setter
  @Getter
  private String jobBoardRpcAddr;
  
  @Override
  public CompletableFuture<Boolean> addJob(JobDTO job, int maxRetries) {
    log.info("JOBBRD: add job={} maxRetries={}", job.getId(), maxRetries);
    job.setJobBoardRpcAddr(jobBoardRpcAddr);
    
    JobItem ji = new JobItem(job, maxRetries);
    ji.state = JobState.AVAILABLE;
    JobItem existingJI = jobItems.get(ji.getId());
    boolean addJob = true;
    if (existingJI != null) {
      switch (existingJI.state) {
        case AVAILABLE:
        case DONE:
        case FAILED:
          log.info("Job with id={} already exists but has state={}.  Adding job again.", ji.getId(),
              existingJI.state);
          addJob = true;
          break;
        case STARTED:
        case PROGRESSING:
          log.warn("Job with id={} already exists and has started!", ji.getId());
          addJob = false;
          break;
      }
    }
    if (addJob) {
      log.info("Adding job: {}", ji);
      jobItems.put(ji.getId(), ji);

      timeOfLastJobAdded = System.currentTimeMillis(); // in case in the middle of negotiating

      newJobNotifier.accept(ji.jobJO);
    }
    return CompletableFuture.completedFuture(addJob);
  }

  @Override
  public CompletableFuture<Boolean> removeJob(String jobId) {
    log.info("JOBBRD: Remove job={}", jobId);
    JobItem ji = jobItems.get(jobId);
    if (ji == null) {
      log.warn("Cannot find job with id={}", jobId);
      return CompletableFuture.completedFuture(false);
    } else {
      switch (ji.state) {
        case AVAILABLE:
        case DONE:
        case FAILED:
          jobItems.remove(jobId);
          ++removeCounter;
          return CompletableFuture.completedFuture(true);
        case STARTED:
        case PROGRESSING:
        default:
          log.warn("Cannot remove job id={} with state={}", jobId, ji.state);
          return CompletableFuture.completedFuture(false);
      }
    }
  }

  @Override
  public CompletableFuture<JobListDTO> findJobs(Map<String, Object> searchParams) {
    JobListDTO availableJobs = getAvailableJobsFor(searchParams);
    return CompletableFuture.completedFuture(availableJobs);
  }

  @Override
  public CompletableFuture<Boolean> pickedJob(String workerAddr, String jobId, long timeOfJobListQuery) {
    if (jobId == null) {
      log.info("JOBBRD: worker={} did not choose a job", workerAddr);

      boolean jobRecentlyAdded=timeOfLastJobAdded>timeOfJobListQuery;
      // jobItems may have changed by the time this reply is received
      if (jobRecentlyAdded) {
        log.info("jobList has since changed since sending jobList to {}", workerAddr);
        return CompletableFuture.completedFuture(false);
      } else {
        return CompletableFuture.completedFuture(true);
      }
    } else {
      log.info("JOBBRD: pickedJob: worker={} job={}", workerAddr, jobId);
      try{
        workerStartedJob(jobId, workerAddr);
        return CompletableFuture.completedFuture(true);
      }catch(Exception e){
        // job may have been removed while consumer was picking from the jobList
        log.warn("When assigning job={} to worker={}", jobId, workerAddr, e);
        return CompletableFuture.completedFuture(false);
      }
    }
  }

  @Override
  public CompletableFuture<Void> jobDone(String workerAddr, String jobId) {
    log.info("JOBBRD: Received DONE message: {} from worker={}", jobId, workerAddr);
    JobItem ji = workerEndedJob(jobId, workerAddr, JobState.DONE);
    log.debug("Done job: {}", ji.jobJO);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> jobPartlyDone(String workerAddr, String jobId) {
    // worker completed its part of the job
    log.info("JOBBRD: Received PARTLY_DONE message: {} from worker={}", jobId, workerAddr);
    JobItem ji = workerEndedJob(jobId, workerAddr, JobState.AVAILABLE);
    log.debug("Partly done: {}", ji.jobJO);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> jobFailed(String workerAddr, String jobId) {
    log.info("JOBBRD: Received FAIL message: {} from worker={}", jobId, workerAddr);
    JobItem job = getJobItem(jobId);
    int failCount = job.incrementFailCount();

    JobState endState = (failCount >= job.retryLimit) ? JobState.FAILED : JobState.AVAILABLE;
    JobItem ji = workerEndedJob(jobId, workerAddr, endState);
    log.debug("Failed job: {}", ji.jobJO);
    return CompletableFuture.completedFuture(null);
  }
  
  @Deprecated
  public void start() throws Exception {
    if(statusPeriod>0){
      AtomicInteger sameLogMsgCount=new AtomicInteger(0);
      //vertx.setPeriodic(statusPeriod, 
      Runnable runnable= ()->{
        long availJobCount = jobItems.entrySet().stream().filter(e -> (e.getValue().state == JobState.AVAILABLE)).count();
        long startedJobCount = jobItems.entrySet().stream().filter(e -> (e.getValue().state == JobState.STARTED)).count();
        long progessingJobCount = jobItems.entrySet().stream().filter(e -> (e.getValue().state == JobState.PROGRESSING)).count();
  //      if(availJobCount+startedJobCount+progessingJobCount==0)
  //        return;
        long doneJobCount = jobItems.entrySet().stream().filter(e -> (e.getValue().state == JobState.DONE)).count();
        long failedJobCount = jobItems.entrySet().stream().filter(e -> (e.getValue().state == JobState.FAILED)).count();
        String logMsg = availJobCount+" avail -> "+
            startedJobCount+" started .. "+
            progessingJobCount+" processing -> "+
            doneJobCount+" doneJobs, "+
            failedJobCount+" failed, "+
            removeCounter+" removed :: ";
        if(!logMsg.equals(prevLogMsg)){
          List<String> availJobsStr=jobItems.entrySet().stream().filter(e -> (e.getValue().state == JobState.AVAILABLE)).map(jiE->{
            JobItem ji = jiE.getValue();
            return ji.getId()+" type="+ji.jobJO.getType();
          }).collect(toList());
          log.info(logMsg+"\n"+availJobsStr);
          prevLogMsg=logMsg;
          sameLogMsgCount.set(0);
        } else {
          if(sameLogMsgCount.incrementAndGet()>sameLogThreshold)
            prevLogMsg=null;
        }
      }; //);
    }

    log.info("Ready: this={}", this);
  }
  
  private int statusPeriod, sameLogThreshold;
  public void periodicLogs(int statusPeriod, int sameLogThreshold) {
    this.statusPeriod=statusPeriod;
    this.sameLogThreshold=sameLogThreshold;
  }
  
  private String prevLogMsg;

  //negotiate with one worker at a time so workers don't choose the same job
  private boolean isNegotiating = false;

  private String toString(JobListDTO jobList) {
    StringBuilder sb = new StringBuilder();
    jobList.getJobs().forEach(j -> sb.append("\n  ").append(j.getId()));
    return sb.toString();
  }

  private JobListDTO getAvailableJobsFor(Map<String, Object> searchParams) {
    final Object jobType=searchParams.get(JobBoardOutput_I.JOBTYPE_PARAM);
    List<JobDTO> jobListL = jobItems.entrySet().stream().filter(e->{
      if (e.getValue().state != JobState.AVAILABLE)
        return false;
      return (jobType==null) || jobType.equals(e.getValue().jobJO.getType());
    }).map( e ->{
      JobItem ji = e.getValue();
      JobDTO dto = ji.jobJO;
      return dto;
    }).collect(Collectors.toList());

    if(jobListL.size()==0)
      log.debug("No '{}' jobs for: {}", jobType, searchParams);
    
    JobListDTO jobList = new JobListDTO(System.currentTimeMillis(), jobListL);
    log.info("availableJobs={}",toString(jobList));
    return jobList;
  }

  private JobItem workerStartedJob(String jobId, String workerAddr) {
    log.info("JOBBRD: worker={} started jobId={}", workerAddr, jobId);
    JobItem job = getJobItem(jobId);
    job.jobJO.setWorkerId(workerAddr);
    job.state = JobState.STARTED;
    return job;
  }

  private JobItem workerEndedJob(String jobId, String workerAddr, JobState newState) {
    log.info("JOBBRD: worker={} ended jobId={} with state={}", workerAddr, jobId, newState);
    JobItem job = getJobItem(jobId);
    job.jobJO.setWorkerId(workerAddr);
    log.debug("Setting job {} state from {} to {}", job.getId(), job.state, newState);
    job.state = newState;
    
    switch(newState) {
      case DONE:
      case FAILED:
        log.info("Removing {} job: {}", job.state, job.getId());
        jobItems.remove(jobId);
        break;
      default:
        break;
    }
    return job;
  }

  private JobItem getJobItem(String jobId) {
    JobItem job = jobItems.get(jobId);
    checkNotNull(job, "Cannot find job with id=" + jobId);
    return job;
  }

  ////
  
  @RequiredArgsConstructor
  @ToString
  private static class Worker {
    final String id;
    final String type;
  }

  @lombok.ToString
  protected static class JobItem {

    enum JobState {
      AVAILABLE, STARTED, PROGRESSING, DONE, FAILED
    };

    JobItem.JobState state;
    @Deprecated
    String completionAddr;
    @Deprecated
    String failureAddr;
    final int retryLimit; // 0 means don't retry
    int jobFailedCount=0;
    final JobDTO jobJO;

    JobItem(JobDTO jobDTO, int maxRetries) {
      jobJO = jobDTO;
      checkNotNull(jobJO.getId());

      retryLimit = maxRetries;
    }

    String getId() {
      return jobJO.getId();
    }

    int incrementFailCount() {
      jobFailedCount+=1;
      return jobFailedCount;
    }
    
    @Deprecated
    public void mergeIn(JobDTO job) {
//      if(job.params!=null)
//        jobJO.getParams().mergeIn(job.params);
    }

  }

}
