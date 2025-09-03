package cn.labzen.network.exception;

import cn.labzen.meta.exception.LabzenRuntimeException;

public class NTPException extends LabzenRuntimeException {

  public NTPException(String message) {
    super(message);
  }

  public NTPException(String message, Object... args) {
    super(message, args);
  }

  public NTPException(Throwable cause) {
    super(cause);
  }

  public NTPException(Throwable cause, String message) {
    super(cause, message);
  }

  public NTPException(Throwable cause, String message, Object... args) {
    super(cause, message, args);
  }
}
