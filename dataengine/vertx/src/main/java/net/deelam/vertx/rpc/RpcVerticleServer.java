package net.deelam.vertx.rpc;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * To be used with RpcVerticleClient.
 */
@RequiredArgsConstructor
@Slf4j
public class RpcVerticleServer {
  final Vertx vertx;
  final String serversBroadcastAddr;

  public <T> RpcVerticleServer start(String serverAddr, T service) {
    return start(serverAddr, service, true);
  }
  public <T> RpcVerticleServer start(String serverAddr, T service, boolean withDebugHook) {
    VertxRpcUtil rpc=new VertxRpcUtil(vertx.eventBus(), serverAddr);
    if (withDebugHook)
      rpc.setHook(new VertxRpcUtil.DebugRpcHook());
    rpc.registerServer(service);

    vertx.eventBus().consumer(serversBroadcastAddr, (Message<String> clientAddr) -> {
      log.info("Got client broadcast from {}", clientAddr.body());
      vertx.eventBus().send(clientAddr.body(), serverAddr);
    });
    return this;
  }

}