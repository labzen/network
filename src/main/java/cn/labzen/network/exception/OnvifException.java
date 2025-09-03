package cn.labzen.network.exception;

import cn.labzen.meta.exception.LabzenRuntimeException;

public class OnvifException extends LabzenRuntimeException {

  public OnvifException(String message) {
    super(message);
  }

  public OnvifException(String message, Object... args) {
    super(message, args);
  }

  public OnvifException(Throwable cause) {
    super(cause);
  }

  public OnvifException(Throwable cause, String message) {
    super(cause, message);
  }

  public OnvifException(Throwable cause, String message, Object... args) {
    super(cause, message, args);
  }
}
