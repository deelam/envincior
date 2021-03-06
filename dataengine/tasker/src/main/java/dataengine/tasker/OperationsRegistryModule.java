package dataengine.tasker;

import javax.inject.Singleton;
import javax.jms.Connection;
import javax.jms.JMSException;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import dataengine.apis.CommunicationConsts;
import dataengine.apis.OperationsRegistry_I;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.deelam.activemq.rpc.ActiveMqRpcServer;
import net.deelam.activemq.rpc.AmqComponentSubscriber;

@RequiredArgsConstructor
@Slf4j
final class OperationsRegistryModule extends AbstractModule {

  @Override
  protected void configure() {
    requireBinding(Connection.class);

    // See http://stackoverflow.com/questions/14781471/guice-differences-between-singleton-class-and-singleton

    // OperationsRegistryRpcService wraps OperationsRegistry and is used as a proxy to OperationsSubscribers (ie, Workers)
    bind(OperationsRegistry_I.class).to(OperationsRegistry.class);
    bind(OperationsRegistry.class).in(Singleton.class);
  }

  // returning OperationsRegistry so it can be closed
  static OperationsRegistry deployOperationsRegistry(Injector injector) {
    OperationsRegistry opsRegSvc = injector.getInstance(OperationsRegistry.class);
    log.info("AMQ: TASKER: Deploying RPC service for OperationsRegistry_I: {} ", opsRegSvc);
    injector.getInstance(ActiveMqRpcServer.class).start(CommunicationConsts.OPSREGISTRY_RPCADDR, opsRegSvc, true);
    
    try {
      Connection connection=injector.getInstance(Connection.class);
      new AmqComponentSubscriber(connection, "OperationsRegistry", 
          CommunicationConsts.RPC_ADDR, CommunicationConsts.OPSREGISTRY_RPCADDR, 
          CommunicationConsts.COMPONENT_TYPE, "OperationsRegistry");
    } catch (JMSException e) {
      log.warn("When setting up AmqComponentSubscriber", e);
    }
    return opsRegSvc;
  }
}
