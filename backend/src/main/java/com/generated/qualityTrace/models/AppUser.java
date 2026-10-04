package com.generated.qualityTrace.models;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 系统用户（检验员 / 主管 / 经理 / 审计员）。
 * 检验任务、资质、检验结论都通过 {@code role} 区分职责，检验员即 role=INSPECTOR 的用户。
 */
@TableName("app_user")
public class AppUser {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 登录名 */
  private String username;

  /** 姓名 */
  private String displayName;

  /** 角色，见 constants/ActorRole */
  private String role;

  /** 状态：ACTIVE / DISABLED */
  private String status;

  private String createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }
}
