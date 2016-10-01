package com.google.common.util.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;

/**
 * Copyright (C) 2010 RapidPM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by RapidPM - Team on 18.09.16.
 */
public class ThreadFactoryBuilder {
  private String nameFormat;
  private boolean deamon;

  public ThreadFactoryBuilder setDaemon(final boolean deamon) {
    this.deamon = deamon;
    return this;
  }

  public ThreadFactoryBuilder setNameFormat(final String nameFormat) {
    this.nameFormat = nameFormat;
    return this;
  }


  public ThreadFactory build() {
    return new DefaulttF

    throw new RuntimeException("not yet impl"); //Todo impl
  }
}
