package dataengine.main;

import java.util.Scanner;
import org.slf4j.Logger;
import lombok.Setter;

public class ConsolePrompter {
  @Setter
  Logger log;
  
  @Setter
  String prefix;

  public ConsolePrompter(String prefix) {
    this.prefix=prefix;
  }

  static Scanner scanner = new Scanner(System.in);

  class PromptMessage {
    String prompt = null;
    long sleepTime = 5000;
    int counter=0;
    int maxPrompts=10;
    
    void reinit(String msg, long sleepTime, int maxPrompts) {
      this.prompt = msg;
      this.sleepTime = sleepTime;
      this.maxPrompts=maxPrompts;
      counter=0;
    }
    boolean showPrompt() {
      if (prompt == null)
        return false;
      ++counter;
      if(log==null)
        System.err.println(prefix+prompt);
      else
        log.info(prefix+prompt);
      if(maxPrompts>0 && counter>maxPrompts)
        prompt=null;
      return true;
    }
    boolean isDone() {
      return prompt == null;
    }
    void pause() throws InterruptedException {
      Thread.sleep(sleepTime);
    }

    public void stop() {
      prompt = null;
    }
  }

  @Setter
  boolean skipUserInput=false;
  
  Thread prompterThread;
  PromptMessage promptMsg = new PromptMessage();

  private boolean stayAlive=true;

  public String getUserInput(String msg, long sleepTime) {
    return getUserInput(msg, sleepTime, -1);
  }
  public String getUserInput(String msg, long sleepTime, int maxPrompts) {
    promptMsg.reinit(msg, sleepTime, maxPrompts);
    if (skipUserInput) {
      promptMsg.showPrompt();
      return "";
    }
    if (prompterThread == null) {
      prompterThread = new Thread(() -> {
        while (stayAlive) {
          try {
            promptMsg.pause();
            promptMsg.showPrompt();
            if (promptMsg.isDone())
              synchronized (prompterThread) {
                prompterThread.wait();
              }
          } catch (InterruptedException e) {
            if(stayAlive)
              log.info("While waiting for user input: {}", e.getMessage());
          }
        }
      }, "myConsolePrompterThread");
      prompterThread.setDaemon(true);
      prompterThread.start();
    }

    promptMsg.showPrompt();
    synchronized (prompterThread) {
      prompterThread.notify();
    }
    try {
      return scanner.nextLine();
    }catch(Exception e) {
      log.warn("", e);
      return "";
    }finally {
      promptMsg.stop();
      scanner.reset();
    }
  }

  public void shutdown() {
    if(prompterThread!=null) {
      stayAlive=false;
      prompterThread.interrupt();
    }
  }
}