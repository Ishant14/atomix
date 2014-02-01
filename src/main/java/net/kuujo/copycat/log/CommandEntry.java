/*
 * Copyright 2014 the original author or authors.
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
package net.kuujo.copycat.log;

import net.kuujo.copycat.Command;
import net.kuujo.copycat.impl.DefaultCommand;

/**
 * A state machine command entry.
 * 
 * @author Jordan Halterman
 */
public class CommandEntry extends Entry {
  private Command command;

  public CommandEntry() {
    super(Type.COMMAND);
  }

  public CommandEntry(long term, Command command) {
    super(Type.COMMAND);
    this.command = command instanceof DefaultCommand ? ((DefaultCommand) command).setEntry(this) : command;
  }

  /**
   * Returns the state machine command.
   * 
   * @return The state machine command.
   */
  public Command command() {
    return command;
  }

  @Override
  public void free() {
    if (log != null) {
      log.free(this);
    }
  }

}