package dataengine.tasker;

import static java.util.stream.Collectors.toList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import dataengine.apis.DepJobService_I;
import dataengine.apis.RpcClientProvider;
import dataengine.apis.SessionsDB_I;
import dataengine.apis.Tasker_I;
import dataengine.apis.CommunicationConsts;
import dataengine.tasker.jobcreators.AddSourceDataset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.deelam.activemq.rpc.ActiveMqRpcServer;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject) )
final class TaskerModule extends AbstractModule {
  final Properties properties;
  
  @Override
  protected void configure() {
    requireBinding(Key.get(new TypeLiteral<RpcClientProvider<SessionsDB_I>>() {}));
    requireBinding(Key.get(new TypeLiteral<RpcClientProvider<DepJobService_I>>() {}));
    
    bind(Properties.class).toInstance(properties);

    // See http://stackoverflow.com/questions/14781471/guice-differences-between-singleton-class-and-singleton
    bind(JobListener_I.class).to(TaskerJobListener.class);
    bind(TaskerJobListener.class).in(Singleton.class);
    
    bind(Tasker_I.class).to(TaskerService.class);
    bind(TaskerService.class).in(Singleton.class);
  }
  
  @Provides
  List<JobsCreator> getJobCreators(Injector injector) {
    // read jobCreators from file
    String[] jobCreatorClasses={
      AddSourceDataset.class.getCanonicalName()
    };
    String jobCreatorsStr = properties.getProperty("jobCreators",String.join(" ",jobCreatorClasses));
    List<String> classes = Arrays.asList(jobCreatorsStr.split(" "));
    List<JobsCreator> jobCreators = classes.stream().map(jcClassName -> {
      try {
        @SuppressWarnings("unchecked")
        Class<? extends JobsCreator> addSrcDataClazz = (Class<? extends JobsCreator>) Class.forName(jcClassName);
        JobsCreator jc = injector.getInstance(addSrcDataClazz);
        return jc;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }).collect(toList());
    return jobCreators;
  }

  static void deployTasker(Injector injector) {
    TaskerService taskerSvc = injector.getInstance(TaskerService.class);
    log.info("AMQ: SERV: Deploying RPC service for TaskerService: {} ", taskerSvc); 
    injector.getInstance(ActiveMqRpcServer.class).start(CommunicationConsts.taskerBroadcastAddr, taskerSvc, true);
  }
  
  static void deployJobListener(Injector injector) {
    TaskerJobListener jobListener = injector.getInstance(TaskerJobListener.class);
    Properties props = injector.getInstance(Properties.class);
    int progressPollIntervalSeconds=Integer.valueOf(props.getProperty("jobListener.progressPollIntervalSeconds", "2"));
    jobListener.setProgressPollIntervalSeconds(progressPollIntervalSeconds);
    Properties compProps = new Properties(props);
    compProps.setProperty("_componentId", "getFromZookeeper-jobListener"); //FIXME
    jobListener.start(compProps);
  }
}
