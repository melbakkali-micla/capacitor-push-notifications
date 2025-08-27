package com.capacitorjs.plugins.pushnotifications;

public class ChatRoutingInfo {

  public String activeDirectChatId;
  public String activeGroupChatId;
  public String type;

  public String getActiveDirectChatId() {
    return activeDirectChatId;
  }

  public void setActiveDirectChatId(String activeDirectChatUsers) {
    this.activeDirectChatId = activeDirectChatUsers;
  }

  public String getActiveGroupChatId() {
    return activeGroupChatId;
  }

  public void setActiveGroupChatId(String activeGroupChatId) {
    this.activeGroupChatId = activeGroupChatId;
  }


  public void setType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
