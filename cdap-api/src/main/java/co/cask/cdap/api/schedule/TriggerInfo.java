/*
 * Copyright © 2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.api.schedule;

/**
 * Base class for the trigger information to be passed to the triggered program.
 */
public abstract class TriggerInfo {
  private final Trigger.Type type;

  public TriggerInfo(Trigger.Type type) {
    this.type = type;
  }

  /**
   * @return The type of the trigger.
   */
  public Trigger.Type getType() {
    return type;
  }
}