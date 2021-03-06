package dataengine.sessions;

import static net.deelam.graph.GrafTxn.tryAndCloseTxn;
import static net.deelam.graph.GrafTxn.tryFAndCloseTxn;
import java.io.Closeable;
import java.io.IOException;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;
import com.tinkerpop.blueprints.util.wrappers.id.IdGraph;
import com.tinkerpop.frames.FramedTransactionalGraph;

import dataengine.sessions.frames.BaseFrame;
import dataengine.sessions.frames.SessionFramesRegistry;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import net.deelam.graph.BpGrafDebug;
import net.deelam.graph.FramedGrafSupplier;
import net.deelam.graph.GrafTxn;
import net.deelam.utils.ConsoleLogging;

@Slf4j
public class SessionDB implements Closeable{
  static final Logger clog=ConsoleLogging.createSlf4jLogger("console.Session");

  private final FramedGrafSupplier fgProvider;
  @Getter
  private final FramedTransactionalGraph<TransactionalGraph> graph;

  @Delegate
  private final SessionDB_FrameHelper frameHelper;

  @Delegate
  private final SessionDB_SessionHelper sessHelper;

  @Delegate
  private final SessionDB_RequestHelper reqHelper;

  private final SessionDB_OperationHelper opHelper;
  
  @Delegate
  private final SessionDB_JobHelper jobHelper;

  @Delegate
  private final SessionDB_DatasetHelper dsHelper;

  //  @Delegate
  //  private final DeprecatedHelper depHelper;

  static final String ROOT_NODE = "ROOT";

  SessionDB(IdGraph<?> tgraph) {
    log.info("SESS: Creating SessionDB with {}", tgraph);
    Vertex root = tgraph.getVertex(ROOT_NODE);
    if (root == null) {
      tryAndCloseTxn(tgraph, () -> tgraph.addVertex(ROOT_NODE));
    }

    fgProvider = new FramedGrafSupplier(SessionFramesRegistry.getFrameClasses());
    graph = fgProvider.get(tgraph);
    frameHelper = new SessionDB_FrameHelper(graph);
    sessHelper = new SessionDB_SessionHelper(graph, frameHelper);
    opHelper = new SessionDB_OperationHelper(graph);
    reqHelper = new SessionDB_RequestHelper(graph, sessHelper, opHelper, frameHelper);
    dsHelper = new SessionDB_DatasetHelper(graph, frameHelper);
    jobHelper = new SessionDB_JobHelper(graph, dsHelper, reqHelper, frameHelper);
    //    depHelper=new DeprecatedHelper(graph, sessHelper, frameHelper);
    log.info("-- Ready: {}", this);
  }

  private static final boolean DEBUG = true;

  public synchronized void close() {
    log.info("SESS: Shutting down " + graph + " " + GrafTxn.isInTransaction());
    if(DEBUG) {
      log.info("Session graph: {}", BpGrafDebug.toString(graph));
      try {
        GraphMLWriter writer=new GraphMLWriter(graph);
        writer.setNormalize(true);
        writer.outputGraph("sessionDb.graphml");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    graph.shutdown();
    log.info("Shut down " + graph);
  };



  public String getBaseDirectory(String sessId) {
    return tryFAndCloseTxn(graph, graph -> sessHelper.getSessionFrame(sessId).getBaseDirectory());
  }

  ///

  synchronized public boolean hasNode(String qualTaskId) {
    return tryAndCloseTxn(graph, graph -> graph.getVertex(qualTaskId) != null);
  }

  private <T> void validateNode(String id, Class<T> expected) {
    Vertex v = graph.getVertex(id);
    if (v == null)
      throw new NoSuchElementException();
    Class<?> clazz = fgProvider.getFrameClassOf(v, BaseFrame.TYPE_KEY);
    if (!expected.isAssignableFrom(clazz))
      throw new IllegalArgumentException();
  }

}
