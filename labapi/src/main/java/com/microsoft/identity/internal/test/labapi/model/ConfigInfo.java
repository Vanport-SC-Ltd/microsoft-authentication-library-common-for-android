/*
 * Azure Identity Labs API
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 1.0.0.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.microsoft.identity.internal.test.labapi.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.microsoft.identity.internal.test.labapi.model.AppInfo;
import com.microsoft.identity.internal.test.labapi.model.LabInfo;
import com.microsoft.identity.internal.test.labapi.model.UserInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * ConfigInfo
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-06-01T10:19:44.716-07:00[America/Los_Angeles]")
public class ConfigInfo {
  @SerializedName("user")
  private UserInfo userInfo = null;

  @SerializedName("app")
  private AppInfo appInfo = null;

  @SerializedName("lab")
  private LabInfo labInfo = null;

  public ConfigInfo userInfo(UserInfo userInfo) {
    this.userInfo = userInfo;
    return this;
  }

   /**
   * Get userInfo
   * @return userInfo
  **/
  @Schema(description = "")
  public UserInfo getUserInfo() {
    return userInfo;
  }

  public void setUserInfo(UserInfo userInfo) {
    this.userInfo = userInfo;
  }

  public ConfigInfo appInfo(AppInfo appInfo) {
    this.appInfo = appInfo;
    return this;
  }

   /**
   * Get appInfo
   * @return appInfo
  **/
  @Schema(description = "")
  public AppInfo getAppInfo() {
    return appInfo;
  }

  public void setAppInfo(AppInfo appInfo) {
    this.appInfo = appInfo;
  }

  public ConfigInfo labInfo(LabInfo labInfo) {
    this.labInfo = labInfo;
    return this;
  }

   /**
   * Get labInfo
   * @return labInfo
  **/
  @Schema(description = "")
  public LabInfo getLabInfo() {
    return labInfo;
  }

  public void setLabInfo(LabInfo labInfo) {
    this.labInfo = labInfo;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfigInfo configInfo = (ConfigInfo) o;
    return Objects.equals(this.userInfo, configInfo.userInfo) &&
        Objects.equals(this.appInfo, configInfo.appInfo) &&
        Objects.equals(this.labInfo, configInfo.labInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userInfo, appInfo, labInfo);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConfigInfo {\n");
    
    sb.append("    userInfo: ").append(toIndentedString(userInfo)).append("\n");
    sb.append("    appInfo: ").append(toIndentedString(appInfo)).append("\n");
    sb.append("    labInfo: ").append(toIndentedString(labInfo)).append("\n");
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
