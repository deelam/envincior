package dataengine.workers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import dataengine.api.Job;
import dataengine.api.Operation;
import dataengine.api.OperationParam;
import dataengine.api.OperationParam.ValuetypeEnum;
import dataengine.api.State;
import dataengine.apis.OperationConsts;
import dataengine.apis.RpcClientProvider;
import dataengine.apis.SessionsDB_I;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class PostRequestWorker extends BaseWorker<Job> {

  @Inject
  public PostRequestWorker(RpcClientProvider<SessionsDB_I> sessDb) {
    super(OperationConsts.TYPE_POSTREQUEST, sessDb);
  }

  @Override
  protected Operation initOperation() {
    Map<String, String> info = new HashMap<>();
    info.put(OperationConsts.OPERATION_TYPE, OperationConsts.TYPE_POSTREQUEST);
    return new Operation().level(1).id(jobType())
        .description("connect request to dataset")
        .info(info)
        .addParamsItem(new OperationParam()
            .key(OperationConsts.PREV_JOBID).required(true)
            .valuetype(ValuetypeEnum.STRING));

    //    requiredParams = OperationUtils.getRequiredParams(getOperation());
  }

  @Override
  protected boolean doWork(Job job) throws Exception {
    CompletableFuture<Void> connectF = getPrevJobDatasetId(job)
        .thenCompose(datasetId -> sessDb.rpc().connectRequestToOutputDataset(job.getRequestId(), datasetId))
        .thenAccept(v -> sessDb.rpc().updateRequestState(job.getRequestId(), State.COMPLETED));
    connectF.get(); // call get so that exception can be thrown
    return true;
  }

}
