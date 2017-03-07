/*
 * Data Engine API
 * orchestrates backend jobs
 *
 * OpenAPI spec version: 0.0.1-SNAPSHOT
 * Contact: agilion@deelam.net
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package dataengine.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import dataengine.api.OperationSelectionMap;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import java.io.Serializable;

/**
 * selected operation values
 */
@ApiModel(description = "selected operation values")

public class OperationSelection  implements Serializable {
  @JsonProperty("id")
  private String id = null;

  @JsonProperty("params")
  private Map params = null;

  @JsonProperty("subOperationSelections")
  private OperationSelectionMap subOperationSelections = null;

  public OperationSelection id(String id) {
    this.id = id;
    return this;
  }

   /**
   * references an operation.id
   * @return id
  **/
  @ApiModelProperty(example = "IngestDataset", required = true, value = "references an operation.id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public OperationSelection params(Map params) {
    this.params = params;
    return this;
  }

   /**
   * Get params
   * @return params
  **/
  @ApiModelProperty(required = true, value = "")
  public Map getParams() {
    return params;
  }

  public void setParams(Map params) {
    this.params = params;
  }

  public OperationSelection subOperationSelections(OperationSelectionMap subOperationSelections) {
    this.subOperationSelections = subOperationSelections;
    return this;
  }

   /**
   * Get subOperationSelections
   * @return subOperationSelections
  **/
  @ApiModelProperty(value = "")
  public OperationSelectionMap getSubOperationSelections() {
    return subOperationSelections;
  }

  public void setSubOperationSelections(OperationSelectionMap subOperationSelections) {
    this.subOperationSelections = subOperationSelections;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OperationSelection operationSelection = (OperationSelection) o;
    return Objects.equals(this.id, operationSelection.id) &&
        Objects.equals(this.params, operationSelection.params) &&
        Objects.equals(this.subOperationSelections, operationSelection.subOperationSelections);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, params, subOperationSelections);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OperationSelection {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    params: ").append(toIndentedString(params)).append("\n");
    sb.append("    subOperationSelections: ").append(toIndentedString(subOperationSelections)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

