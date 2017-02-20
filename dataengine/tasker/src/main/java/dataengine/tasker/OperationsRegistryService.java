package dataengine.tasker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import dataengine.api.Operation;
import dataengine.api.OperationParam;
import dataengine.api.OperationParam.ValuetypeEnum;
import dataengine.apis.OperationsRegistry_I;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Inject) )
public class OperationsRegistryService implements OperationsRegistry_I {

  final OperationsRegistryVerticle opsRegVert;
  
  @Override
  public CompletableFuture<List<Operation>> listOperations() {
    ArrayList<Operation> list = new ArrayList<>();
    List<OperationParam> params=new ArrayList<>();
    list.add(new Operation().id("ADD_SOURCE_DATASET").description("add source dataset")
        .params(params));
    params.add(new OperationParam().key("inputUri").required(true).description("location of source dataset")
        .valuetype(ValuetypeEnum.STRING).defaultValue(null).isMultivalued(false));
    params.add(new OperationParam().key("dataformat").required(true).description("type and format of data")
        .valuetype(ValuetypeEnum.ENUM).defaultValue(null).isMultivalued(false)
        .addPossibleValuesItem("TELEPHONE.CSV").addPossibleValuesItem("PEOPLE.CSV")); // TODO: 1:retrieve from Workers
    list.addAll(opsRegVert.getOperations());
    return CompletableFuture.completedFuture(list);
  }

}
