package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.MessageModel;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
public class MessageController {
  private final MessageSendingOperations<String> messageSendingOperations;

  public MessageController(MessageSendingOperations<String> messageSendingOperations) {
    this.messageSendingOperations = messageSendingOperations;
  }

  public void send(MessageModel model) {
    messageSendingOperations.convertAndSend("/socket/messages", model);
  }
}
